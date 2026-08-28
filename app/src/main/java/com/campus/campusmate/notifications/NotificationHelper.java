package com.campus.campusmate.notifications;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.campus.campusmate.R;
import com.campus.campusmate.activities.AdminDashboardActivity;
import com.campus.campusmate.activities.AnnouncementDetailActivity;
import com.campus.campusmate.activities.EventDetailActivity;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.SessionManager;

public final class NotificationHelper {

    private NotificationHelper() {}

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        manager.createNotificationChannel(channel(Constants.CHANNEL_ANNOUNCEMENTS,
                "Announcements", "New announcements posted to JHENORA"));
        manager.createNotificationChannel(channel(Constants.CHANNEL_EVENTS,
                "Events", "New events posted to JHENORA"));
        manager.createNotificationChannel(channel(Constants.CHANNEL_APPROVALS,
                "Approvals", "Items awaiting admin approval"));
        manager.createNotificationChannel(channel(Constants.CHANNEL_UPDATES,
                "Updates", "General JHENORA updates"));
    }

    /**
     * Checks and requests POST_NOTIFICATIONS permission for Android 13+.
     * @param activity The calling activity.
     */
    public static void requestPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    private static NotificationChannel channel(String id, String name, String description) {
        NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(description);
        return channel;
    }

    public static void notifyAnnouncement(Context context, String announcementId, String title, String body) {
        if (!new SessionManager(context).isNotificationEnabled(Constants.PREF_NOTIF_ANNOUNCEMENTS)) return;
        Intent intent = new Intent(context, AnnouncementDetailActivity.class);
        intent.putExtra(Constants.EXTRA_ANNOUNCEMENT_ID, announcementId);
        show(context, Constants.CHANNEL_ANNOUNCEMENTS, Constants.NOTIF_ID_ANNOUNCEMENT,
                R.drawable.ic_announcements, title, body, intent);
    }

    public static void notifyEvent(Context context, String eventId, String title, String body) {
        if (!new SessionManager(context).isNotificationEnabled(Constants.PREF_NOTIF_EVENTS)) return;
        Intent intent = new Intent(context, EventDetailActivity.class);
        intent.putExtra(Constants.EXTRA_EVENT_ID, eventId);
        show(context, Constants.CHANNEL_EVENTS, Constants.NOTIF_ID_EVENT,
                R.drawable.ic_events, title, body, intent);
    }

    public static void notifyApprovals(Context context, String title, String body) {
        if (!new SessionManager(context).isNotificationEnabled(Constants.PREF_NOTIF_APPROVALS)) return;
        Intent intent = new Intent(context, AdminDashboardActivity.class);
        show(context, Constants.CHANNEL_APPROVALS, Constants.NOTIF_ID_APPROVAL,
                R.drawable.ic_announcements, title, body, intent);
    }

    public static void notifyPostApproved(Context context, String title, String body) {
        Intent intent = new Intent(context, com.campus.campusmate.activities.MainActivity.class);
        show(context, Constants.CHANNEL_UPDATES, 2001,
                R.drawable.ic_shield_check, title, body, intent);
    }

    public static void notifyPostRejected(Context context, String title, String body) {
        Intent intent = new Intent(context, com.campus.campusmate.activities.MainActivity.class);
        show(context, Constants.CHANNEL_UPDATES, 2002,
                R.drawable.ic_announcements, title, body, intent);
    }

    private static void show(Context context, String channelId, int notificationId, int smallIcon,
                              String title, String body, Intent targetIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        targetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, targetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        NotificationManagerCompat.from(context).notify(notificationId, notification);
    }
}
