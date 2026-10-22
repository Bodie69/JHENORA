package com.campus.campusmate.fragments.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.campus.campusmate.R;
import com.campus.campusmate.databinding.FragmentAdminStatisticsBinding;
import com.campus.campusmate.viewmodels.AdminViewModel;
import com.campus.campusmate.viewmodels.AnnouncementViewModel;
import com.campus.campusmate.viewmodels.EventViewModel;
import com.campus.campusmate.views.BarChartView;

import java.util.ArrayList;
import java.util.List;

public class AdminStatisticsFragment extends Fragment {

    private FragmentAdminStatisticsBinding binding;
    private AdminViewModel adminViewModel;
    private AnnouncementViewModel announcementViewModel;
    private EventViewModel eventViewModel;

    private int facultyCount;
    private int studentCount;
    private int orgCount;
    private int announcementCount;
    private int eventCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminStatisticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminViewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);
        announcementViewModel = new ViewModelProvider(requireActivity()).get(AnnouncementViewModel.class);
        eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);

        setupObservers();
        loadData();
    }

    private void loadData() {
        adminViewModel.loadAllUsers();
        announcementViewModel.loadAllAnnouncements();
        eventViewModel.loadAllEvents();
    }

    @Override
    public void onResume() {
        super.onResume();
        // This tab's ViewPager2 page is kept alive (not recreated) while another tab is
        // shown, so without this, counts would freeze at whatever they were on first open
        // instead of reflecting activity elsewhere.
        if (adminViewModel != null && announcementViewModel != null && eventViewModel != null) {
            loadData();
        }
    }

    private void setupObservers() {
        adminViewModel.totalUsers.observe(getViewLifecycleOwner(), count ->
                binding.tvTotalUsers.setText(String.valueOf(count != null ? count : 0)));

        adminViewModel.totalFaculty.observe(getViewLifecycleOwner(), count -> {
            facultyCount = count != null ? count : 0;
            binding.tvTotalFaculty.setText(String.valueOf(facultyCount));
            updateUserChart();
        });

        adminViewModel.totalStudents.observe(getViewLifecycleOwner(), count -> {
            studentCount = count != null ? count : 0;
            binding.tvTotalStudents.setText(String.valueOf(studentCount));
            updateUserChart();
        });

        adminViewModel.totalOrganizations.observe(getViewLifecycleOwner(), count -> {
            orgCount = count != null ? count : 0;
            binding.tvTotalOrgs.setText(String.valueOf(orgCount));
            updateUserChart();
        });

        adminViewModel.pendingCount.observe(getViewLifecycleOwner(), count ->
                binding.tvPendingCount.setText(String.valueOf(count != null ? count : 0)));

        announcementViewModel.announcements.observe(getViewLifecycleOwner(), list -> {
            announcementCount = list != null ? list.size() : 0;
            binding.tvTotalAnnouncements.setText(String.valueOf(announcementCount));
            updateContentChart();
        });

        eventViewModel.events.observe(getViewLifecycleOwner(), list -> {
            eventCount = list != null ? list.size() : 0;
            binding.tvTotalEvents.setText(String.valueOf(eventCount));
            updateContentChart();
        });
    }

    private void updateUserChart() {
        List<BarChartView.Entry> entries = new ArrayList<>();
        entries.add(new BarChartView.Entry("Faculty", facultyCount, requireContext().getColor(R.color.role_faculty)));
        entries.add(new BarChartView.Entry("Students", studentCount, requireContext().getColor(R.color.role_student)));
        entries.add(new BarChartView.Entry("Orgs", orgCount, requireContext().getColor(R.color.role_organization)));
        binding.chartUsers.setEntries(entries);
    }

    private void updateContentChart() {
        List<BarChartView.Entry> entries = new ArrayList<>();
        entries.add(new BarChartView.Entry("Announcements", announcementCount, requireContext().getColor(R.color.primary)));
        entries.add(new BarChartView.Entry("Events", eventCount, requireContext().getColor(R.color.success)));
        binding.chartContent.setEntries(entries);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
