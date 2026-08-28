package com.campus.campusmate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.campus.campusmate.R;
import com.campus.campusmate.databinding.ActivityEventDetailBinding;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.repositories.EventRepository;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.DateUtils;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.utils.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class EventDetailActivity extends AppCompatActivity {

    private ActivityEventDetailBinding binding;
    private String eventId;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEventDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        eventId = getIntent().getStringExtra(Constants.EXTRA_EVENT_ID);
        if (eventId == null) { finish(); return; }

        EventRepository.getInstance().init(this);
        session = new SessionManager(this);

        binding.btnBack.setOnClickListener(v -> finish());
        loadEvent();
    }

    private void loadEvent() {
        binding.progressBar.setVisibility(View.VISIBLE);
        EventRepository.getInstance().fetchEventById(eventId,
                new EventRepository.OnEventFetchedListener() {
                    @Override
                    public void onFetched(Event e) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (e != null) bindData(e);
                    }

                    @Override
                    public void onError(String message) {
                        binding.progressBar.setVisibility(View.GONE);
                        UiUtils.showToast(EventDetailActivity.this, message);
                    }
                });
    }

    private void bindData(Event e) {
        binding.tvTitle.setText(e.getTitle());
        binding.tvAuthor.setText(e.getAuthorName() != null ? e.getAuthorName() : "");
        binding.tvDate.setText(DateUtils.formatDateTime(e.getCreatedAt()));

        String dept = Event.VISIBILITY_GENERAL.equals(e.getVisibility())
                ? getString(R.string.label_general) : e.getDepartment();
        binding.tvDept.setText(dept != null ? dept : "");

        if (e.getStartDate() > 0) {
            binding.tvEventDate.setText(DateUtils.formatDateTime(e.getStartDate()));
            binding.tvEventDate.setVisibility(View.VISIBLE);
        }

        setField(binding.tvWhatValue, e.getWhat());
        setField(binding.tvWhoValue, e.getWho());
        setField(binding.tvWhenValue, e.getWhen());
        setField(binding.tvWhereValue, e.getWhere());
        setField(binding.tvWhyValue, e.getWhy());

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
                .setPositiveButton(R.string.btn_delete, (d, w) -> {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    EventRepository.getInstance().deleteEvent(e.getId(),
                            session.getUserId(), session.getUserName(),
                            new EventRepository.OnSimpleListener() {
                                @Override
                                public void onSuccess() {
                                    UiUtils.showToast(EventDetailActivity.this,
                                            getString(R.string.success_post_deleted));
                                    finish();
                                }

                                @Override
                                public void onError(String message) {
                                    binding.progressBar.setVisibility(View.GONE);
                                    UiUtils.showToast(EventDetailActivity.this, message);
                                }
                            });
                })
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
        }
    }
}
