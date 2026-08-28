package com.campus.campusmate.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.campus.campusmate.activities.AnnouncementDetailActivity;
import com.campus.campusmate.activities.CreateAnnouncementActivity;
import com.campus.campusmate.activities.CreateEventActivity;
import com.campus.campusmate.activities.EventDetailActivity;
import com.campus.campusmate.activities.MainActivity;
import com.campus.campusmate.adapters.AnnouncementAdapter;
import com.campus.campusmate.adapters.EventAdapter;
import com.campus.campusmate.base.BaseFragment;
import com.campus.campusmate.databinding.FragmentHomeBinding;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.viewmodels.AnnouncementViewModel;
import com.campus.campusmate.viewmodels.EventViewModel;

import java.util.List;

public class HomeFragment extends BaseFragment<FragmentHomeBinding> {

    private AnnouncementViewModel announcementViewModel;
    private EventViewModel eventViewModel;
    private AnnouncementAdapter announcementAdapter;
    private EventAdapter eventAdapter;
    private SessionManager session;

    @Override
    protected FragmentHomeBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentHomeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        session = new SessionManager(requireContext());

        announcementViewModel = new ViewModelProvider(requireActivity())
                .get(AnnouncementViewModel.class);
        eventViewModel = new ViewModelProvider(requireActivity())
                .get(EventViewModel.class);

        setupRecyclerViews();
        setupQuickActions();
        loadData();
    }

    private void setupRecyclerViews() {
        announcementAdapter = new AnnouncementAdapter(requireContext());
        announcementAdapter.setListener(new AnnouncementAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Announcement a) {
                Intent intent = new Intent(requireContext(), AnnouncementDetailActivity.class);
                intent.putExtra(Constants.EXTRA_ANNOUNCEMENT_ID, a.getId());
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(Announcement a, View anchor) {}
        });
        binding.rvAnnouncements.setAdapter(announcementAdapter);

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
        binding.rvEvents.setAdapter(eventAdapter);
    }

    private void setupQuickActions() {
        boolean canPost = session.canPost();
        binding.cardActionAnnounce.setVisibility(canPost ? View.VISIBLE : View.GONE);
        binding.cardActionEvents.setVisibility(canPost ? View.VISIBLE : View.GONE);

        binding.cardActionAnnounce.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateAnnouncementActivity.class)));
        binding.cardActionEvents.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateEventActivity.class)));
        binding.cardActionCalendar.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).navigateToCalendar();
            }
        });
        
        binding.cardActionStats.setOnClickListener(v -> {
            // Optional: navigate to stats or just show a message
            if (requireActivity() instanceof MainActivity) {
                 ((MainActivity) requireActivity()).navigateToProfile(); // Fallback to profile for now
            }
        });

        binding.tvSeeAllAnnouncements.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).navigateToAnnouncements();
            }
        });
        binding.tvSeeAllEvents.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).navigateToEvents();
            }
        });
    }

    @Override
    protected void setupObservers() {
        announcementViewModel.announcements.observe(getViewLifecycleOwner(), list -> {
            if (list == null) return;
            List<Announcement> preview = list.subList(0, Math.min(list.size(), Constants.HOME_PREVIEW_COUNT));
            announcementAdapter.submitList(preview);
            binding.tvNoAnnouncements.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        eventViewModel.events.observe(getViewLifecycleOwner(), list -> {
            if (list == null) return;
            List<Event> preview = list.subList(0, Math.min(list.size(), Constants.HOME_PREVIEW_COUNT));
            eventAdapter.submitList(preview);
            binding.tvNoEvents.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void loadData() {
        String dept = session.getUserDepartment();
        announcementViewModel.loadApprovedAnnouncements(dept);
        eventViewModel.loadApprovedEvents(dept);
    }
}
