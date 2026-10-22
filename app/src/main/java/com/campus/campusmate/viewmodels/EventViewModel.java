package com.campus.campusmate.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.base.BaseViewModel;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.models.FeedFilter;
import com.campus.campusmate.repositories.BaseRepository.OnPageListener;
import com.campus.campusmate.repositories.EventRepository;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.SupabaseStorage;

import java.util.ArrayList;
import java.util.List;

public class EventViewModel extends BaseViewModel {

    private final EventRepository repo;

    /**
     * The complete approved-events set for {@code userDept} (see {@link #loadApprovedEvents}).
     * Kept as a full, non-paginated list because {@code CalendarFragment} needs every event to
     * mark arbitrary dates, not just whatever page the Events tab feed has scrolled to.
     */
    public final MutableLiveData<List<Event>> events = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<Event>> pendingEvents = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<Event>> userEvents = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Event> loadedEvent = new MutableLiveData<>();
    public final MutableLiveData<com.campus.campusmate.utils.Event<String>> successMessage = new MutableLiveData<>();

    /** Infinite-scroll feed backing the Events tab list; see {@link #loadEventsFeed}. */
    public final MutableLiveData<List<Event>> eventsFeed = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Boolean> isLoadingMoreEvents = new MutableLiveData<>(false);
    /** True while {@link #eventsFeed} is the saved offline copy because the server was unreachable. */
    public final MutableLiveData<Boolean> isShowingSaved = new MutableLiveData<>(false);

    private String feedDept;
    private int feedOffset = 0;
    private boolean feedHasMore = true;
    private boolean feedFetching = false;
    /** Feed filter/search/sort. Lives here so it survives rotation and fragment recreation. */
    private FeedFilter feedFilter = FeedFilter.defaults();
    /** Bumped on every feed restart so responses for a superseded filter are dropped. */
    private int feedGeneration = 0;

    public EventViewModel(@NonNull Application application) {
        super(application);
        repo = EventRepository.getInstance();
        repo.init(application);
    }

    /** Loads a single event by id; result is posted to {@link #loadedEvent}. */
    public void loadEvent(String id) {
        setLoading(true);
        repo.fetchEventById(id, new EventRepository.OnEventFetchedListener() {
            @Override
            public void onFetched(Event event) {
                setLoading(false);
                loadedEvent.postValue(event);
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    /**
     * Uploads {@code imageUri} (if present) then creates/updates the event.
     * Mirrors the previous Activity-side orchestration so the Activity only triggers this
     * and observes {@link #successMessage}/{@link #errorMessage}/{@link #isLoading}.
     */
    public void submitWithImage(Event event, Uri imageUri, boolean isEdit) {
        setLoading(true);
        if (imageUri == null) {
            submitOrUpdate(event, isEdit);
            return;
        }
        SupabaseStorage.uploadImage(getApplication(), imageUri, "events", new SupabaseStorage.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                event.setImageUrl(publicUrl);
                submitOrUpdate(event, isEdit);
            }

            @Override
            public void onFailure(String errorMessage) {
                _errorMessage.postValue(new com.campus.campusmate.utils.Event<>("Image upload failed, submitting without image"));
                submitOrUpdate(event, isEdit);
            }
        });
    }

    private void submitOrUpdate(Event event, boolean isEdit) {
        if (isEdit) {
            updateEvent(event);
        } else {
            submitEvent(event);
        }
    }

    /** Loads the complete approved-events set for {@code userDept} (Calendar, Home preview). */
    public void loadApprovedEvents(String userDept) {
        repo.observeApprovedEvents(userDept, events);
    }

    public FeedFilter getFeedFilter() {
        return feedFilter;
    }

    /** Applies {@code filter} to the Events tab feed, reloading from page one if it changed. */
    public void setFeedFilter(FeedFilter filter) {
        if (filter.equals(feedFilter)) return;
        feedFilter = filter;
        loadEventsFeed(feedDept);
    }

    /** (Re)starts the Events tab's infinite-scroll feed from the first page for {@code userDept}. */
    public void loadEventsFeed(String userDept) {
        feedDept = userDept;
        feedOffset = 0;
        feedHasMore = true;
        feedFetching = true;
        int generation = ++feedGeneration;
        isLoadingMoreEvents.postValue(false);
        repo.fetchApprovedEventsPage(userDept, feedFilter, 0, Constants.PAGE_SIZE, new OnPageListener<Event>() {
            @Override
            public void onLoaded(List<Event> page) {
                if (generation != feedGeneration) return;
                feedFetching = false;
                feedOffset = page.size();
                feedHasMore = page.size() == Constants.PAGE_SIZE;
                isShowingSaved.postValue(false);
                eventsFeed.postValue(page);
            }

            @Override
            public void onLoadedFromCache(List<Event> items) {
                if (generation != feedGeneration) return;
                feedFetching = false;
                feedOffset = items.size();
                feedHasMore = false;
                isShowingSaved.postValue(true);
                eventsFeed.postValue(items);
            }

            @Override
            public void onError(String message) {
                if (generation != feedGeneration) return;
                feedFetching = false;
                eventsFeed.postValue(null);
            }
        });
    }

    /**
     * Appends the next page of the Events tab feed, if one hasn't already been requested and
     * the previous page wasn't short. Safe to call repeatedly from a scroll listener.
     */
    public void loadMoreEventsFeed() {
        if (feedFetching || !feedHasMore) return;
        feedFetching = true;
        int generation = feedGeneration;
        isLoadingMoreEvents.postValue(true);
        repo.fetchApprovedEventsPage(feedDept, feedFilter, feedOffset, Constants.PAGE_SIZE, new OnPageListener<Event>() {
            @Override
            public void onLoaded(List<Event> page) {
                if (generation != feedGeneration) return;
                feedFetching = false;
                isLoadingMoreEvents.postValue(false);
                feedOffset += page.size();
                feedHasMore = page.size() == Constants.PAGE_SIZE;
                List<Event> current = eventsFeed.getValue();
                List<Event> combined = current != null ? new ArrayList<>(current) : new ArrayList<>();
                combined.addAll(page);
                eventsFeed.postValue(combined);
            }

            @Override
            public void onError(String message) {
                if (generation != feedGeneration) return;
                feedFetching = false;
                isLoadingMoreEvents.postValue(false);
            }
        });
    }

    public void loadPendingEvents() {
        repo.observePendingEvents(pendingEvents);
    }

    public void loadUserEvents(String userId) {
        repo.observeUserEvents(userId, userEvents);
    }

    public void loadAllEvents() {
        repo.observeAllEvents(events);
    }

    public void submitEvent(Event event) {
        setLoading(true);
        repo.createEvent(event, new EventRepository.OnCompleteListener() {
            @Override
            public void onSuccess(String id) {
                setLoading(false);
                successMessage.postValue(new com.campus.campusmate.utils.Event<>("Event submitted for approval"));
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    public void updateEvent(Event event) {
        setLoading(true);
        repo.updateEvent(event, new EventRepository.OnCompleteListener() {
            @Override
            public void onSuccess(String id) {
                setLoading(false);
                successMessage.postValue(new com.campus.campusmate.utils.Event<>("Event updated"));
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    public void deleteEvent(String id, String userId, String userName) {
        setLoading(true);
        repo.deleteEvent(id, userId, userName, new EventRepository.OnSimpleListener() {
            @Override
            public void onSuccess() {
                setLoading(false);
                successMessage.postValue(new com.campus.campusmate.utils.Event<>("Event deleted"));
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    public void approveEvent(String id, String adminId, String adminName) {
        setLoading(true);
        repo.approveEvent(id, adminId, adminName, new EventRepository.OnSimpleListener() {
            @Override
            public void onSuccess() {
                setLoading(false);
                successMessage.postValue(new com.campus.campusmate.utils.Event<>("Event approved"));
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    public void rejectEvent(String id, String adminId, String adminName, String reason) {
        setLoading(true);
        repo.rejectEvent(id, adminId, adminName, reason, new EventRepository.OnSimpleListener() {
            @Override
            public void onSuccess() {
                setLoading(false);
                successMessage.postValue(new com.campus.campusmate.utils.Event<>("Event rejected"));
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    public List<Event> getEventsOnDate(List<Event> all, long dayStart, long dayEnd) {
        if (all == null) return new ArrayList<>();
        List<Event> result = new ArrayList<>();
        for (Event e : all) {
            if (e.getStartDate() > 0) {
                long start = e.getStartDate();
                long end = e.getEndDate() > 0 ? e.getEndDate() : start;
                if (start <= dayEnd && end >= dayStart) {
                    result.add(e);
                }
            }
        }
        return result;
    }
}
