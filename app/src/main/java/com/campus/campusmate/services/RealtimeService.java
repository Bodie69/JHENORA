package com.campus.campusmate.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.campus.campusmate.R;
import com.campus.campusmate.activities.MainActivity;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.notifications.NotificationHelper;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.utils.SupabaseManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import timber.log.Timber;

/**
 * Keeps a persistent Supabase Realtime (Phoenix websocket) connection open so the author of an
 * announcement/event is notified the instant an admin approves or rejects it, regardless of
 * whether the app is foregrounded, backgrounded, or the process was previously killed. Runs as
 * a foreground service (required to survive Android's background execution limits) with a
 * minimal-priority "always on" notification — see {@link #buildNotification()}.
 * <p>
 * Falls back to {@link SyncWorker}'s periodic poll for the rare case this connection or the
 * service itself gets torn down by the OS before it reconnects.
 */
public class RealtimeService extends Service {

    private static final int NOTIF_ID = 9001;
    private static final long HEARTBEAT_INTERVAL_MS = 25_000L;
    private static final long RECONNECT_BASE_DELAY_MS = 3_000L;
    private static final long RECONNECT_MAX_DELAY_MS = 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable heartbeatRunnable = this::sendHeartbeatAndReschedule;
    private final Runnable reconnectRunnable = this::connect;

    private OkHttpClient client;
    private WebSocket webSocket;
    private SessionManager session;
    private int reconnectAttempt = 0;

    public static void start(Context context) {
        try {
            ContextCompat.startForegroundService(context, new Intent(context, RealtimeService.class));
        } catch (Exception e) {
            // Android may refuse a foreground-service start from a non user-visible context
            // (e.g. a background WorkManager job restarting the process). SyncWorker's periodic
            // poll still covers approvals/rejections in that case, just not instantly.
            Timber.w(e, "Could not start RealtimeService");
        }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, RealtimeService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        session = new SessionManager(this);
        client = SupabaseManager.getInstance(this).getHttpClient();
        startForeground(NOTIF_ID, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!session.isLoggedIn()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        connect();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (webSocket != null) {
            webSocket.close(1000, "service stopping");
            webSocket = null;
        }
        super.onDestroy();
    }

    private void connect() {
        handler.removeCallbacks(reconnectRunnable);
        String userId = session.getUserId();
        if (userId == null) {
            stopSelf();
            return;
        }

        String wsBase = Constants.SUPABASE_URL.replaceFirst("^http", "ws");
        String url = wsBase + "/realtime/v1/websocket?apikey=" + Constants.SUPABASE_ANON_KEY + "&vsn=1.0.0";
        Request request = new Request.Builder().url(url).build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                reconnectAttempt = 0;
                joinChannel(ws, userId);
                handler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS);
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                handleMessage(text);
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, @Nullable Response response) {
                Timber.w(t, "Realtime socket failure");
                scheduleReconnect();
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                scheduleReconnect();
            }
        });
    }

    /**
     * Joins a single channel carrying two postgres_changes subscriptions (announcements and
     * events), both filtered server-side to this user's own rows so approvals/rejections for
     * other authors' posts never even reach the device.
     */
    private void joinChannel(WebSocket ws, String userId) {
        String payload = "{"
                + "\"topic\":\"realtime:public:post-status-" + userId + "\","
                + "\"event\":\"phx_join\","
                + "\"payload\":{\"config\":{\"postgres_changes\":["
                + "{\"event\":\"UPDATE\",\"schema\":\"public\",\"table\":\"" + Constants.DB_ANNOUNCEMENTS
                + "\",\"filter\":\"author_id=eq." + userId + "\"},"
                + "{\"event\":\"UPDATE\",\"schema\":\"public\",\"table\":\"" + Constants.DB_EVENTS
                + "\",\"filter\":\"author_id=eq." + userId + "\"}"
                + "]}},"
                + "\"ref\":\"1\"}";
        ws.send(payload);
    }

    private void sendHeartbeatAndReschedule() {
        if (webSocket != null) {
            webSocket.send("{\"topic\":\"phoenix\",\"event\":\"heartbeat\",\"payload\":{},\"ref\":\"hb\"}");
        }
        handler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS);
    }

    private void scheduleReconnect() {
        handler.removeCallbacks(heartbeatRunnable);
        if (!session.isLoggedIn()) {
            stopSelf();
            return;
        }
        reconnectAttempt++;
        long delay = Math.min(RECONNECT_BASE_DELAY_MS * (1L << Math.min(reconnectAttempt, 5)), RECONNECT_MAX_DELAY_MS);
        handler.postDelayed(reconnectRunnable, delay);
    }

    private void handleMessage(String text) {
        try {
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            JsonElement eventEl = root.get("event");
            if (eventEl == null || !"postgres_changes".equals(eventEl.getAsString())) return;

            JsonObject payload = root.getAsJsonObject("payload");
            JsonObject data = payload != null ? payload.getAsJsonObject("data") : null;
            if (data == null) return;

            String table = data.has("table") ? data.get("table").getAsString() : "";
            JsonObject newRow = data.getAsJsonObject("new");
            if (newRow == null) return;

            String status = stringField(newRow, "status");
            String authorId = stringField(newRow, "author_id");
            if (status == null || authorId == null || !authorId.equals(session.getUserId())) return;

            boolean isEvent = Constants.DB_EVENTS.equals(table);
            String id = stringField(newRow, "id");
            String title = stringField(newRow, "title");
            String kind = isEvent ? "event" : "post";

            if (Announcement.STATUS_APPROVED.equals(status)) {
                NotificationHelper.notifyPostApproved(this,
                        isEvent ? "Event Approved" : "Announcement Approved",
                        "Your " + kind + " \"" + title + "\" is now live.", id);
            } else if (Announcement.STATUS_REJECTED.equals(status)) {
                String reason = stringField(newRow, "rejection_reason");
                String body = "Your " + kind + " \"" + title + "\" was not approved."
                        + (reason != null && !reason.isEmpty() ? " Reason: " + reason : "");
                NotificationHelper.notifyPostRejected(this,
                        isEvent ? "Event Rejected" : "Announcement Rejected", body, id);
            }
        } catch (Exception e) {
            Timber.w(e, "Failed to parse realtime message");
        }
    }

    @Nullable
    private String stringField(JsonObject obj, String field) {
        JsonElement el = obj.get(field);
        return (el == null || el.isJsonNull()) ? null : el.getAsString();
    }

    private Notification buildNotification() {
        NotificationHelper.ensureRealtimeChannel(this);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        return new NotificationCompat.Builder(this, Constants.CHANNEL_REALTIME)
                .setSmallIcon(R.drawable.ic_shield_check)
                .setContentTitle("JHENORA")
                .setContentText("Listening for approval updates")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
    }
}
