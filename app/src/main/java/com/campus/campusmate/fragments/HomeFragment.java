package com.campus.campusmate.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.campus.campusmate.R;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class HomeFragment extends BaseFragment<FragmentHomeBinding> {

    private AnnouncementViewModel announcementViewModel;
    private EventViewModel eventViewModel;
    private AnnouncementAdapter announcementAdapter;
    private EventAdapter eventAdapter;
    private SessionManager session;

    /** Non-null only while the Quick Stats dialog is on screen; lets the persistent
     *  observers below refresh its counts live instead of relying on stale getValue() reads. */
    private View statsDialogView;

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
        
        binding.cardActionStats.setOnClickListener(v -> showQuickStats());

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

            // Quick Stats' "Upcoming Events" count piggybacks on this same full list; the
            // observer below simply no-ops while the dialog is closed (statsDialogView == null).
            if (statsDialogView != null) {
                long now = System.currentTimeMillis();
                int upcoming = 0;
                for (Event e : list) {
                    if (e.getStartDate() >= now) upcoming++;
                }
                ((TextView) statsDialogView.findViewById(R.id.tv_stat_events))
                        .setText(String.valueOf(upcoming));
            }
        });

        // Quick Stats counts: these observers live for the fragment's whole view lifecycle
        // (registered once here, not per dialog-open) and simply no-op while the dialog is closed.
        announcementViewModel.approvedAnnouncementsFull.observe(getViewLifecycleOwner(), list -> {
            if (statsDialogView == null || list == null) return;
            ((TextView) statsDialogView.findViewById(R.id.tv_stat_announcements))
                    .setText(String.valueOf(list.size()));
        });

        announcementViewModel.userAnnouncements.observe(getViewLifecycleOwner(), list -> {
            if (statsDialogView == null || list == null) return;
            updateMyPostsCounts();
        });

        eventViewModel.userEvents.observe(getViewLifecycleOwner(), list -> {
            if (statsDialogView == null || list == null) return;
            updateMyPostsCounts();
        });
    }

    private void showQuickStats() {
        statsDialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_quick_stats, null);

        ((TextView) statsDialogView.findViewById(R.id.tv_stat_department))
                .setText(session.getUserDepartment() != null ? session.getUserDepartment() : "General");

        boolean canPost = session.canPost();
        statsDialogView.findViewById(R.id.section_my_posts).setVisibility(canPost ? View.VISIBLE : View.GONE);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Quick Stats")
                .setView(statsDialogView)
                .setPositiveButton(R.string.btn_close, null)
                .setOnDismissListener(d -> statsDialogView = null)
                .create();
        dialog.show();

        // Trigger fresh loads; the persistent observers above push results into this dialog
        // as they arrive (announcements/events full lists get fetched a page/chunk at a time).
        announcementViewModel.loadApprovedAnnouncementsFull(session.getUserDepartment());
        if (canPost) {
            String uid = session.getUserId();
            announcementViewModel.loadUserAnnouncements(uid);
            eventViewModel.loadUserEvents(uid);
        }
    }

    private void updateMyPostsCounts() {
        if (statsDialogView == null) return;
        List<Announcement> myAnnouncements = announcementViewModel.userAnnouncements.getValue();
        List<Event> myEvents = eventViewModel.userEvents.getValue();

        int pending = 0, approved = 0, rejected = 0;
        if (myAnnouncements != null) {
            for (Announcement a : myAnnouncements) {
                if (a.isPending()) pending++;
                else if (a.isApproved()) approved++;
                else if (a.isRejected()) rejected++;
            }
        }
        if (myEvents != null) {
            for (Event e : myEvents) {
                if (e.isPending()) pending++;
                else if (e.isApproved()) approved++;
                else if (e.isRejected()) rejected++;
            }
        }

        ((TextView) statsDialogView.findViewById(R.id.tv_stat_pending)).setText(String.valueOf(pending));
        ((TextView) statsDialogView.findViewById(R.id.tv_stat_approved)).setText(String.valueOf(approved));
        ((TextView) statsDialogView.findViewById(R.id.tv_stat_rejected)).setText(String.valueOf(rejected));
    }

    private void loadData() {
        String dept = session.getUserDepartment();
        announcementViewModel.loadApprovedAnnouncements(dept);
        eventViewModel.loadApprovedEvents(dept);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Covers returning from Create/Detail activities launched from Home:
        // the host Activity resumes (and so do we) even while this tab is hidden.
        if (announcementViewModel != null && eventViewModel != null) loadData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // MainActivity keeps bottom-nav tabs alive via show()/hide() rather than
        // recreating them, so this covers switching back to this tab (onResume
        // above only fires on Activity-level resume, e.g. returning from another
        // screen — not on a same-Activity show()/hide() tab switch).
        if (!hidden && announcementViewModel != null && eventViewModel != null) {
            loadData();
        }
    }
}
