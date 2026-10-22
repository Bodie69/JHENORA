package com.campus.campusmate.fragments;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campus.campusmate.R;
import com.campus.campusmate.activities.AnnouncementDetailActivity;
import com.campus.campusmate.activities.CreateAnnouncementActivity;
import com.campus.campusmate.adapters.AnnouncementAdapter;
import com.campus.campusmate.base.BaseFragment;
import com.campus.campusmate.databinding.FragmentAnnouncementsBinding;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.FeedFilter;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.NetworkMonitor;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.viewmodels.AnnouncementViewModel;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementFragment extends BaseFragment<FragmentAnnouncementsBinding> {

    private AnnouncementViewModel viewModel;
    private AnnouncementAdapter adapter;
    private SessionManager session;

    private static final long SEARCH_DEBOUNCE_MS = 350;

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

        viewModel.loadApprovedAnnouncements(session.getUserDepartment());
        refreshList();
    }

    private void reloadFeed() {
        if (viewModel != null && session != null) {
            viewModel.loadApprovedAnnouncements(session.getUserDepartment());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Covers returning from CreateAnnouncementActivity/AnnouncementDetailActivity:
        // the host Activity resumes (and so do we) even while this tab is hidden.
        reloadFeed();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // MainActivity keeps bottom-nav tabs alive via show()/hide() rather than
        // recreating them, so this covers switching back to this tab (onResume
        // above only fires on Activity-level resume, e.g. returning from another
        // screen — not on a same-Activity show()/hide() tab switch).
        if (!hidden) reloadFeed();
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

        binding.rvAnnouncements.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) return;
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;
                int visibleThreshold = 5;
                if (lm.findLastVisibleItemPosition() >= adapter.getItemCount() - 1 - visibleThreshold) {
                    viewModel.loadMoreAnnouncements();
                }
            }
        });
    }

    private void setupSearch() {
        // Restore the ViewModel's query, which outlives this view, before listening for edits.
        binding.etSearch.setText(viewModel.getFeedFilter().query);
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Search runs on the server, so wait for a pause in typing.
                binding.etSearch.removeCallbacks(applySearch);
                binding.etSearch.postDelayed(applySearch, SEARCH_DEBOUNCE_MS);
            }
        });
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            binding.etSearch.removeCallbacks(applySearch);
            applySearch.run();
            UiUtils.hideKeyboard(v);
            return true;
        });
    }

    private final Runnable applySearch = () -> {
        if (binding == null) return;
        Editable text = binding.etSearch.getText();
        updateFilter(viewModel.getFeedFilter().withQuery(text != null ? text.toString() : ""));
    };

    /** Applies a changed filter: the ViewModel reloads page one from the server. */
    private void updateFilter(FeedFilter filter) {
        if (!filter.equals(viewModel.getFeedFilter())) {
            binding.swipeRefresh.setRefreshing(true);
            viewModel.setFeedFilter(filter);
        }
        updateChipUI();
    }

    private void setupFilters() {
        binding.chipAll.setOnClickListener(v -> updateFilter(viewModel.getFeedFilter().withScope(FeedFilter.SCOPE_ALL)));
        binding.chipGeneral.setOnClickListener(v -> updateFilter(viewModel.getFeedFilter().withScope(FeedFilter.SCOPE_GENERAL)));
        binding.chipBsit.setOnClickListener(v -> updateFilter(viewModel.getFeedFilter().withScope(Constants.DEPT_BSIT)));
        binding.chipBtled.setOnClickListener(v -> updateFilter(viewModel.getFeedFilter().withScope(Constants.DEPT_BTLED)));
        binding.chipEduc.setOnClickListener(v -> updateFilter(viewModel.getFeedFilter().withScope(Constants.DEPT_EDUC)));
        binding.chipNewest.setOnClickListener(v -> updateFilter(viewModel.getFeedFilter().withSort(FeedFilter.SORT_NEWEST)));
        binding.chipOldest.setOnClickListener(v -> updateFilter(viewModel.getFeedFilter().withSort(FeedFilter.SORT_OLDEST)));
        binding.chipUrgent.setOnClickListener(v -> {
            FeedFilter f = viewModel.getFeedFilter();
            updateFilter(f.withUrgentOnly(!f.urgentOnly));
        });

        restrictDepartmentChips();
        updateChipUI();
    }

    /**
     * The feed only ever holds General posts plus the user's own department's (see the
     * repository's audience filter), so other departments' chips could only show an empty list.
     */
    private void restrictDepartmentChips() {
        String myDept = session.getUserDepartment();
        binding.chipBsit.setVisibility(Constants.DEPT_BSIT.equals(myDept) ? View.VISIBLE : View.GONE);
        binding.chipBtled.setVisibility(Constants.DEPT_BTLED.equals(myDept) ? View.VISIBLE : View.GONE);
        binding.chipEduc.setVisibility(Constants.DEPT_EDUC.equals(myDept) ? View.VISIBLE : View.GONE);

        FeedFilter filter = viewModel.getFeedFilter();
        if (filter.isDepartmentScope() && !filter.scope.equals(myDept)) {
            viewModel.setFeedFilter(filter.withScope(FeedFilter.SCOPE_ALL));
        }
    }

    private void updateChipUI() {
        FeedFilter f = viewModel.getFeedFilter();
        setChipSelected(binding.chipAll, FeedFilter.SCOPE_ALL.equals(f.scope));
        setChipSelected(binding.chipGeneral, FeedFilter.SCOPE_GENERAL.equals(f.scope));
        setChipSelected(binding.chipBsit, Constants.DEPT_BSIT.equals(f.scope));
        setChipSelected(binding.chipBtled, Constants.DEPT_BTLED.equals(f.scope));
        setChipSelected(binding.chipEduc, Constants.DEPT_EDUC.equals(f.scope));
        setChipSelected(binding.chipNewest, FeedFilter.SORT_NEWEST.equals(f.sort));
        setChipSelected(binding.chipOldest, FeedFilter.SORT_OLDEST.equals(f.sort));
        setChipSelected(binding.chipUrgent, f.urgentOnly);
    }

    private void setChipSelected(Chip chip, boolean isSelected) {
        // Chips are non-checkable (see Widget.JHENORA.Chip); the selected state drives both the
        // colour and TalkBack's "selected" announcement.
        chip.setSelected(isSelected);
    }

    private void setupFab() {
        binding.fab.setVisibility(session.canPost() ? View.VISIBLE : View.GONE);
        binding.fab.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateAnnouncementActivity.class)));
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() ->
                viewModel.loadApprovedAnnouncements(session.getUserDepartment()));
    }

    @Override
    protected void setupObservers() {
        // announcements is only re-posted once the network call actually completes (see
        // AnnouncementViewModel#loadApprovedAnnouncements), so this is also the right place to
        // end the pull-to-refresh spin — ending it synchronously in setupSwipeRefresh() instead
        // made the indicator vanish immediately, before the refresh had actually happened.
        viewModel.announcements.observe(getViewLifecycleOwner(), list -> {
            binding.swipeRefresh.setRefreshing(false);
            refreshList();
        });
        viewModel.isLoadingMore.observe(getViewLifecycleOwner(), loading ->
                binding.progressLoadMore.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));
        viewModel.isShowingSaved.observe(getViewLifecycleOwner(), saved ->
                binding.tvOfflineBanner.setVisibility(Boolean.TRUE.equals(saved) ? View.VISIBLE : View.GONE));
        // Swap the saved copy for live posts as soon as the connection is back.
        NetworkMonitor.getInstance(requireContext()).isOnline().observe(getViewLifecycleOwner(), online -> {
            if (Boolean.TRUE.equals(online) && Boolean.TRUE.equals(viewModel.isShowingSaved.getValue())) {
                reloadFeed();
            }
        });
    }

    private void refreshList() {
        List<Announcement> raw = viewModel.announcements.getValue();
        if (raw == null) return;

        // Filtering, search and sorting already happened on the server (see FeedFilter).
        adapter.submitList(new ArrayList<>(raw));
        binding.tvEmpty.setText(viewModel.getFeedFilter().equals(FeedFilter.defaults())
                ? R.string.no_announcements : R.string.no_results);
        binding.tvEmpty.setVisibility(raw.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
