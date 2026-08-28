package com.campus.campusmate.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.base.BaseViewModel;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.repositories.EventRepository;

import java.util.ArrayList;
import java.util.List;

public class EventViewModel extends BaseViewModel {

    private final EventRepository repo;

    public final MutableLiveData<List<Event>> events = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<Event>> pendingEvents = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<Event>> userEvents = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<String> successMessage = new MutableLiveData<>();
    public final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    public final MutableLiveData<String> filterDept = new MutableLiveData<>("All");

    public EventViewModel(@NonNull Application application) {
        super(application);
        repo = EventRepository.getInstance();
        repo.init(application);
    }

    public void loadApprovedEvents(String userDept) {
        repo.observeApprovedEvents(userDept, events);
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
                successMessage.postValue("Event submitted for approval");
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
                successMessage.postValue("Event updated");
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
                successMessage.postValue("Event deleted");
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
                successMessage.postValue("Event approved");
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
                successMessage.postValue("Event rejected");
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    public List<Event> getFilteredEvents(List<Event> all, String dept, String query) {
        if (all == null) return new ArrayList<>();
        List<Event> filtered = new ArrayList<>();
        String q = query != null ? query.toLowerCase().trim() : "";

        for (Event e : all) {
            boolean matchDept = "All".equals(dept)
                    || Event.VISIBILITY_GENERAL.equals(e.getVisibility()) && "General".equals(dept)
                    || e.getDepartment() != null && e.getDepartment().equals(dept);
            boolean matchQuery = q.isEmpty()
                    || (e.getTitle() != null && e.getTitle().toLowerCase().contains(q))
                    || (e.getWhere() != null && e.getWhere().toLowerCase().contains(q))
                    || (e.getAuthorName() != null && e.getAuthorName().toLowerCase().contains(q));
            if (matchDept && matchQuery) filtered.add(e);
        }
        filtered.sort((a, b) -> Long.compare(a.getStartDate(), b.getStartDate()));
        return filtered;
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
