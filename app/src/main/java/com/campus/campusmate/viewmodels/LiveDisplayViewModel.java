package com.campus.campusmate.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.base.BaseViewModel;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.repositories.AnnouncementRepository;
import com.campus.campusmate.repositories.DepartmentRepository;
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
        // The kiosk is campus-wide, not tied to one department — it shows every approved
        // post, General and department-scoped alike, with each card's own badge naming
        // which (see LiveDisplayActivity#setBadges).
        announcementRepo.observeAllApprovedAnnouncements(announcements);
        eventRepo.observeAllApprovedEvents(events);
        // The kiosk runs for days without restarting, so pick up logo changes on each refresh.
        DepartmentRepository.getInstance().refreshLogoCache();
    }
}
