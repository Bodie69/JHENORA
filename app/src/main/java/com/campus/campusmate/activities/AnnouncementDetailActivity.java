package com.campus.campusmate.activities;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.campus.campusmate.R;
import com.campus.campusmate.base.BaseActivity;
import com.campus.campusmate.databinding.ActivityAnnouncementDetailBinding;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.DateUtils;
import com.campus.campusmate.utils.DepartmentLogos;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.viewmodels.AnnouncementViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AnnouncementDetailActivity extends BaseActivity<ActivityAnnouncementDetailBinding> {

    private AnnouncementViewModel viewModel;
    private String announcementId;

    @Override
    protected ActivityAnnouncementDetailBinding inflateBinding(LayoutInflater inflater) {
        return ActivityAnnouncementDetailBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        announcementId = getIntent().getStringExtra(Constants.EXTRA_ANNOUNCEMENT_ID);
        if (announcementId == null) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(AnnouncementViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());
        viewModel.loadAnnouncement(announcementId);
    }

    @Override
    protected void setupObservers() {
        viewModel.isLoading.observe(this, loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.loadedAnnouncement.observe(this, a -> {
            if (a != null) {
                bindData(a);
            } else {
                UiUtils.showToast(this, getString(R.string.error_generic));
            }
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

    private void bindData(Announcement a) {
        binding.tvTitle.setText(a.getTitle());
        binding.tvAuthor.setText(a.getAuthorName() != null ? a.getAuthorName() : "");
        binding.tvDate.setText(DateUtils.formatDateTime(a.getCreatedAt()));

        boolean isGeneral = Announcement.VISIBILITY_GENERAL.equals(a.getVisibility());
        String dept = isGeneral ? getString(R.string.label_general) : a.getDepartment();
        binding.tvDept.setText(dept != null ? dept : "");
        DepartmentLogos.bind(binding.tvDept, isGeneral ? Constants.DEPT_GENERAL : a.getDepartment());

        UiUtils.applyPriorityBadge(binding.tvPriority,
                a.getPriority() != null ? a.getPriority() : "normal");
        UiUtils.applyStatusBadge(binding.tvStatus,
                a.getStatus() != null ? a.getStatus() : "pending");

        setFieldRow(binding.tvWhatValue, a.getWhat());
        setFieldRow(binding.tvWhenValue, a.getWhen());
        setFieldRow(binding.tvWhereValue, a.getWhere());
        setFieldRow(binding.tvAdditionalValue, a.getAdditionalInfo());

        if (a.getImageUrl() != null && !a.getImageUrl().isEmpty()) {
            binding.ivCover.setVisibility(View.VISIBLE);
            Glide.with(this).load(a.getImageUrl()).into(binding.ivCover);
            binding.ivCover.setOnClickListener(v -> FullScreenImageActivity.start(this, a.getImageUrl()));
        } else {
            binding.ivCover.setVisibility(View.GONE);
        }

        if (a.getApprovedBy() != null) {
            binding.tvApprovedBy.setText("Approved by " + a.getApprovedBy());
            binding.tvApprovedBy.setVisibility(View.VISIBLE);
        } else {
            binding.tvApprovedBy.setVisibility(View.GONE);
        }

        boolean canManage = session.isAdmin()
                || (session.getUserId() != null && session.getUserId().equals(a.getAuthorId()));
        binding.layoutOwnerActions.setVisibility(canManage ? View.VISIBLE : View.GONE);
        if (canManage) {
            binding.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(this, CreateAnnouncementActivity.class);
                intent.putExtra(Constants.EXTRA_ANNOUNCEMENT_ID, a.getId());
                intent.putExtra(Constants.EXTRA_EDIT_MODE, true);
                startActivity(intent);
                finish();
            });
            binding.btnDelete.setOnClickListener(v -> confirmDelete(a));
        }
    }

    private void confirmDelete(Announcement a) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_confirm_delete)
                .setMessage(R.string.dialog_confirm_delete_msg)
                .setPositiveButton(R.string.btn_delete, (d, w) ->
                        viewModel.deleteAnnouncement(a.getId(), session.getUserId(), session.getUserName()))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void setFieldRow(android.widget.TextView tv, String value) {
        if (tv == null) return;
        if (value != null && !value.isEmpty()) {
            tv.setText(value);
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.GONE);
            View label = (View) tv.getParent();
            if (label != null) label.setVisibility(View.GONE);
        }
    }
}
