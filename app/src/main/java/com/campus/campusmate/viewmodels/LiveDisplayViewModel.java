package com.campus.campusmate.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.base.BaseViewModel;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.repositories.AnnouncementRepository;
import com.campus.campusmate.repositories.EventRepository;

import java.util.List;

/**
 * Backs {@link com.campus.campusmate.activities.LiveDisplayActivity}, which previously
 * queried {@link AnnouncementRepository} and {@link EventRepository} directly.
 */
public class LiveDisplayViewModel extends BaseViewModel {

    private final AnnouncementRepository announcementRepo;
    private final EventRepository eventRepo;

    public final MutableLiveData<List<Announcement>> announcements = new MutableLiveData<>();
    public final MutableLiveData<List<Event>> events = new MutableLiveData<>();

    public LiveDisplayViewModel(@NonNull Application application) {
        super(application);
        announcementRepo = AnnouncementRepository.getInstance();
        announcementRepo.init(application);
        eventRepo = EventRepository.getInstance();
        eventRepo.init(application);
    }

    public void loadData() {
        announcementRepo.observeApprovedAnnouncements(null, announcements);
        eventRepo.observeApprovedEvents(null, events);
    }
}
