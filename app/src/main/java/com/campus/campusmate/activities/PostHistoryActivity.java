package com.campus.campusmate.activities;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.campus.campusmate.adapters.AnnouncementAdapter;
import com.campus.campusmate.base.BaseActivity;
import com.campus.campusmate.databinding.ActivityPostHistoryBinding;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.viewmodels.AnnouncementViewModel;

/**
 * Dedicated list of the current user's own announcements, opened from the Profile screen's
 * "Post History" row (previously an unbounded RecyclerView embedded directly in the profile page).
 */
public class PostHistoryActivity extends BaseActivity<ActivityPostHistoryBinding> {

    private AnnouncementViewModel viewModel;
    private AnnouncementAdapter adapter;
    private boolean firstLoad = true;

    @Override
    protected ActivityPostHistoryBinding inflateBinding(LayoutInflater inflater) {
        return ActivityPostHistoryBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AnnouncementViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new AnnouncementAdapter(this);
        adapter.setListener(new AnnouncementAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Announcement a) {
                Intent intent = new Intent(PostHistoryActivity.this, AnnouncementDetailActivity.class);
                intent.putExtra(Constants.EXTRA_ANNOUNCEMENT_ID, a.getId());
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(Announcement a, View anchor) {}
        });
        binding.rvPostHistory.setAdapter(adapter);

        String userId = session.getUserId();
        if (userId != null) viewModel.loadUserAnnouncements(userId);
    }

    @Override
    protected void setupObservers() {
        viewModel.userAnnouncements.observe(this, list -> {
            firstLoad = false;
            binding.progressBar.setVisibility(View.GONE);
            adapter.submitList(list);
            binding.tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Picks up edits/deletes made from AnnouncementDetailActivity without re-showing the spinner.
        if (!firstLoad) {
            String userId = session.getUserId();
            if (userId != null) viewModel.loadUserAnnouncements(userId);
        }
    }
}
