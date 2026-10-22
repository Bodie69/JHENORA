package com.campus.campusmate.fragments.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.campus.campusmate.R;
import com.campus.campusmate.activities.EventDetailActivity;
import com.campus.campusmate.adapters.EventAdapter;
import com.campus.campusmate.databinding.FragmentAdminContentBinding;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.models.FeedFilter;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.PostFilters;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.viewmodels.EventViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class AdminEventsFragment extends Fragment {

    private FragmentAdminContentBinding binding;
    private EventViewModel viewModel;
    private EventAdapter adapter;
    private SessionManager session;
    private List<Event> allItems = new ArrayList<>();

    private static final String STATE_STATUS = "status_filter";
    private static final String STATE_SCOPE = "scope_filter";
    /** Required post status, or {@code null} for any. */
    @Nullable private String statusFilter;
    /** {@link FeedFilter#SCOPE_ALL}, {@link FeedFilter#SCOPE_GENERAL} or a department code. */
    private String scopeFilter = FeedFilter.SCOPE_ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminContentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
        session = new SessionManager(requireContext());

        binding.etSearch.setHint("Search Events");

        if (savedInstanceState != null) {
            statusFilter = savedInstanceState.getString(STATE_STATUS);
            scopeFilter = savedInstanceState.getString(STATE_SCOPE, FeedFilter.SCOPE_ALL);
        }

        setupRecyclerView();
        setupFilters();
        setupSearch();
        setupObservers();

        viewModel.loadAllEvents();
    }

    @Override
    public void onResume() {
        super.onResume();
        // This tab's ViewPager2 page is kept alive (not recreated) while another tab is
        // shown, so without this, posts created/approved/deleted elsewhere would sit stale
        // here until the app is force-restarted.
        if (viewModel != null) viewModel.loadAllEvents();
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
            public void onItemLongClick(Event e, View anchor) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Admin Actions")
                        .setItems(new String[]{"Delete Event"}, (dialog, which) -> {
                            if (which == 0) confirmDelete(e);
                        })
                        .show();
            }
        });
        binding.rvContent.setAdapter(adapter);
    }

    private void confirmDelete(Event e) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_confirm_delete)
                .setMessage(R.string.dialog_confirm_delete_msg)
                .setPositiveButton(R.string.btn_delete, (d, w) -> {
                    viewModel.deleteEvent(e.getId(), session.getUserId(), session.getUserName());
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void setupFilters() {
        binding.chipStatusAll.setOnClickListener(v -> setStatusFilter(null));
        binding.chipPending.setOnClickListener(v -> setStatusFilter(Event.STATUS_PENDING));
        binding.chipApproved.setOnClickListener(v -> setStatusFilter(Event.STATUS_APPROVED));
        binding.chipRejected.setOnClickListener(v -> setStatusFilter(Event.STATUS_REJECTED));
        binding.chipScopeAll.setOnClickListener(v -> setScopeFilter(FeedFilter.SCOPE_ALL));
        binding.chipGeneral.setOnClickListener(v -> setScopeFilter(FeedFilter.SCOPE_GENERAL));
        binding.chipBsit.setOnClickListener(v -> setScopeFilter(Constants.DEPT_BSIT));
        binding.chipBtled.setOnClickListener(v -> setScopeFilter(Constants.DEPT_BTLED));
        binding.chipEduc.setOnClickListener(v -> setScopeFilter(Constants.DEPT_EDUC));
        updateChipUI();
    }

    private void setStatusFilter(@Nullable String status) {
        statusFilter = status;
        updateChipUI();
        filter(binding.etSearch.getText().toString());
    }

    private void setScopeFilter(String scope) {
        scopeFilter = scope;
        updateChipUI();
        filter(binding.etSearch.getText().toString());
    }

    private void updateChipUI() {
        // Non-checkable chips: selected state drives the colour and TalkBack's announcement.
        binding.chipStatusAll.setSelected(statusFilter == null);
        binding.chipPending.setSelected(Event.STATUS_PENDING.equals(statusFilter));
        binding.chipApproved.setSelected(Event.STATUS_APPROVED.equals(statusFilter));
        binding.chipRejected.setSelected(Event.STATUS_REJECTED.equals(statusFilter));
        binding.chipScopeAll.setSelected(FeedFilter.SCOPE_ALL.equals(scopeFilter));
        binding.chipGeneral.setSelected(FeedFilter.SCOPE_GENERAL.equals(scopeFilter));
        binding.chipBsit.setSelected(Constants.DEPT_BSIT.equals(scopeFilter));
        binding.chipBtled.setSelected(Constants.DEPT_BTLED.equals(scopeFilter));
        binding.chipEduc.setSelected(Constants.DEPT_EDUC.equals(scopeFilter));
    }

    private void setupSearch() {
        binding.ivSearch.setOnClickListener(v -> {
            binding.etSearch.requestFocus();
            UiUtils.hideKeyboard(binding.etSearch);
            filter(binding.etSearch.getText().toString());
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                UiUtils.hideKeyboard(v);
                filter(v.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void setupObservers() {
        viewModel.events.observe(getViewLifecycleOwner(), list -> {
            allItems = list != null ? list : new ArrayList<>();
            filter(binding.etSearch.getText().toString());
        });
    }

    private void filter(String query) {
        List<Event> filtered = new ArrayList<>();
        for (Event e : allItems) {
            if (PostFilters.matches(e.getStatus(), e.getVisibility(), e.getDepartment(),
                    statusFilter, scopeFilter)
                    && PostFilters.matchesQuery(query, e.getTitle(), e.getAuthorName(),
                    e.getWhat(), e.getWhere(), e.getDepartment())) {
                filtered.add(e);
            }
        }
        adapter.submitList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_STATUS, statusFilter);
        outState.putString(STATE_SCOPE, scopeFilter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
