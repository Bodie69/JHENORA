package com.campus.campusmate.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.base.BaseViewModel;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.repositories.AnnouncementRepository;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementViewModel extends BaseViewModel {

    private final AnnouncementRepository repo;

    public final MutableLiveData<List<Announcement>> announcements = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<Announcement>> pendingAnnouncements = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<Announcement>> userAnnouncements = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<String> successMessage = new MutableLiveData<>();
    public final MutableLiveData<String> filterDept = new MutableLiveData<>("All");
    public final MutableLiveData<String> sortOrder = new MutableLiveData<>("Latest");
    public final MutableLiveData<String> searchQuery = new MutableLiveData<>("");

    public AnnouncementViewModel(@NonNull Application application) {
        super(application);
        repo = AnnouncementRepository.getInstance();
        repo.init(application);
    }

    public void loadApprovedAnnouncements(String userDept) {
        repo.observeApprovedAnnouncements(userDept, announcements);
    }

    public void loadPendingAnnouncements() {
        repo.observePendingAnnouncements(pendingAnnouncements);
    }

    public void loadUserAnnouncements(String userId) {
        repo.observeUserAnnouncements(userId, userAnnouncements);
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
                successMessage.postValue("Announcement submitted for approval");
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
                successMessage.postValue("Announcement updated");
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
                successMessage.postValue("Announcement deleted");
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
                successMessage.postValue("Announcement approved");
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
                successMessage.postValue("Announcement rejected");
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    public List<Announcement> getFilteredAnnouncements(List<Announcement> all, String dept, String sort, String query) {
        if (all == null) return new ArrayList<>();
        List<Announcement> filtered = new ArrayList<>();
        String q = query != null ? query.toLowerCase().trim() : "";

        for (Announcement a : all) {
            boolean matchDept = "All".equals(dept)
                    || Announcement.VISIBILITY_GENERAL.equals(a.getVisibility()) && "General".equals(dept)
                    || a.getDepartment() != null && a.getDepartment().equals(dept);
            boolean matchQuery = q.isEmpty()
                    || (a.getTitle() != null && a.getTitle().toLowerCase().contains(q))
                    || (a.getWhat() != null && a.getWhat().toLowerCase().contains(q))
                    || (a.getAuthorName() != null && a.getAuthorName().toLowerCase().contains(q));
            if (matchDept && matchQuery) filtered.add(a);
        }

        filtered.sort((a, b) -> {
            if ("Priority".equals(sort)) {
                int wA = priorityWeight(a.getPriority());
                int wB = priorityWeight(b.getPriority());
                if (wA != wB) return Integer.compare(wB, wA);
            }
            return Long.compare(b.getCreatedAt(), a.getCreatedAt());
        });
        return filtered;
    }

    private int priorityWeight(String p) {
        if (Announcement.PRIORITY_URGENT.equals(p)) return 3;
        if (Announcement.PRIORITY_HIGH.equals(p)) return 2;
        return 1;
    }
}
