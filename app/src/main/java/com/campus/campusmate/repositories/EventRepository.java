package com.campus.campusmate.repositories;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.models.AuditLog;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.models.FeedFilter;
import com.campus.campusmate.utils.PostFilters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository class responsible for managing Event data via the Supabase API. Approved events it
 * fetches are saved to the {@link #cache}, and the reads behind the Events, Home, Calendar and
 * detail screens fall back to that copy when offline.
 */
public class EventRepository extends BaseRepository {

    private static EventRepository instance;

    private EventRepository() {}

    public static synchronized EventRepository getInstance() {
        if (instance == null) instance = new EventRepository();
        return instance;
    }

    @Override
    public void init(Context context) {
        super.init(context);
    }

    public void createEvent(Event event, OnCompleteListener listener) {
        String id = UUID.randomUUID().toString();
        event.setId(id);
        enqueue(dataApi.insertEvent(event, "return=minimal"), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                logAudit(event.getAuthorId(), event.getAuthorName(),
                        AuditLog.ACTION_CREATE, AuditLog.TARGET_EVENT,
                        id, "Event created: " + event.getTitle());
                listener.onSuccess(id);
            }

            @Override
            public void onError(String message) {
                listener.onError("Failed to create event");
            }
        });
    }

    public void updateEvent(Event event, OnCompleteListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + event.getId());
        Map<String, Object> fields = buildEventPatch(event);
        enqueue(dataApi.patchEvent(filter, fields, "return=minimal"), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                cache.updateEvent(event);
                listener.onSuccess(event.getId());
            }

            @Override
            public void onError(String message) {
                listener.onError("Update failed");
            }
        });
    }

    public void deleteEvent(String id, String userId, String userName, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        enqueue(dataApi.deleteEvent(filter), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                logAudit(userId, userName, AuditLog.ACTION_DELETE,
                        AuditLog.TARGET_EVENT, id, "Event deleted");
                cache.removeEvent(id);
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
                listener.onError("Delete failed");
            }
        });
    }

    public void approveEvent(String id, String adminId, String adminName, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", Event.STATUS_APPROVED);
        fields.put("approved_by", adminName);
        fields.put("approved_at", System.currentTimeMillis());
        fields.put("updated_at", System.currentTimeMillis());
        enqueue(dataApi.patchEvent(filter, fields, "return=minimal"), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                logAudit(adminId, adminName, AuditLog.ACTION_APPROVE,
                        AuditLog.TARGET_EVENT, id, "Event approved");
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
                listener.onError("Approval failed");
            }
        });
    }

    public void rejectEvent(String id, String adminId, String adminName,
                            String reason, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", Event.STATUS_REJECTED);
        fields.put("rejection_reason", reason);
        fields.put("updated_at", System.currentTimeMillis());
        enqueue(dataApi.patchEvent(filter, fields, "return=minimal"), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                logAudit(adminId, adminName, AuditLog.ACTION_REJECT,
                        AuditLog.TARGET_EVENT, id, "Event rejected: " + reason);
                cache.removeEvent(id);
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
                listener.onError("Rejection failed");
            }
        });
    }

    /**
     * Loads the complete approved-and-visible-to-{@code userDept} event set, walked in
     * {@link com.campus.campusmate.utils.Constants#BULK_FETCH_CHUNK_SIZE}-row chunks rather than
     * one unbounded request. Used by screens that need the whole list at once (home preview,
     * live display rotation). For the scrollable Events feed, use {@link #fetchApprovedEventsPage}.
     */
    public void observeApprovedEvents(String userDept, MutableLiveData<List<Event>> liveData) {
        Map<String, String> filter = approvedEventsFilter(userDept);
        fetchAllPages(
                (range, callback) -> enqueue(dataApi.getEvents(filter, range), callback),
                cachingResult(liveData, userDept, false));
    }

    /**
     * Like {@link #observeApprovedEvents}, but for the campus-wide Live Display kiosk: every
     * approved event regardless of visibility/department. The kiosk isn't scoped to any one
     * department — each card's own department badge (BSIT/BTLED/EDUC) identifies what a
     * department-only event is about, the same way a General one is labeled "General".
     */
    public void observeAllApprovedEvents(MutableLiveData<List<Event>> liveData) {
        Map<String, String> filter = allApprovedEventsFilter();
        fetchAllPages(
                (range, callback) -> enqueue(dataApi.getEvents(filter, range), callback),
                cachingResult(liveData, null, true));
    }

    /**
     * Posts a complete approved list to {@code liveData}, saving it for offline use; if the
     * request fails, posts the saved copy instead, or {@code null} when nothing is saved.
     */
    private OnResult<List<Event>> cachingResult(MutableLiveData<List<Event>> liveData,
            String userDept, boolean everyDepartment) {
        return new OnResult<List<Event>>() {
            @Override
            public void onSuccess(List<Event> body) {
                cache.saveEvents(body);
                liveData.postValue(body);
            }

            @Override
            public void onError(String message) {
                cache.loadEvents(userDept, everyDepartment,
                        saved -> liveData.postValue(saved.isEmpty() ? null : saved));
            }
        };
    }

    /**
     * Replaces the offline copy with the full current set: what {@code userDept} can see, or
     * every approved event for the Live Display ({@code everyDepartment}). Blocks; for
     * background workers. Also drops saved events deleted or unpublished meanwhile.
     *
     * @throws IOException if any part of the set can't be fetched (the copy is left as it was)
     */
    public void syncOfflineCacheNow(String userDept, boolean everyDepartment) throws IOException {
        Map<String, String> filter = everyDepartment
                ? allApprovedEventsFilter() : approvedEventsFilter(userDept);
        cache.replaceEventsNow(fetchAllPagesNow(range -> dataApi.getEvents(filter, range)));
    }

    private Map<String, String> allApprovedEventsFilter() {
        Map<String, String> filter = new HashMap<>();
        filter.put("status", "eq." + Event.STATUS_APPROVED);
        filter.put("order", "start_date.asc");
        return filter;
    }

    /**
     * Loads one {@code limit}-row page of approved events starting at {@code offset}, for
     * infinite-scroll feeds, narrowed and ordered by {@code feed} on the server so filters and
     * search reach past the loaded pages. Unlike {@link #observeApprovedEvents}, the caller
     * decides when (or whether) to fetch the next page.
     */
    public void fetchApprovedEventsPage(String userDept, FeedFilter feed, int offset, int limit,
                                         OnPageListener<Event> listener) {
        Map<String, String> filter = approvedEventsFilter(userDept);
        long now = System.currentTimeMillis();
        List<String> timeframe = new ArrayList<>();
        String order;
        if (FeedFilter.TIME_UPCOMING.equals(feed.timeframe)) {
            // Not ended yet. end_date is 0 when unset, so a future start also counts.
            timeframe.add("or(end_date.gte." + now + ",start_date.gte." + now + ")");
            order = "start_date.asc,id.asc";
        } else if (FeedFilter.TIME_PAST.equals(feed.timeframe)) {
            timeframe.add("or(end_date.lt." + now + ",end_date.is.null)");
            timeframe.add("start_date.lt." + now);
            order = "start_date.desc,id.desc";
        } else {
            order = "start_date.desc,id.desc";
        }
        applyFeedFilter(filter, feed, timeframe, "title", "what", "where_info", "author_name");
        filter.put("order", order);
        String range = offset + "-" + (offset + limit - 1);
        enqueue(dataApi.getEvents(filter, range), new OnResult<List<Event>>() {
            @Override
            public void onSuccess(List<Event> body) {
                cache.saveEvents(body);
                listener.onLoaded(body != null ? body : new ArrayList<>());
            }

            @Override
            public void onError(String message) {
                // Offline, the first page is replaced by every saved match; later pages just fail.
                if (offset > 0) {
                    listener.onError(message);
                    return;
                }
                cache.loadEvents(userDept, false, saved -> {
                    if (saved.isEmpty()) {
                        listener.onError(message);
                    } else {
                        listener.onLoadedFromCache(PostFilters.filterEventFeed(saved, feed, System.currentTimeMillis()));
                    }
                });
            }
        });
    }

    private Map<String, String> approvedEventsFilter(String userDept) {
        Map<String, String> filter = new HashMap<>();
        filter.put("status", "eq." + Event.STATUS_APPROVED);
        if (userDept != null && !userDept.isEmpty()) {
            filter.put("or", "(visibility.eq.general,department.eq." + userDept + ")");
        } else {
            filter.put("visibility", "eq.general");
        }
        // Explicit order is required for chunk/page offsets to be stable across requests.
        filter.put("order", "start_date.asc");
        return filter;
    }

    public void observePendingEvents(MutableLiveData<List<Event>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("status", "eq." + Event.STATUS_PENDING);
        filter.put("order", "created_at.asc");
        fetchAllPages(
                (range, callback) -> enqueue(dataApi.getEvents(filter, range), callback),
                new OnResult<List<Event>>() {
                    @Override
                    public void onSuccess(List<Event> body) {
                        liveData.postValue(body);
                    }

                    @Override
                    public void onError(String message) {
                        liveData.postValue(null);
                    }
                });
    }

    public void observeUserEvents(String userId, MutableLiveData<List<Event>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("author_id", "eq." + userId);
        filter.put("order", "created_at.desc");
        fetchAllPages(
                (range, callback) -> enqueue(dataApi.getEvents(filter, range), callback),
                new OnResult<List<Event>>() {
                    @Override
                    public void onSuccess(List<Event> body) {
                        liveData.postValue(body);
                    }

                    @Override
                    public void onError(String message) {
                        liveData.postValue(null);
                    }
                });
    }

    public void fetchEventById(String id, OnEventFetchedListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        enqueue(dataApi.getEvents(filter, null), new OnResult<List<Event>>() {
            @Override
            public void onSuccess(List<Event> body) {
                if (body != null && !body.isEmpty()) {
                    cache.updateEvent(body.get(0));
                    listener.onFetched(body.get(0));
                } else {
                    cache.removeEvent(id);
                    listener.onFetched(null);
                }
            }

            @Override
            public void onError(String message) {
                cache.findEvent(id, saved -> {
                    if (saved != null) {
                        listener.onFetched(saved);
                    } else {
                        listener.onError(message);
                    }
                });
            }
        });
    }

    public void observeAllEvents(MutableLiveData<List<Event>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("order", "created_at.desc");
        fetchAllPages(
                (range, callback) -> enqueue(dataApi.getEvents(filter, range), callback),
                new OnResult<List<Event>>() {
                    @Override
                    public void onSuccess(List<Event> body) {
                        liveData.postValue(body);
                    }

                    @Override
                    public void onError(String message) {
                        liveData.postValue(null);
                    }
                });
    }


    private Map<String, Object> buildEventPatch(Event e) {
        Map<String, Object> m = new HashMap<>();
        m.put("title", e.getTitle());
        m.put("what", e.getWhat());
        m.put("who", e.getWho());
        m.put("when_info", e.getWhen());
        m.put("where_info", e.getWhere());
        m.put("why", e.getWhy());
        m.put("registration_link", e.getRegistrationLink());
        m.put("image_url", e.getImageUrl());
        m.put("visibility", e.getVisibility());
        m.put("department", e.getDepartment());
        m.put("status", e.getStatus());
        m.put("start_date", e.getStartDate());
        m.put("end_date", e.getEndDate());
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

    public interface OnEventFetchedListener {
        void onFetched(Event event);
        void onError(String message);
    }
}
