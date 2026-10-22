package com.campus.campusmate.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.base.BaseViewModel;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.FeedFilter;
import com.campus.campusmate.repositories.AnnouncementRepository;
import com.campus.campusmate.repositories.BaseRepository.OnPageListener;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.SupabaseStorage;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementViewModel extends BaseViewModel {

    private final AnnouncementRepository repo;

    public final MutableLiveData<List<Announcement>> announcements = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<Announcement>> pendingAnnouncements = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<Announcement>> userAnnouncements = new MutableLiveData<>(new ArrayList<>());
    /** Complete approved-and-visible set for the Quick Stats count (see {@link #loadApprovedAnnouncementsFull}). */
    public final MutableLiveData<List<Announcement>> approvedAnnouncementsFull = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Announcement> loadedAnnouncement = new MutableLiveData<>();
    public final MutableLiveData<com.campus.campusmate.utils.Event<String>> successMessage = new MutableLiveData<>();

    /** Whether a next-page fetch for {@link #announcements} is in flight (feed footer spinner). */
    public final MutableLiveData<Boolean> isLoadingMore = new MutableLiveData<>(false);
    /** True while {@link #announcements} is the saved offline copy because the server was unreachable. */
    public final MutableLiveData<Boolean> isShowingSaved = new MutableLiveData<>(false);

    private String feedDept;
    private int feedOffset = 0;
    private boolean feedHasMore = true;
    private boolean feedFetching = false;
    /** Feed filter/search/sort. Lives here so it survives rotation and fragment recreation. */
    private FeedFilter feedFilter = FeedFilter.defaults();
    /** Bumped on every feed restart so responses for a superseded filter are dropped. */
    private int feedGeneration = 0;

    public AnnouncementViewModel(@NonNull Application application) {
        super(application);
        repo = AnnouncementRepository.getInstance();
        repo.init(application);
    }

    /** Loads a single announcement by id; result is posted to {@link #loadedAnnouncement}. */
    public void loadAnnouncement(String id) {
        setLoading(true);
        repo.fetchAnnouncementById(id, new AnnouncementRepository.OnAnnouncementFetchedListener() {
            @Override
            public void onFetched(Announcement announcement) {
                setLoading(false);
                loadedAnnouncement.postValue(announcement);
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    /**
     * Uploads {@code imageUri} (if present) then creates/updates the announcement.
     * Mirrors the previous Activity-side orchestration so the Activity only triggers this
     * and observes {@link #successMessage}/{@link #errorMessage}/{@link #isLoading}.
     */
    public void submitWithImage(Announcement announcement, Uri imageUri, boolean isEdit) {
        setLoading(true);
        if (imageUri == null) {
            submitOrUpdate(announcement, isEdit);
            return;
        }
        SupabaseStorage.uploadImage(getApplication(), imageUri, "announcements", new SupabaseStorage.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                announcement.setImageUrl(publicUrl);
                submitOrUpdate(announcement, isEdit);
            }

            @Override
            public void onFailure(String errorMessage) {
                _errorMessage.postValue(new com.campus.campusmate.utils.Event<>("Image upload failed, submitting without image"));
                submitOrUpdate(announcement, isEdit);
            }
        });
    }

    private void submitOrUpdate(Announcement announcement, boolean isEdit) {
        if (isEdit) {
            updateAnnouncement(announcement);
        } else {
            submitAnnouncement(announcement);
        }
    }

    public FeedFilter getFeedFilter() {
        return feedFilter;
    }

    /** Applies {@code filter} to the Announcements feed, reloading from page one if it changed. */
    public void setFeedFilter(FeedFilter filter) {
        if (filter.equals(feedFilter)) return;
        feedFilter = filter;
        loadApprovedAnnouncements(feedDept);
    }

    /** (Re)starts the Announcements feed from the first page for {@code userDept}. */
    public void loadApprovedAnnouncements(String userDept) {
        feedDept = userDept;
        feedOffset = 0;
        feedHasMore = true;
        feedFetching = true;
        int generation = ++feedGeneration;
        isLoadingMore.postValue(false);
        repo.fetchApprovedAnnouncementsPage(userDept, feedFilter, 0, Constants.PAGE_SIZE, new OnPageListener<Announcement>() {
            @Override
            public void onLoaded(List<Announcement> page) {
                if (generation != feedGeneration) return;
                feedFetching = false;
                feedOffset = page.size();
                feedHasMore = page.size() == Constants.PAGE_SIZE;
                isShowingSaved.postValue(false);
                announcements.postValue(page);
            }

            @Override
            public void onLoadedFromCache(List<Announcement> items) {
                if (generation != feedGeneration) return;
                feedFetching = false;
                feedOffset = items.size();
                feedHasMore = false;
                isShowingSaved.postValue(true);
                announcements.postValue(items);
            }

            @Override
            public void onError(String message) {
                if (generation != feedGeneration) return;
                feedFetching = false;
                announcements.postValue(null);
            }
        });
    }

    /**
     * Appends the next page of the Announcements feed, if one hasn't already been requested
     * and the previous page wasn't short (i.e. there's more to load). Safe to call repeatedly
     * from a scroll listener — no-ops while a fetch is already in flight or the feed is exhausted.
     */
    public void loadMoreAnnouncements() {
        if (feedFetching || !feedHasMore) return;
        feedFetching = true;
        int generation = feedGeneration;
        isLoadingMore.postValue(true);
        repo.fetchApprovedAnnouncementsPage(feedDept, feedFilter, feedOffset, Constants.PAGE_SIZE, new OnPageListener<Announcement>() {
            @Override
            public void onLoaded(List<Announcement> page) {
                if (generation != feedGeneration) return;
                feedFetching = false;
                isLoadingMore.postValue(false);
                feedOffset += page.size();
                feedHasMore = page.size() == Constants.PAGE_SIZE;
                List<Announcement> current = announcements.getValue();
                List<Announcement> combined = current != null ? new ArrayList<>(current) : new ArrayList<>();
                combined.addAll(page);
                announcements.postValue(combined);
            }

            @Override
            public void onError(String message) {
                if (generation != feedGeneration) return;
                feedFetching = false;
                isLoadingMore.postValue(false);
                // Leave the already-loaded pages in place; the user can retry by scrolling again.
            }
        });
    }

    public void loadPendingAnnouncements() {
        repo.observePendingAnnouncements(pendingAnnouncements);
    }

    public void loadUserAnnouncements(String userId) {
        repo.observeUserAnnouncements(userId, userAnnouncements);
    }

    /** Loads the full approved-and-visible announcement count for {@code userDept} (Quick Stats). */
    public void loadApprovedAnnouncementsFull(String userDept) {
        repo.observeApprovedAnnouncements(userDept, approvedAnnouncementsFull);
    }

    public void loadAllAnnouncements() {
        repo.observeAllAnnouncements(announcements);
    }

    public void submitAnnouncement(Announcement announcement) {
        setLoading(true);
        repo.createAnnouncement(announcement, new AnnouncementRepository.OnCompleteListener() {
            @Override
            public void onSuccess(String id) {
                setLoading(false);
                successMessage.postValue(new com.campus.campusmate.utils.Event<>("Announcement submitted for approval"));
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    public void updateAnnouncement(Announcement announcement) {
        setLoading(true);
        repo.updateAnnouncement(announcement, new AnnouncementRepository.OnCompleteListener() {
            @Override
            public void onSuccess(String id) {
                setLoading(false);
                successMessage.postValue(new com.campus.campusmate.utils.Event<>("Announcement updated"));
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    public void deleteAnnouncement(String id, String userId, String userName) {
        setLoading(true);
        repo.deleteAnnouncement(id, userId, userName, new AnnouncementRepository.OnSimpleListener() {
            @Override
            public void onSuccess() {
                setLoading(false);
                successMessage.postValue(new com.campus.campusmate.utils.Event<>("Announcement deleted"));
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    public void approveAnnouncement(String id, String adminId, String adminName) {
        setLoading(true);
        repo.approveAnnouncement(id, adminId, adminName, new AnnouncementRepository.OnSimpleListener() {
            @Override
            public void onSuccess() {
                setLoading(false);
                successMessage.postValue(new com.campus.campusmate.utils.Event<>("Announcement approved"));
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    public void rejectAnnouncement(String id, String adminId, String adminName, String reason) {
        setLoading(true);
        repo.rejectAnnouncement(id, adminId, adminName, reason, new AnnouncementRepository.OnSimpleListener() {
            @Override
            public void onSuccess() {
                setLoading(false);
                successMessage.postValue(new com.campus.campusmate.utils.Event<>("Announcement rejected"));
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }
}
