package com.campus.campusmate.activities;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.campus.campusmate.R;
import com.campus.campusmate.base.BaseActivity;
import com.campus.campusmate.databinding.ActivityEventDetailBinding;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.DateUtils;
import com.campus.campusmate.utils.DepartmentLogos;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.viewmodels.EventViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class EventDetailActivity extends BaseActivity<ActivityEventDetailBinding> {

    private EventViewModel viewModel;
    private String eventId;

    @Override
    protected ActivityEventDetailBinding inflateBinding(LayoutInflater inflater) {
        return ActivityEventDetailBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        eventId = getIntent().getStringExtra(Constants.EXTRA_EVENT_ID);
        if (eventId == null) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(EventViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());
        viewModel.loadEvent(eventId);
    }

    @Override
    protected void setupObservers() {
        viewModel.isLoading.observe(this, loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.loadedEvent.observe(this, e -> {
            if (e != null) bindData(e);
        });

        viewModel.successMessage.observe(this, msgEvent -> {
            String msg = msgEvent != null ? msgEvent.getContentIfNotHandled() : null;
            if (msg != null) {
                UiUtils.showToast(this, getString(R.string.success_post_deleted));
                finish();
            }
        });

        viewModel.errorMessage.observe(this, msgEvent -> {
            String msg = msgEvent != null ? msgEvent.getContentIfNotHandled() : null;
            if (msg != null) UiUtils.showToast(this, msg);
        });
    }

    private void bindData(Event e) {
        binding.tvTitle.setText(e.getTitle());
        binding.tvAuthor.setText(e.getAuthorName() != null ? e.getAuthorName() : "");
        binding.tvDate.setText(DateUtils.formatDateTime(e.getCreatedAt()));

        boolean isGeneral = Event.VISIBILITY_GENERAL.equals(e.getVisibility());
        String dept = isGeneral ? getString(R.string.label_general) : e.getDepartment();
        binding.tvDept.setText(dept != null ? dept : "");
        DepartmentLogos.bind(binding.tvDept, isGeneral ? Constants.DEPT_GENERAL : e.getDepartment());

        if (e.getStartDate() > 0) {
            binding.tvEventDate.setText(DateUtils.formatDateTime(e.getStartDate()));
            binding.tvEventDate.setVisibility(View.VISIBLE);
        }

        setField(binding.tvWhatValue, e.getWhat());
        setField(binding.tvWhereValue, e.getWhere());

        if (e.getRegistrationLink() != null && !e.getRegistrationLink().isEmpty()) {
            binding.btnRegister.setVisibility(View.VISIBLE);
            binding.btnRegister.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(e.getRegistrationLink()));
                    startActivity(intent);
                } catch (Exception ex) {
                    UiUtils.showToast(this, "Cannot open registration link");
                }
            });
        } else {
            binding.btnRegister.setVisibility(View.GONE);
        }

        if (e.getImageUrl() != null && !e.getImageUrl().isEmpty()) {
            binding.ivCover.setVisibility(View.VISIBLE);
            Glide.with(this).load(e.getImageUrl()).into(binding.ivCover);
            binding.ivCover.setOnClickListener(v -> FullScreenImageActivity.start(this, e.getImageUrl()));
        } else {
            binding.ivCover.setVisibility(View.GONE);
        }

        boolean canManage = session.isAdmin()
                || (session.getUserId() != null && session.getUserId().equals(e.getAuthorId()));
        binding.layoutOwnerActions.setVisibility(canManage ? View.VISIBLE : View.GONE);
        if (canManage) {
            binding.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(this, CreateEventActivity.class);
                intent.putExtra(Constants.EXTRA_EVENT_ID, e.getId());
                intent.putExtra(Constants.EXTRA_EDIT_MODE, true);
                startActivity(intent);
                finish();
            });
            binding.btnDelete.setOnClickListener(v -> confirmDelete(e));
        }
    }

    private void confirmDelete(Event e) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_confirm_delete)
                .setMessage(R.string.dialog_confirm_delete_msg)
                .setPositiveButton(R.string.btn_delete, (d, w) ->
                        viewModel.deleteEvent(e.getId(), session.getUserId(), session.getUserName()))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void setField(android.widget.TextView tv, String value) {
        if (tv == null) return;
        if (value != null && !value.isEmpty()) {
            tv.setText(value);
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.GONE);
            View row = (View) tv.getParent();
            if (row != null) row.setVisibility(View.GONE);
        }
    }
}
