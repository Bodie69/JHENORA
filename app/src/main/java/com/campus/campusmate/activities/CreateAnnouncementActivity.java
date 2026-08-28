package com.campus.campusmate.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.campus.campusmate.R;
import com.campus.campusmate.databinding.ActivityCreateAnnouncementBinding;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.repositories.AnnouncementRepository;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.InputSanitizer;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.utils.SupabaseStorage;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.utils.ValidationUtils;
import com.campus.campusmate.viewmodels.AnnouncementViewModel;
import com.bumptech.glide.Glide;

public class CreateAnnouncementActivity extends AppCompatActivity {

    private ActivityCreateAnnouncementBinding binding;
    private AnnouncementViewModel viewModel;
    private SessionManager session;
    private Uri selectedImageUri;
    private String editAnnouncementId;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateAnnouncementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(AnnouncementViewModel.class);

        setupDropdowns();
        setupObservers();
        setupListeners();

        editAnnouncementId = getIntent().getStringExtra(Constants.EXTRA_ANNOUNCEMENT_ID);
        boolean isEdit = getIntent().getBooleanExtra(Constants.EXTRA_EDIT_MODE, false)
                && editAnnouncementId != null;
        if (isEdit) {
            loadForEdit();
        }
    }

    private void loadForEdit() {
        binding.tvScreenTitle.setText(R.string.title_edit_announcement);
        binding.btnSubmit.setText(R.string.btn_update);
        binding.progressBar.setVisibility(View.VISIBLE);
        AnnouncementRepository.getInstance().fetchAnnouncementById(editAnnouncementId,
                new AnnouncementRepository.OnAnnouncementFetchedListener() {
                    @Override
                    public void onFetched(Announcement a) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (a != null) {
                            editingAnnouncement = a;
                            prefillForm(a);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        binding.progressBar.setVisibility(View.GONE);
                        UiUtils.showToast(CreateAnnouncementActivity.this, message);
                    }
                });
    }

    private void prefillForm(Announcement a) {
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

    private void setupObservers() {
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

        if (selectedImageUri != null) {
            uploadImageThenSubmit(a);
        } else {
            submitAnnouncement(a);
        }
    }

    private void uploadImageThenSubmit(Announcement a) {
        binding.progressBar.setVisibility(View.VISIBLE);
        SupabaseStorage.uploadImage(this, selectedImageUri, "announcements", new SupabaseStorage.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                a.setImageUrl(publicUrl);
                submitAnnouncement(a);
            }

            @Override
            public void onFailure(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                UiUtils.showToast(CreateAnnouncementActivity.this, "Image upload failed, submitting without image");
                submitAnnouncement(a);
            }
        });
    }

    private void submitAnnouncement(Announcement a) {
        if (editingAnnouncement != null) {
            viewModel.updateAnnouncement(a);
        } else {
            viewModel.submitAnnouncement(a);
        }
    }

    private String priorityFromText(String text) {
        if (text.equalsIgnoreCase("Urgent")) return Announcement.PRIORITY_URGENT;
        if (text.equalsIgnoreCase("High")) return Announcement.PRIORITY_HIGH;
        return Announcement.PRIORITY_NORMAL;
    }
}
