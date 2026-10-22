package com.campus.campusmate.activities;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.campus.campusmate.adapters.EventAdapter;
import com.campus.campusmate.base.BaseActivity;
import com.campus.campusmate.databinding.ActivityMyEventsBinding;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.viewmodels.EventViewModel;

/**
 * Dedicated list of the current user's own events, opened from the Profile screen's
 * "My Events" row (previously an unbounded RecyclerView embedded directly in the profile page).
 */
public class MyEventsActivity extends BaseActivity<ActivityMyEventsBinding> {

    private EventViewModel viewModel;
    private EventAdapter adapter;
    private boolean firstLoad = true;

    @Override
    protected ActivityMyEventsBinding inflateBinding(LayoutInflater inflater) {
        return ActivityMyEventsBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(EventViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new EventAdapter(this);
        adapter.setListener(new EventAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Event e) {
                Intent intent = new Intent(MyEventsActivity.this, EventDetailActivity.class);
                intent.putExtra(Constants.EXTRA_EVENT_ID, e.getId());
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(Event e, View anchor) {}
        });
        binding.rvMyEvents.setAdapter(adapter);

        String userId = session.getUserId();
        if (userId != null) viewModel.loadUserEvents(userId);
    }

    @Override
    protected void setupObservers() {
        viewModel.userEvents.observe(this, list -> {
            firstLoad = false;
            binding.progressBar.setVisibility(View.GONE);
            adapter.submitList(list);
            binding.tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!firstLoad) {
            String userId = session.getUserId();
            if (userId != null) viewModel.loadUserEvents(userId);
        }
    }
}
