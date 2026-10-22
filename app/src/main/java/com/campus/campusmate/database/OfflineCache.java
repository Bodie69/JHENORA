package com.campus.campusmate.database;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.Event;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import timber.log.Timber;

/**
 * Saved copy of the approved announcements and events the signed-in user can see, so the feeds,
 * calendar and detail screens still work without a connection.
 * <p>
 * Repositories write every approved post they fetch here and read it back when a request fails.
 * {@code SyncWorker} replaces the whole set whenever the device is online, which also drops
 * posts that were deleted or unpublished on the server. The cache is cleared on logout, so one
 * user's department posts never show for the next user.
 * <p>
 * Async methods run on a single background thread and deliver results on the main thread,
 * like Retrofit callbacks do. Methods ending in {@code Now} block and are for worker threads.
 */
public final class OfflineCache {

    /** Receives a cache read on the main thread. */
    public interface Callback<T> {
        void onResult(T result);
    }

    private static volatile OfflineCache instance;

    private final CachedPostDao dao;
    private final Executor io = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    private OfflineCache(Context context) {
        dao = AppDatabase.getInstance(context).cachedPostDao();
    }

    public static OfflineCache getInstance(Context context) {
        if (instance == null) {
            synchronized (OfflineCache.class) {
                if (instance == null) instance = new OfflineCache(context.getApplicationContext());
            }
        }
        return instance;
    }

    // --- Announcements ---

    /** Saves or refreshes the approved announcements in {@code announcements}; others are ignored. */
    public void saveAnnouncements(@Nullable List<Announcement> announcements) {
        if (announcements == null || announcements.isEmpty()) return;
        List<CachedPost> rows = announcementRows(announcements);
        run(() -> dao.upsert(rows));
    }

    /**
     * Keeps the cached copy of one fetched announcement current: saves it if approved, and
     * removes it otherwise (it was rejected, archived or sent back for approval).
     */
    public void updateAnnouncement(@NonNull Announcement announcement) {
        if (announcement.isApproved()) {
            saveAnnouncements(Collections.singletonList(announcement));
        } else {
            removeAnnouncement(announcement.getId());
        }
    }

    public void removeAnnouncement(@Nullable String id) {
        if (id != null) run(() -> dao.delete(CachedPost.KIND_ANNOUNCEMENT, id));
    }

    /** Makes {@code announcements} the complete cached set, dropping any others. */
    @WorkerThread
    public void replaceAnnouncementsNow(@NonNull List<Announcement> announcements) {
        dao.replaceKind(CachedPost.KIND_ANNOUNCEMENT, announcementRows(announcements));
    }

    /**
     * Loads the cached announcements visible to {@code department}, newest first, or every
     * cached announcement when {@code everyDepartment} is set (Live Display).
     */
    public void loadAnnouncements(@Nullable String department, boolean everyDepartment,
                                  @NonNull Callback<List<Announcement>> callback) {
        read(() -> {
            List<Announcement> list = decode(
                    query(CachedPost.KIND_ANNOUNCEMENT, department, everyDepartment), Announcement.class);
            list.sort(Comparator.comparingLong(Announcement::getCreatedAt).reversed());
            return list;
        }, new ArrayList<>(), callback);
    }

    /** Looks up one cached announcement; delivers {@code null} if it isn't saved. */
    public void findAnnouncement(@NonNull String id, @NonNull Callback<Announcement> callback) {
        read(() -> decodeOne(dao.find(CachedPost.KIND_ANNOUNCEMENT, id), Announcement.class),
                null, callback);
    }

    // --- Events ---

    /** Saves or refreshes the approved events in {@code events}; others are ignored. */
    public void saveEvents(@Nullable List<Event> events) {
        if (events == null || events.isEmpty()) return;
        List<CachedPost> rows = eventRows(events);
        run(() -> dao.upsert(rows));
    }

    /** Event counterpart of {@link #updateAnnouncement}. */
    public void updateEvent(@NonNull Event event) {
        if (event.isApproved()) {
            saveEvents(Collections.singletonList(event));
        } else {
            removeEvent(event.getId());
        }
    }

    public void removeEvent(@Nullable String id) {
        if (id != null) run(() -> dao.delete(CachedPost.KIND_EVENT, id));
    }

    /** Makes {@code events} the complete cached set, dropping any others. */
    @WorkerThread
    public void replaceEventsNow(@NonNull List<Event> events) {
        dao.replaceKind(CachedPost.KIND_EVENT, eventRows(events));
    }

    /**
     * Loads the cached events visible to {@code department}, earliest start first, or every
     * cached event when {@code everyDepartment} is set (Live Display).
     */
    public void loadEvents(@Nullable String department, boolean everyDepartment,
                           @NonNull Callback<List<Event>> callback) {
        read(() -> {
            List<Event> list = decode(
                    query(CachedPost.KIND_EVENT, department, everyDepartment), Event.class);
            list.sort(Comparator.comparingLong(Event::getStartDate));
            return list;
        }, new ArrayList<>(), callback);
    }

    /** Looks up one cached event; delivers {@code null} if it isn't saved. */
    public void findEvent(@NonNull String id, @NonNull Callback<Event> callback) {
        read(() -> decodeOne(dao.find(CachedPost.KIND_EVENT, id), Event.class), null, callback);
    }

    // --- Housekeeping ---

    /** Deletes every saved post. Called on logout. */
    public void clear() {
        run(dao::deleteAll);
    }

    // --- Internals ---

    private List<CachedPost> query(String kind, @Nullable String department, boolean everyDepartment) {
        if (everyDepartment) return dao.getAll(kind);
        String dept = department != null && !department.isEmpty() ? department : null;
        return dao.getVisibleTo(kind, dept);
    }

    private List<CachedPost> announcementRows(List<Announcement> announcements) {
        List<CachedPost> rows = new ArrayList<>();
        for (Announcement a : announcements) {
            if (a == null || a.getId() == null || !a.isApproved()) continue;
            rows.add(new CachedPost(CachedPost.KIND_ANNOUNCEMENT, a.getId(), a.getVisibility(),
                    a.getDepartment(), gson.toJson(a)));
        }
        return rows;
    }

    private List<CachedPost> eventRows(List<Event> events) {
        List<CachedPost> rows = new ArrayList<>();
        for (Event e : events) {
            if (e == null || e.getId() == null || !e.isApproved()) continue;
            rows.add(new CachedPost(CachedPost.KIND_EVENT, e.getId(), e.getVisibility(),
                    e.getDepartment(), gson.toJson(e)));
        }
        return rows;
    }

    private <T> List<T> decode(List<CachedPost> rows, Class<T> type) {
        List<T> list = new ArrayList<>();
        for (CachedPost row : rows) {
            T item = decodeOne(row, type);
            if (item != null) list.add(item);
        }
        return list;
    }

    @Nullable
    private <T> T decodeOne(@Nullable CachedPost row, Class<T> type) {
        if (row == null) return null;
        try {
            return gson.fromJson(row.json, type);
        } catch (RuntimeException e) {
            Timber.w(e, "Skipping unreadable cached %s %s", row.kind, row.id);
            return null;
        }
    }

    /**
     * Runs a read on the cache thread and delivers it on the main thread, or {@code fallback}
     * if the database can't be read: a broken cache must look like an empty one, not crash.
     */
    private <T> void read(Supplier<T> query, T fallback, Callback<T> callback) {
        io.execute(() -> {
            T result;
            try {
                result = query.get();
            } catch (RuntimeException e) {
                Timber.w(e, "Offline cache read failed");
                result = fallback;
            }
            T delivered = result;
            mainThread.post(() -> callback.onResult(delivered));
        });
    }

    /** Runs a write on the cache thread; a failed cache write must never break the caller. */
    private void run(Runnable write) {
        io.execute(() -> {
            try {
                write.run();
            } catch (RuntimeException e) {
                Timber.w(e, "Offline cache write failed");
            }
        });
    }
}
