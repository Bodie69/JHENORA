package com.campus.campusmate.fragments;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.lifecycle.ViewModelProvider;

import com.campus.campusmate.activities.EventDetailActivity;
import com.campus.campusmate.adapters.EventAdapter;
import com.campus.campusmate.base.BaseFragment;
import com.campus.campusmate.databinding.FragmentCalendarBinding;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.viewmodels.EventViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends BaseFragment<FragmentCalendarBinding> {

    private EventViewModel viewModel;
    private EventAdapter eventAdapter;
    private long selectedDate;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

    @Override
    protected FragmentCalendarBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentCalendarBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
        
        selectedDate = System.currentTimeMillis();
        binding.tvSelectedDate.setText(dateFormat.format(selectedDate));
        
        setupCalendar();
        setupRecyclerView();
    }

    private void setupCalendar() {
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            selectedDate = cal.getTimeInMillis();
            binding.tvSelectedDate.setText(dateFormat.format(selectedDate));
            refreshEventsForDate();
        });
    }

    private void setupRecyclerView() {
        eventAdapter = new EventAdapter(requireContext());
        eventAdapter.setListener(new EventAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Event e) {
                Intent intent = new Intent(requireContext(), EventDetailActivity.class);
                intent.putExtra(Constants.EXTRA_EVENT_ID, e.getId());
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(Event e, View anchor) {}
        });
        binding.rvDayEvents.setAdapter(eventAdapter);
    }

    @Override
    protected void setupObservers() {
        viewModel.events.observe(getViewLifecycleOwner(), list -> {
            refreshEventsForDate();
        });
    }

    private void refreshEventsForDate() {
        List<Event> all = viewModel.events.getValue();
        if (all == null) return;

        List<Event> filtered = new ArrayList<>();
        Calendar sel = Calendar.getInstance();
        sel.setTimeInMillis(selectedDate);

        for (Event e : all) {
            Calendar start = Calendar.getInstance();
            start.setTimeInMillis(e.getStartDate());
            
            if (start.get(Calendar.YEAR) == sel.get(Calendar.YEAR) &&
                start.get(Calendar.DAY_OF_YEAR) == sel.get(Calendar.DAY_OF_YEAR)) {
                filtered.add(e);
            }
        }
        eventAdapter.submitList(filtered);
        binding.tvNoEvents.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
