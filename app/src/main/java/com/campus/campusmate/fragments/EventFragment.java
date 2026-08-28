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
import com.campus.campusmate.activities.CreateEventActivity;
import com.campus.campusmate.activities.EventDetailActivity;
import com.campus.campusmate.adapters.EventAdapter;
import com.campus.campusmate.base.BaseFragment;
import com.campus.campusmate.databinding.FragmentEventsBinding;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.viewmodels.EventViewModel;

import java.util.ArrayList;
import java.util.List;

public class EventFragment extends BaseFragment<FragmentEventsBinding> {

    private EventViewModel viewModel;
    private EventAdapter adapter;
    private SessionManager session;

    private String currentDeptFilter = "All";

    @Override
    protected FragmentEventsBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentEventsBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        session = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);

        setupRecyclerView();
        setupSearch();
        setupFilters();
        setupFab();
        setupSwipeRefresh();
        
        refreshList();
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(requireContext());
        adapter.setListener(new EventAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Event e) {
                Intent intent = new Intent(requireContext(), EventDetailActivity.class);
                intent.putExtra(Constants.EXTRA_EVENT_ID, e.getId());
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(Event e, View anchor) {}
        });
        binding.rvEvents.setAdapter(adapter);
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
                startActivity(new Intent(requireContext(), CreateEventActivity.class)));
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.loadApprovedEvents(session.getUserDepartment());
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    @Override
    protected void setupObservers() {
        viewModel.events.observe(getViewLifecycleOwner(), list -> {
            refreshList();
        });
    }

    private void refreshList() {
        List<Event> raw = viewModel.events.getValue();
        if (raw == null) return;

        String query = binding.etSearch.getText().toString().toLowerCase();
        List<Event> filtered = new ArrayList<>();
        for (Event e : raw) {
            boolean deptMatch = "All".equals(currentDeptFilter) 
                    || currentDeptFilter.equals(e.getDepartment())
                    || (Constants.VISIBILITY_GENERAL.equals(currentDeptFilter) && Constants.VISIBILITY_GENERAL.equals(e.getVisibility()));
            
            boolean searchMatch = query.isEmpty() 
                    || e.getTitle().toLowerCase().contains(query) 
                    || (e.getWhat() != null && e.getWhat().toLowerCase().contains(query));
            
            if (deptMatch && searchMatch) {
                filtered.add(e);
            }
        }
        adapter.submitList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
