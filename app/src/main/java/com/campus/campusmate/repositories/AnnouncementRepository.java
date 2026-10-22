package com.campus.campusmate.repositories;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.AuditLog;
import com.campus.campusmate.models.FeedFilter;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.PostFilters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository class responsible for managing Announcement data.
 * Handles fetching, creating, updating, approving, and rejecting announcements via Supabase API.
 * Approved announcements it fetches are saved to the {@link #cache}, and the reads behind the
 * Announcements, Home, Calendar and detail screens fall back to that copy when offline.
 */
public class AnnouncementRepository extends BaseRepository {

    private static AnnouncementRepository instance;

    private AnnouncementRepository() {}

    public static synchronized AnnouncementRepository getInstance() {
        if (instance == null) instance = new AnnouncementRepository();
        return instance;
    }

    @Override
    public void init(Context context) {
        super.init(context);
    }

    // --- Create / Update / Delete ---

    public void createAnnouncement(Announcement announcement, OnCompleteListener listener) {
        String id = UUID.randomUUID().toString();
        announcement.setId(id);
        enqueue(dataApi.insertAnnouncement(announcement, "return=minimal"), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                logAudit(announcement.getAuthorId(), announcement.getAuthorName(),
                        AuditLog.ACTION_CREATE, AuditLog.TARGET_ANNOUNCEMENT, id,
                        "Announcement created: " + announcement.getTitle());
                listener.onSuccess(id);
            }

            @Override
            public void onError(String message) {
                listener.onError("Failed to create announcement");
            }
        });
    }

    public void updateAnnouncement(Announcement announcement, OnCompleteListener listener) {
        announcement.setUpdatedAt(System.currentTimeMillis());
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + announcement.getId());
        Map<String, Object> fields = buildAnnouncementPatch(announcement);
        enqueue(dataApi.patchAnnouncement(filter, fields, "return=minimal"), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                cache.updateAnnouncement(announcement);
                listener.onSuccess(announcement.getId());
            }

            @Override
            public void onError(String message) {
                listener.onError("Update failed");
            }
        });
    }

    public void deleteAnnouncement(String id, String userId, String userName, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        enqueue(dataApi.deleteAnnouncement(filter), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                logAudit(userId, userName, AuditLog.ACTION_DELETE,
                        AuditLog.TARGET_ANNOUNCEMENT, id, "Announcement deleted");
                cache.removeAnnouncement(id);
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
                listener.onError("Delete failed");
            }
        });
    }

    public void approveAnnouncement(String id, String adminId, String adminName, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", Announcement.STATUS_APPROVED);
        fields.put("approved_by", adminName);
        fields.put("approved_at", System.currentTimeMillis());
        fields.put("updated_at", System.currentTimeMillis());
        enqueue(dataApi.patchAnnouncement(filter, fields, "return=minimal"), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                logAudit(adminId, adminName, AuditLog.ACTION_APPROVE,
                        AuditLog.TARGET_ANNOUNCEMENT, id, "Announcement approved");
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
                listener.onError("Approval failed");
            }
        });
    }

    public void rejectAnnouncement(String id, String adminId, String adminName,
                                   String reason, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", Announcement.STATUS_REJECTED);
        fields.put("rejection_reason", reason);
        fields.put("updated_at", System.currentTimeMillis());
        enqueue(dataApi.patchAnnouncement(filter, fields, "return=minimal"), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                logAudit(adminId, adminName, AuditLog.ACTION_REJECT,
                        AuditLog.TARGET_ANNOUNCEMENT, id, "Announcement rejected: " + reason);
                cache.removeAnnouncement(id);
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
                listener.onError("Rejection failed");
            }
        });
    }

    // --- Queries ---

    /**
     * Loads the complete approved-and-visible-to-{@code userDept} announcement set, walked in
     * {@link Constants#BULK_FETCH_CHUNK_SIZE}-row chunks rather than one unbounded request.
     * Used by screens that need the whole list at once (home preview, live display rotation).
     * For the scrollable Announcements feed, use {@link #fetchApprovedAnnouncementsPage} instead.
     */
    public void observeApprovedAnnouncements(String userDept, MutableLiveData<List<Announcement>> liveData) {
        Map<String, String> filter = approvedAnnouncementsFilter(userDept);
        fetchAllPages(
                (range, callback) -> enqueue(dataApi.getAnnouncements(filter, range), callback),
                cachingResult(liveData, userDept, false));
    }

    /**
     * Like {@link #observeApprovedAnnouncements}, but for the campus-wide Live Display kiosk:
     * every approved announcement regardless of visibility/department. The kiosk isn't scoped
     * to any one department — each card's own department badge (BSIT/BTLED/EDUC) identifies
     * what a department-only post is about, the same way a General one is labeled "General".
     */
    public void observeAllApprovedAnnouncements(MutableLiveData<List<Announcement>> liveData) {
        Map<String, String> filter = allApprovedAnnouncementsFilter();
        fetchAllPages(
                (range, callback) -> enqueue(dataApi.getAnnouncements(filter, range), callback),
                cachingResult(liveData, null, true));
    }

    /**
     * Posts a complete approved list to {@code liveData}, saving it for offline use; if the
     * request fails, posts the saved copy instead, or {@code null} when nothing is saved.
     */
    private OnResult<List<Announcement>> cachingResult(MutableLiveData<List<Announcement>> liveData,
            String userDept, boolean everyDepartment) {
        return new OnResult<List<Announcement>>() {
            @Override
            public void onSuccess(List<Announcement> body) {
                cache.saveAnnouncements(body);
                liveData.postValue(body);
            }

            @Override
            public void onError(String message) {
                cache.loadAnnouncements(userDept, everyDepartment,
                        saved -> liveData.postValue(saved.isEmpty() ? null : saved));
            }
        };
    }

    /**
     * Replaces the offline copy with the full current set: what {@code userDept} can see, or
     * every approved announcement for the Live Display ({@code everyDepartment}). Blocks; for
     * background workers. Also drops saved announcements deleted or unpublished meanwhile.
     *
     * @throws IOException if any part of the set can't be fetched (the copy is left as it was)
     */
    public void syncOfflineCacheNow(String userDept, boolean everyDepartment) throws IOException {
        Map<String, String> filter = everyDepartment
                ? allApprovedAnnouncementsFilter() : approvedAnnouncementsFilter(userDept);
        cache.replaceAnnouncementsNow(fetchAllPagesNow(range -> dataApi.getAnnouncements(filter, range)));
    }

    private Map<String, String> allApprovedAnnouncementsFilter() {
        Map<String, String> filter = new HashMap<>();
        filter.put("status", "eq." + Announcement.STATUS_APPROVED);
        filter.put("order", "created_at.desc");
        return filter;
    }

    /**
     * Loads one {@code limit}-row page of approved announcements starting at {@code offset},
     * for infinite-scroll feeds, narrowed and ordered by {@code feed} on the server so filters
     * and search reach past the loaded pages. Unlike {@link #observeApprovedAnnouncements},
     * the caller decides when (or whether) to fetch the next page.
     */
    public void fetchApprovedAnnouncementsPage(String userDept, FeedFilter feed, int offset, int limit,
                                                OnPageListener<Announcement> listener) {
        Map<String, String> filter = approvedAnnouncementsFilter(userDept);
        applyFeedFilter(filter, feed, new ArrayList<>(),
                "title", "what", "where_info", "author_name");
        if (feed.urgentOnly) filter.put("priority", "eq." + Announcement.PRIORITY_URGENT);
        // id breaks created_at ties so page offsets stay stable.
        filter.put("order", FeedFilter.SORT_OLDEST.equals(feed.sort)
                ? "created_at.asc,id.asc" : "created_at.desc,id.desc");
        String range = offset + "-" + (offset + limit - 1);
        enqueue(dataApi.getAnnouncements(filter, range), new OnResult<List<Announcement>>() {
            @Override
            public void onSuccess(List<Announcement> body) {
                cache.saveAnnouncements(body);
                listener.onLoaded(body != null ? body : new java.util.ArrayList<>());
            }

            @Override
            public void onError(String message) {
                // Offline, the first page is replaced by every saved match; later pages just fail.
                if (offset > 0) {
                    listener.onError(message);
                    return;
                }
                cache.loadAnnouncements(userDept, false, saved -> {
                    if (saved.isEmpty()) {
                        listener.onError(message);
                    } else {
                        listener.onLoadedFromCache(PostFilters.filterAnnouncementFeed(saved, feed));
                    }
                });
            }
        });
    }

    private Map<String, String> approvedAnnouncementsFilter(String userDept) {
        Map<String, String> filter = new HashMap<>();
        filter.put("status", "eq." + Announcement.STATUS_APPROVED);
        if (userDept != null && !userDept.isEmpty()) {
            filter.put("or", "(visibility.eq.general,department.eq." + userDept + ")");
        } else {
            filter.put("visibility", "eq.general");
        }
        // Explicit order is required for chunk/page offsets to be stable across requests.
        filter.put("order", "created_at.desc");
        return filter;
    }

    public void observePendingAnnouncements(MutableLiveData<List<Announcement>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("status", "eq." + Announcement.STATUS_PENDING);
        filter.put("order", "created_at.asc");
        fetchAllPages(
                (range, callback) -> enqueue(dataApi.getAnnouncements(filter, range), callback),
                new OnResult<List<Announcement>>() {
                    @Override
                    public void onSuccess(List<Announcement> body) {
                        liveData.postValue(body);
                    }

                    @Override
                    public void onError(String message) {
                        liveData.postValue(null);
                    }
                });
    }

    public void observeUserAnnouncements(String userId, MutableLiveData<List<Announcement>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("author_id", "eq." + userId);
        filter.put("order", "created_at.desc");
        fetchAllPages(
                (range, callback) -> enqueue(dataApi.getAnnouncements(filter, range), callback),
                new OnResult<List<Announcement>>() {
                    @Override
                    public void onSuccess(List<Announcement> body) {
                        liveData.postValue(body);
                    }

                    @Override
                    public void onError(String message) {
                        liveData.postValue(null);
                    }
                });
    }

    public void observeAllAnnouncements(MutableLiveData<List<Announcement>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("order", "created_at.desc");
        fetchAllPages(
                (range, callback) -> enqueue(dataApi.getAnnouncements(filter, range), callback),
                new OnResult<List<Announcement>>() {
                    @Override
                    public void onSuccess(List<Announcement> body) {
                        liveData.postValue(body);
                    }

                    @Override
                    public void onError(String message) {
                        liveData.postValue(null);
                    }
                });
    }

    public void fetchAnnouncementById(String id, OnAnnouncementFetchedListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        enqueue(dataApi.getAnnouncements(filter, null), new OnResult<List<Announcement>>() {
            @Override
            public void onSuccess(List<Announcement> body) {
                if (body != null && !body.isEmpty()) {
                    cache.updateAnnouncement(body.get(0));
                    listener.onFetched(body.get(0));
                } else {
                    cache.removeAnnouncement(id);
                    listener.onFetched(null);
                }
            }

            @Override
            public void onError(String message) {
                cache.findAnnouncement(id, saved -> {
                    if (saved != null) {
                        listener.onFetched(saved);
                    } else {
                        listener.onError(message);
                    }
                });
            }
        });
    }


    private Map<String, Object> buildAnnouncementPatch(Announcement a) {
        Map<String, Object> m = new HashMap<>();
        m.put("title", a.getTitle());
        m.put("what", a.getWhat());
        m.put("who", a.getWho());
        m.put("when_info", a.getWhen());
        m.put("event_date", a.getEventDate());
        m.put("where_info", a.getWhere());
        m.put("why", a.getWhy());
        m.put("additional_info", a.getAdditionalInfo());
        m.put("image_url", a.getImageUrl());
        m.put("visibility", a.getVisibility());
        m.put("department", a.getDepartment());
        m.put("priority", a.getPriority());
        m.put("category", a.getCategory());
        m.put("status", a.getStatus());
        m.put("updated_at", a.getUpdatedAt());
        return m;
    }

    public interface OnCompleteListener {
        void onSuccess(String id);
        void onError(String message);
    }

    public interface OnSimpleListener {
        void onSuccess();
        void onError(String message);
    }

    public interface OnAnnouncementFetchedListener {
        void onFetched(Announcement announcement);
        void onError(String message);
    }
}
