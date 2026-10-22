package com.campus.campusmate.activities;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.campus.campusmate.R;
import com.campus.campusmate.base.BaseActivity;
import com.campus.campusmate.databinding.ActivityCreateEventBinding;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.DateUtils;
import com.campus.campusmate.utils.InputSanitizer;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.utils.ValidationUtils;
import com.campus.campusmate.viewmodels.EventViewModel;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Calendar;
import java.util.TimeZone;

public class CreateEventActivity extends BaseActivity<ActivityCreateEventBinding> {

    private EventViewModel viewModel;
    private Uri selectedImageUri;
    private long selectedStartDate = 0;
    private long selectedEndDate = 0;
    private String editEventId;
    private Event editingEvent;

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
    protected ActivityCreateEventBinding inflateBinding(LayoutInflater inflater) {
        return ActivityCreateEventBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(EventViewModel.class);

        setupDropdowns();
        setupListeners();

        editEventId = getIntent().getStringExtra(Constants.EXTRA_EVENT_ID);
        boolean isEdit = getIntent().getBooleanExtra(Constants.EXTRA_EDIT_MODE, false)
                && editEventId != null;
        if (isEdit) {
            binding.tvScreenTitle.setText(R.string.title_edit_event);
            binding.btnSubmit.setText(R.string.btn_update);
            viewModel.loadEvent(editEventId);
        }
    }

    private void prefillForm(Event e) {
        editingEvent = e;
        binding.etTitle.setText(e.getTitle());
        binding.etWhat.setText(e.getWhat());
        binding.etWhere.setText(e.getWhere());
        binding.etRegistrationLink.setText(e.getRegistrationLink());

        if (e.getStartDate() > 0) {
            selectedStartDate = e.getStartDate();
            binding.etStartDate.setText(DateUtils.formatDateTime(e.getStartDate()));
        }
        if (e.getEndDate() > 0) {
            selectedEndDate = e.getEndDate();
            binding.etEndDate.setText(DateUtils.formatDateTime(e.getEndDate()));
        }

        boolean isDept = Event.VISIBILITY_DEPARTMENT.equals(e.getVisibility());
        binding.acVisibility.setText(isDept ? "Department Only" : "General", false);
        binding.layoutDepartment.setVisibility(isDept ? View.VISIBLE : View.GONE);
        if (isDept && e.getDepartment() != null) {
            binding.acDepartment.setText(e.getDepartment(), false);
        }

        if (e.getImageUrl() != null && !e.getImageUrl().isEmpty()) {
            Glide.with(this).load(e.getImageUrl()).centerCrop().into(binding.ivPreview);
            binding.ivPreview.setVisibility(View.VISIBLE);
            binding.btnAddImage.setText(R.string.btn_change_image);
        }
    }

    private void setupDropdowns() {
        String[] visibilities = getResources().getStringArray(R.array.visibilities);
        binding.acVisibility.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, visibilities));
        binding.acVisibility.setText(visibilities[0], false);

        String[] depts = getResources().getStringArray(R.array.departments);
        binding.acDepartment.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, depts));

        binding.acVisibility.setOnItemClickListener((parent, view, pos, id) -> {
            binding.layoutDepartment.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    protected void setupObservers() {
        viewModel.isLoading.observe(this, loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnSubmit.setEnabled(!loading);
        });
        viewModel.successMessage.observe(this, msgEvent -> {
            String msg = msgEvent != null ? msgEvent.getContentIfNotHandled() : null;
            if (msg != null) { UiUtils.showToast(this, msg); finish(); }
        });
        viewModel.errorMessage.observe(this, msgEvent -> {
            String msg = msgEvent != null ? msgEvent.getContentIfNotHandled() : null;
            if (msg != null) UiUtils.showSnackbar(binding.getRoot(), msg);
        });
        viewModel.loadedEvent.observe(this, e -> {
            if (e != null) prefillForm(e);
        });
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnAddImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.btnSubmit.setOnClickListener(v -> attemptSubmit());

        binding.etStartDate.setOnClickListener(v -> showDatePicker(true));
        binding.etEndDate.setOnClickListener(v -> showDatePicker(false));
        binding.tilStartDate.setEndIconOnClickListener(v -> showDatePicker(true));
        binding.tilEndDate.setEndIconOnClickListener(v -> showDatePicker(false));
    }

    /** Date step, then a time step for that date — combined into one timestamp so Start/End
     *  carry a real time of day instead of always landing on midnight. */
    private void showDatePicker(boolean isStart) {
        long existing = isStart ? selectedStartDate : selectedEndDate;
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isStart ? getString(R.string.hint_start_date) : getString(R.string.hint_end_date))
                .setSelection(existing > 0 ? existing : MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        picker.addOnPositiveButtonClickListener(selectionUtc -> {
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(selectionUtc);

            Calendar prior = Calendar.getInstance();
            if (existing > 0) prior.setTimeInMillis(existing);
            int hour = existing > 0 ? prior.get(Calendar.HOUR_OF_DAY) : 9;
            int minute = existing > 0 ? prior.get(Calendar.MINUTE) : 0;

            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(hour)
                    .setMinute(minute)
                    .setTitleText(isStart ? getString(R.string.hint_start_date) : getString(R.string.hint_end_date))
                    .build();
            timePicker.addOnPositiveButtonClickListener(v -> {
                Calendar local = Calendar.getInstance();
                local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH),
                        timePicker.getHour(), timePicker.getMinute(), 0);
                local.set(Calendar.MILLISECOND, 0);
                long timestamp = local.getTimeInMillis();
                if (isStart) {
                    selectedStartDate = timestamp;
                    binding.etStartDate.setText(DateUtils.formatDateTime(timestamp));
                } else {
                    selectedEndDate = timestamp;
                    binding.etEndDate.setText(DateUtils.formatDateTime(timestamp));
                }
            });
            timePicker.show(getSupportFragmentManager(), isStart ? "start_time_picker" : "end_time_picker");
        });
        picker.show(getSupportFragmentManager(), isStart ? "start_date_picker" : "end_date_picker");
    }

    private void attemptSubmit() {
        String title = InputSanitizer.sanitize(binding.etTitle.getText().toString());
        String what = InputSanitizer.sanitizeMultiline(binding.etWhat.getText().toString());

        binding.tilTitle.setError(null);
        binding.tilWhat.setError(null);

        if (!ValidationUtils.isNotEmpty(title)) {
            binding.tilTitle.setError(getString(R.string.error_field_required));
            return;
        }
        if (!ValidationUtils.isNotEmpty(what)) {
            binding.tilWhat.setError(getString(R.string.error_field_required));
            return;
        }

        Event e = editingEvent != null ? editingEvent
                : new Event(title, session.getUserId(), session.getUserName(), session.getUserRole());
        e.setTitle(title);
        e.setWhat(what);
        e.setWhere(InputSanitizer.sanitize(binding.etWhere.getText().toString()));
        e.setRegistrationLink(InputSanitizer.sanitizeUrl(binding.etRegistrationLink.getText().toString()));
        e.setStartDate(selectedStartDate);
        e.setEndDate(selectedEndDate);
        e.setDepartment(session.getUserDepartment());

        String visText = binding.acVisibility.getText().toString();
        boolean isDept = visText.contains("Department");
        e.setVisibility(isDept ? Event.VISIBILITY_DEPARTMENT : Event.VISIBILITY_GENERAL);
        if (isDept) {
            String dept = binding.acDepartment.getText().toString();
            e.setDepartment(dept.isEmpty() ? session.getUserDepartment() : dept);
        }

        viewModel.submitWithImage(e, selectedImageUri, editingEvent != null);
    }
}
