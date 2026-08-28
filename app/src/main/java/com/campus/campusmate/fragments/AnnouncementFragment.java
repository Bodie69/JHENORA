package com.campus.campusmate.fragments;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.campus.campusmate.R;
import com.campus.campusmate.activities.AnnouncementDetailActivity;
import com.campus.campusmate.activities.CreateAnnouncementActivity;
import com.campus.campusmate.adapters.AnnouncementAdapter;
import com.campus.campusmate.base.BaseFragment;
import com.campus.campusmate.databinding.FragmentAnnouncementsBinding;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.viewmodels.AnnouncementViewModel;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementFragment extends BaseFragment<FragmentAnnouncementsBinding> {

    private AnnouncementViewModel viewModel;
    private AnnouncementAdapter adapter;
    private SessionManager session;

    private String currentDeptFilter = "All";

    @Override
    protected FragmentAnnouncementsBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentAnnouncementsBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        session = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(AnnouncementViewModel.class);

        setupRecyclerView();
        setupSearch();
        setupFilters();
        setupFab();
        setupSwipeRefresh();
        
        refreshList();
    }

    private void setupRecyclerView() {
        adapter = new AnnouncementAdapter(requireContext());
        adapter.setListener(new AnnouncementAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Announcement a) {
                Intent intent = new Intent(requireContext(), AnnouncementDetailActivity.class);
                intent.putExtra(Constants.EXTRA_ANNOUNCEMENT_ID, a.getId());
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(Announcement a, View anchor) {}
        });
        binding.rvAnnouncements.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilters() {
        binding.chipAll.setOnClickListener(v -> { currentDeptFilter = "All"; updateChipUI(); refreshList(); });
        binding.chipGeneral.setOnClickListener(v -> { currentDeptFilter = Constants.VISIBILITY_GENERAL; updateChipUI(); refreshList(); });
        binding.chipBsit.setOnClickListener(v -> { currentDeptFilter = Constants.DEPT_BSIT; updateChipUI(); refreshList(); });
        binding.chipBtled.setOnClickListener(v -> { currentDeptFilter = Constants.DEPT_BTLED; updateChipUI(); refreshList(); });
        binding.chipEduc.setOnClickListener(v -> { currentDeptFilter = Constants.DEPT_EDUC; updateChipUI(); refreshList(); });

        restrictDepartmentChips();
        updateChipUI();
    }

    private void restrictDepartmentChips() {
        if (session.isStudent()) {
            String myDept = session.getUserDepartment();
            if (Constants.DEPT_BSIT.equals(myDept)) {
                binding.chipBtled.setVisibility(View.GONE);
                binding.chipEduc.setVisibility(View.GONE);
            } else if (Constants.DEPT_BTLED.equals(myDept)) {
                binding.chipBsit.setVisibility(View.GONE);
                binding.chipEduc.setVisibility(View.GONE);
            } else if (Constants.DEPT_EDUC.equals(myDept)) {
                binding.chipBsit.setVisibility(View.GONE);
                binding.chipBtled.setVisibility(View.GONE);
            }
        }
    }

    private void updateChipUI() {
        setChipSelected(binding.chipAll, "All".equals(currentDeptFilter));
        setChipSelected(binding.chipGeneral, Constants.VISIBILITY_GENERAL.equals(currentDeptFilter));
        setChipSelected(binding.chipBsit, Constants.DEPT_BSIT.equals(currentDeptFilter));
        setChipSelected(binding.chipBtled, Constants.DEPT_BTLED.equals(currentDeptFilter));
        setChipSelected(binding.chipEduc, Constants.DEPT_EDUC.equals(currentDeptFilter));
    }

    private void setChipSelected(TextView chip, boolean isSelected) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    private void setupFab() {
        binding.fab.setVisibility(session.canPost() ? View.VISIBLE : View.GONE);
        binding.fab.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateAnnouncementActivity.class)));
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.loadApprovedAnnouncements(session.getUserDepartment());
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    @Override
    protected void setupObservers() {
        viewModel.announcements.observe(getViewLifecycleOwner(), list -> {
            refreshList();
        });
    }

    private void refreshList() {
        List<Announcement> raw = viewModel.announcements.getValue();
        if (raw == null) return;

        String query = binding.etSearch.getText().toString().toLowerCase();
        List<Announcement> filtered = new ArrayList<>();
        for (Announcement a : raw) {
            boolean deptMatch = "All".equals(currentDeptFilter) 
                    || currentDeptFilter.equals(a.getDepartment())
                    || (Constants.VISIBILITY_GENERAL.equals(currentDeptFilter) && Constants.VISIBILITY_GENERAL.equals(a.getVisibility()));
            
            boolean searchMatch = query.isEmpty() 
                    || a.getTitle().toLowerCase().contains(query) 
                    || (a.getWhat() != null && a.getWhat().toLowerCase().contains(query));
            
            if (deptMatch && searchMatch) {
                filtered.add(a);
            }
        }
        adapter.submitList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
