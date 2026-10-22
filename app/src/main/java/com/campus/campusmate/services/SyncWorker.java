package com.campus.campusmate.services;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.notifications.NotificationHelper;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.utils.SupabaseManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;
import timber.log.Timber;

/** Periodic background check for new approved content and post status changes. */
public class SyncWorker extends Worker {

    private static final String PREFS = "campusmate_sync_prefs";
    private static final String KEY_LAST_ANNOUNCEMENT = "last_announcement_created_at";
    private static final String KEY_LAST_EVENT = "last_event_created_at";
    private static final String KEY_LAST_PENDING_COUNT = "last_pending_count";
    private static final String KEY_USER_POSTS_LAST_SYNC = "user_posts_last_sync";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn()) return Result.success();

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        try {
            Timber.d("Starting periodic sync");

            checkAnnouncements(context, session, prefs);
            checkEvents(context, session, prefs);
            
            if (session.canPost()) {
                checkOwnPostStatus(context, session, prefs);
            }
            
            if (session.isAdmin()) {
                checkPendingApprovals(context, prefs);
            }
            
            return Result.success();
        } catch (Exception e) {
            Timber.e(e, "Sync error");
            return Result.retry();
        }
    }

    private void checkAnnouncements(Context context, SessionManager session, SharedPreferences prefs)
            throws Exception {
        Map<String, String> filter = new HashMap<>();
        filter.put("status", "eq." + Announcement.STATUS_APPROVED);
        String dept = session.getUserDepartment();
        if (dept != null && !dept.isEmpty()) {
            filter.put("or", "(visibility.eq.general,department.eq." + dept + ")");
        } else {
            filter.put("visibility", "eq.general");
        }
        filter.put("order", "created_at.desc");

        Response<List<Announcement>> response = SupabaseManager.getInstance(context)
                .dataApi().getAnnouncements(filter, "0-19").execute();
        if (!response.isSuccessful() || response.body() == null) return;

        long lastSeen = prefs.getLong(KEY_LAST_ANNOUNCEMENT, 0);
        long newest = lastSeen;
        int newCount = 0;
        Announcement latest = null;

        for (Announcement a : response.body()) {
            if (a.getCreatedAt() > lastSeen) {
                newCount++;
                if (latest == null || a.getCreatedAt() > latest.getCreatedAt()) latest = a;
            }
            newest = Math.max(newest, a.getCreatedAt());
        }

        if (lastSeen > 0 && newCount > 0 && latest != null) {
            String body = newCount == 1 ? latest.getTitle() : newCount + " new announcements available";
            NotificationHelper.notifyAnnouncement(context, latest.getId(), "Campus Bulletin", body);
        }
        prefs.edit().putLong(KEY_LAST_ANNOUNCEMENT, newest).apply();
    }

    private void checkEvents(Context context, SessionManager session, SharedPreferences prefs)
            throws Exception {
        Map<String, String> filter = new HashMap<>();
        filter.put("status", "eq." + Event.STATUS_APPROVED);
        String dept = session.getUserDepartment();
        if (dept != null && !dept.isEmpty()) {
            filter.put("or", "(visibility.eq.general,department.eq." + dept + ")");
        } else {
            filter.put("visibility", "eq.general");
        }
        filter.put("order", "created_at.desc");

        Response<List<Event>> response = SupabaseManager.getInstance(context)
                .dataApi().getEvents(filter, "0-19").execute();
        if (!response.isSuccessful() || response.body() == null) return;

        long lastSeen = prefs.getLong(KEY_LAST_EVENT, 0);
        long newest = lastSeen;
        int newCount = 0;
        Event latest = null;

        for (Event e : response.body()) {
            if (e.getCreatedAt() > lastSeen) {
                newCount++;
                if (latest == null || e.getCreatedAt() > latest.getCreatedAt()) latest = e;
            }
            newest = Math.max(newest, e.getCreatedAt());
        }

        if (lastSeen > 0 && newCount > 0 && latest != null) {
            String body = newCount == 1 ? latest.getTitle() : newCount + " new campus events";
            NotificationHelper.notifyEvent(context, latest.getId(), "New Event", body);
        }
        prefs.edit().putLong(KEY_LAST_EVENT, newest).apply();
    }

    private void checkOwnPostStatus(Context context, SessionManager session, SharedPreferences prefs) throws Exception {
        String userId = session.getUserId();
        long lastCheck = prefs.getLong(KEY_USER_POSTS_LAST_SYNC, 0);
        long now = System.currentTimeMillis();

        // First-ever run has nothing to compare against — recording the baseline here (rather
        // than notifying about every already-resolved post the user has) mirrors how
        // checkAnnouncements/checkEvents above treat their own first run.
        boolean firstRun = lastCheck == 0;

        // 1. Check Announcements
        Map<String, String> aFilter = new HashMap<>();
        aFilter.put("author_id", "eq." + userId);
        if (!firstRun) {
            aFilter.put("updated_at", "gt." + lastCheck);
        }

        Response<List<Announcement>> aResponse = SupabaseManager.getInstance(context)
                .dataApi().getAnnouncements(aFilter, null).execute();

        if (!firstRun && aResponse.isSuccessful() && aResponse.body() != null) {
            for (Announcement a : aResponse.body()) {
                if (Announcement.STATUS_APPROVED.equals(a.getStatus())) {
                    NotificationHelper.notifyPostApproved(context, "Announcement Approved",
                            "Your post \"" + a.getTitle() + "\" is now live.", a.getId());
                } else if (Announcement.STATUS_REJECTED.equals(a.getStatus())) {
                    NotificationHelper.notifyPostRejected(context, "Announcement Rejected",
                            "Your post \"" + a.getTitle() + "\" was not approved.", a.getId());
                }
            }
        }

        // 2. Check Events
        Map<String, String> eFilter = new HashMap<>();
        eFilter.put("author_id", "eq." + userId);
        if (!firstRun) {
            eFilter.put("updated_at", "gt." + lastCheck);
        }

        Response<List<Event>> eResponse = SupabaseManager.getInstance(context)
                .dataApi().getEvents(eFilter, null).execute();

        if (!firstRun && eResponse.isSuccessful() && eResponse.body() != null) {
            for (Event e : eResponse.body()) {
                if (Event.STATUS_APPROVED.equals(e.getStatus())) {
                    NotificationHelper.notifyPostApproved(context, "Event Approved",
                            "Your event \"" + e.getTitle() + "\" is now live.", e.getId());
                } else if (Event.STATUS_REJECTED.equals(e.getStatus())) {
                    NotificationHelper.notifyPostRejected(context, "Event Rejected",
                            "Your event \"" + e.getTitle() + "\" was not approved.", e.getId());
                }
            }
        }

        prefs.edit().putLong(KEY_USER_POSTS_LAST_SYNC, now).apply();
    }

    private void checkPendingApprovals(Context context, SharedPreferences prefs) throws Exception {
        Map<String, String> announcementFilter = new HashMap<>();
        announcementFilter.put("status", "eq." + Announcement.STATUS_PENDING);
        Response<List<Announcement>> aResponse = SupabaseManager.getInstance(context)
                .dataApi().getAnnouncements(announcementFilter, null).execute();

        Map<String, String> eventFilter = new HashMap<>();
        eventFilter.put("status", "eq." + Event.STATUS_PENDING);
        Response<List<Event>> eResponse = SupabaseManager.getInstance(context)
                .dataApi().getEvents(eventFilter, null).execute();

        int pendingCount = 0;
        if (aResponse.isSuccessful() && aResponse.body() != null) pendingCount += aResponse.body().size();
        if (eResponse.isSuccessful() && eResponse.body() != null) pendingCount += eResponse.body().size();

        int lastCount = prefs.getInt(KEY_LAST_PENDING_COUNT, 0);
        if (pendingCount > lastCount) {
            NotificationHelper.notifyApprovals(context, "New Approvals Pending",
                    pendingCount + " item" + (pendingCount == 1 ? "" : "s") + " awaiting your review.");
        }
        prefs.edit().putInt(KEY_LAST_PENDING_COUNT, pendingCount).apply();
    }
}
