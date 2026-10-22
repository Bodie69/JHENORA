package com.campus.campusmate.fragments;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.campus.campusmate.R;
import com.campus.campusmate.activities.AnnouncementDetailActivity;
import com.campus.campusmate.activities.EventDetailActivity;
import com.campus.campusmate.adapters.CalendarAgendaAdapter;
import com.campus.campusmate.adapters.CalendarAgendaItem;
import com.campus.campusmate.adapters.CalendarDateAdapter;
import com.campus.campusmate.base.BaseFragment;
import com.campus.campusmate.databinding.FragmentCalendarBinding;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.DateUtils;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.viewmodels.AnnouncementViewModel;
import com.campus.campusmate.viewmodels.EventViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Calendar agenda screen. Announcements and Events are both surfaced here, linked
 * together in one day-by-day list: an event lands on the day it's scheduled for,
 * an announcement lands on the day it was posted (it has no structured schedule).
 */
public class CalendarFragment extends BaseFragment<FragmentCalendarBinding> {

    /** How many days (including today) the rolling date strip shows. */
    private static final int VISIBLE_DAYS = 14;

    private EventViewModel eventViewModel;
    private AnnouncementViewModel announcementViewModel;
    private CalendarDateAdapter dateAdapter;
    private CalendarAgendaAdapter agendaAdapter;
    private final List<CalendarDateAdapter.DateItem> dateItems = new ArrayList<>();
    private final List<CalendarAgendaItem> allItems = new ArrayList<>();
    private int selectedPosition = 0;
    private final SimpleDateFormat shortDateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

    @Override
    protected FragmentCalendarBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentCalendarBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
        announcementViewModel = new ViewModelProvider(requireActivity()).get(AnnouncementViewModel.class);

        setupDateStrip();
        setupAgendaList();

        // Load fresh data on open rather than relying on Home having populated
        // these (shared, activity-scoped) view models first.
        loadFreshData();
    }

    private void loadFreshData() {
        String dept = new SessionManager(requireContext()).getUserDepartment();
        eventViewModel.loadApprovedEvents(dept);
        announcementViewModel.loadApprovedAnnouncements(dept);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Covers returning from Create/Edit/Detail activities launched from a
        // tapped agenda card: the host Activity resumes (and so do we) even
        // while this tab is hidden.
        if (eventViewModel != null && announcementViewModel != null) loadFreshData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // MainActivity keeps bottom-nav tabs alive via show()/hide() rather than
        // recreating them, so this covers switching back to this tab (onResume
        // above only fires on Activity-level resume, e.g. returning from another
        // screen — not on a same-Activity show()/hide() tab switch).
        if (!hidden && eventViewModel != null && announcementViewModel != null) {
            loadFreshData();
        }
    }

    private void setupDateStrip() {
        dateAdapter = new CalendarDateAdapter(requireContext(), dateItems);
        dateAdapter.setListener(position -> {
            selectedPosition = position;
            dateAdapter.setSelectedPosition(position);
            binding.rvDates.smoothScrollToPosition(position);
            refreshAgendaForSelectedDate();
        });
        binding.rvDates.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvDates.setAdapter(dateAdapter);
    }

    private void setupAgendaList() {
        agendaAdapter = new CalendarAgendaAdapter(requireContext());
        agendaAdapter.setListener(item -> {
            Intent intent = item.type == CalendarAgendaItem.Type.EVENT
                    ? new Intent(requireContext(), EventDetailActivity.class)
                    : new Intent(requireContext(), AnnouncementDetailActivity.class);
            String extraKey = item.type == CalendarAgendaItem.Type.EVENT
                    ? Constants.EXTRA_EVENT_ID : Constants.EXTRA_ANNOUNCEMENT_ID;
            intent.putExtra(extraKey, item.id);
            startActivity(intent);
        });
        binding.rvDayEvents.setAdapter(agendaAdapter);
    }

    @Override
    protected void setupObservers() {
        eventViewModel.events.observe(getViewLifecycleOwner(), list -> rebuildAgenda());
        announcementViewModel.announcements.observe(getViewLifecycleOwner(), list -> rebuildAgenda());
    }

    private long dayTimestamp(int offsetDays) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, offsetDays);
        return DateUtils.getStartOfDay(cal.getTimeInMillis());
    }

    private String labelFor(int offsetDays, long timestamp) {
        if (offsetDays == 0) return getString(R.string.calendar_today);
        if (offsetDays == 1) return getString(R.string.calendar_tomorrow);
        return shortDateFormat.format(new Date(timestamp));
    }

    private void rebuildAgenda() {
        allItems.clear();

        List<Event> events = eventViewModel.events.getValue();
        if (events != null) {
            for (Event e : events) {
                if (e.getStartDate() > 0) allItems.add(CalendarAgendaItem.fromEvent(e));
            }
        }

        List<Announcement> announcements = announcementViewModel.announcements.getValue();
        if (announcements != null) {
            for (Announcement a : announcements) {
                allItems.add(CalendarAgendaItem.fromAnnouncement(a));
            }
        }

        rebuildDateStrip();
        refreshAgendaForSelectedDate();
    }

    private void rebuildDateStrip() {
        long previouslySelectedDay = dateItems.isEmpty() || selectedPosition >= dateItems.size()
                ? dayTimestamp(0) : dateItems.get(selectedPosition).timestamp;

        dateItems.clear();
        for (int i = 0; i < VISIBLE_DAYS; i++) {
            long day = dayTimestamp(i);
            int count = countItemsOnDay(day);
            dateItems.add(new CalendarDateAdapter.DateItem(day, labelFor(i, day), count));
        }

        selectedPosition = 0;
        for (int i = 0; i < dateItems.size(); i++) {
            if (DateUtils.isSameDay(dateItems.get(i).timestamp, previouslySelectedDay)) {
                selectedPosition = i;
                break;
            }
        }

        dateAdapter.submitList(dateItems);
        dateAdapter.setSelectedPosition(selectedPosition);
    }

    private int countItemsOnDay(long day) {
        int count = 0;
        for (CalendarAgendaItem item : allItems) {
            if (DateUtils.isSameDay(item.dayTimestamp, day)) count++;
        }
        return count;
    }

    private void refreshAgendaForSelectedDate() {
        if (dateItems.isEmpty()) return;

        long selectedDay = dateItems.get(selectedPosition).timestamp;
        List<CalendarAgendaItem> filtered = new ArrayList<>();
        for (CalendarAgendaItem item : allItems) {
            if (DateUtils.isSameDay(item.dayTimestamp, selectedDay)) {
                filtered.add(item);
            }
        }
        Collections.sort(filtered, Comparator.comparingLong(i -> i.sortTimestamp));

        agendaAdapter.submitList(filtered);
        binding.tvNoEvents.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
