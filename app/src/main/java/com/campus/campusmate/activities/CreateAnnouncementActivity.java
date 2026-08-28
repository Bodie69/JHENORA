package com.campus.campusmate.activities;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import com.campus.campusmate.R;
import com.campus.campusmate.base.BaseActivity;
import com.campus.campusmate.databinding.ActivityCreateAnnouncementBinding;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.InputSanitizer;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.utils.ValidationUtils;
import com.campus.campusmate.viewmodels.AnnouncementViewModel;
import com.bumptech.glide.Glide;

public class CreateAnnouncementActivity extends BaseActivity<ActivityCreateAnnouncementBinding> {

    private AnnouncementViewModel viewModel;
    private Uri selectedImageUri;
    private String editAnnouncementId;
    private boolean isEdit;
    private Announcement editingAnnouncement;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(uri).centerCrop().into(binding.ivPreview);
                    binding.ivPreview.setVisibility(View.VISIBLE);
                    binding.btnAddImage.setText(R.string.btn_change_image);
                }
            });

    @Override
    protected ActivityCreateAnnouncementBinding inflateBinding(LayoutInflater inflater) {
        return ActivityCreateAnnouncementBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AnnouncementViewModel.class);

        setupDropdowns();
        setupListeners();

        editAnnouncementId = getIntent().getStringExtra(Constants.EXTRA_ANNOUNCEMENT_ID);
        isEdit = getIntent().getBooleanExtra(Constants.EXTRA_EDIT_MODE, false) && editAnnouncementId != null;
        if (isEdit) {
            binding.tvScreenTitle.setText(R.string.title_edit_announcement);
            binding.btnSubmit.setText(R.string.btn_update);
            viewModel.loadAnnouncement(editAnnouncementId);
        }
    }

    private void prefillForm(Announcement a) {
        editingAnnouncement = a;
        binding.etTitle.setText(a.getTitle());
        binding.etWhat.setText(a.getWhat());
        binding.etWho.setText(a.getWho());
        binding.etWhen.setText(a.getWhen());
        binding.etWhere.setText(a.getWhere());
        binding.etWhy.setText(a.getWhy());
        binding.etAdditionalInfo.setText(a.getAdditionalInfo());

        if (Announcement.PRIORITY_URGENT.equals(a.getPriority())) {
            binding.acPriority.setText("Urgent", false);
        } else if (Announcement.PRIORITY_HIGH.equals(a.getPriority())) {
            binding.acPriority.setText("High", false);
        } else {
            binding.acPriority.setText("Normal", false);
        }

        boolean isDept = Announcement.VISIBILITY_DEPARTMENT.equals(a.getVisibility());
        binding.acVisibility.setText(isDept ? "Department Only" : "General", false);
        binding.layoutDepartment.setVisibility(isDept ? View.VISIBLE : View.GONE);
        if (isDept && a.getDepartment() != null) {
            binding.acDepartment.setText(a.getDepartment(), false);
        }

        if (a.getImageUrl() != null && !a.getImageUrl().isEmpty()) {
            Glide.with(this).load(a.getImageUrl()).centerCrop().into(binding.ivPreview);
            binding.ivPreview.setVisibility(View.VISIBLE);
            binding.btnAddImage.setText(R.string.btn_change_image);
        }
    }

    private void setupDropdowns() {
        String[] priorities = getResources().getStringArray(R.array.priorities);
        binding.acPriority.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, priorities));
        binding.acPriority.setText(priorities[0], false);

        String[] visibilities = getResources().getStringArray(R.array.visibilities);
        binding.acVisibility.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, visibilities));
        binding.acVisibility.setText(visibilities[0], false);

        String[] depts = getResources().getStringArray(R.array.departments);
        binding.acDepartment.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, depts));

        binding.acVisibility.setOnItemClickListener((parent, view, position, id) -> {
            boolean isDept = position == 1;
            binding.layoutDepartment.setVisibility(isDept ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    protected void setupObservers() {
        viewModel.isLoading.observe(this, loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnSubmit.setEnabled(!loading);
        });
        viewModel.successMessage.observe(this, msg -> {
            if (msg != null) {
                UiUtils.showToast(this, msg);
                finish();
            }
        });
        viewModel.errorMessage.observe(this, msg -> {
            if (msg != null) UiUtils.showSnackbar(binding.getRoot(), msg);
        });
        viewModel.loadedAnnouncement.observe(this, a -> {
            if (a != null) prefillForm(a);
        });
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnAddImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.btnSubmit.setOnClickListener(v -> attemptSubmit());
    }

    private void attemptSubmit() {
        String title = InputSanitizer.sanitize(binding.etTitle.getText().toString());
        String what = InputSanitizer.sanitizeMultiline(binding.etWhat.getText().toString());

        if (!ValidationUtils.isNotEmpty(title)) {
            binding.tilTitle.setError(getString(R.string.error_field_required));
            return;
        }
        if (!ValidationUtils.isNotEmpty(what)) {
            binding.tilWhat.setError(getString(R.string.error_field_required));
            return;
        }

        Announcement a = editingAnnouncement != null ? editingAnnouncement : new Announcement(title,
                session.getUserId(), session.getUserName(), session.getUserRole());
        a.setTitle(title);
        a.setWhat(what);
        a.setWho(InputSanitizer.sanitize(binding.etWho.getText().toString()));
        a.setWhen(InputSanitizer.sanitize(binding.etWhen.getText().toString()));
        a.setWhere(InputSanitizer.sanitize(binding.etWhere.getText().toString()));
        a.setWhy(InputSanitizer.sanitize(binding.etWhy.getText().toString()));
        a.setAdditionalInfo(InputSanitizer.sanitizeMultiline(binding.etAdditionalInfo.getText().toString()));
        a.setDepartment(session.getUserDepartment());

        String priorityText = binding.acPriority.getText().toString();
        a.setPriority(priorityFromText(priorityText));

        String visText = binding.acVisibility.getText().toString();
        boolean isDept = visText.contains("Department");
        a.setVisibility(isDept ? Announcement.VISIBILITY_DEPARTMENT : Announcement.VISIBILITY_GENERAL);
        if (isDept) {
            String dept = binding.acDepartment.getText().toString();
            a.setDepartment(dept.isEmpty() ? session.getUserDepartment() : dept);
        }

        viewModel.submitWithImage(a, selectedImageUri, editingAnnouncement != null);
    }

    private String priorityFromText(String text) {
        if (text.equalsIgnoreCase("Urgent")) return Announcement.PRIORITY_URGENT;
        if (text.equalsIgnoreCase("High")) return Announcement.PRIORITY_HIGH;
        return Announcement.PRIORITY_NORMAL;
    }
}
