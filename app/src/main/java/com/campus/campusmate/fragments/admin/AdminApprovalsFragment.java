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
import com.campus.campusmate.adapters.ApprovalAdapter;
import com.campus.campusmate.databinding.FragmentAdminApprovalsBinding;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.models.User;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.viewmodels.AdminViewModel;
import com.campus.campusmate.viewmodels.AnnouncementViewModel;
import com.campus.campusmate.viewmodels.EventViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AdminApprovalsFragment extends Fragment {

    private FragmentAdminApprovalsBinding binding;
    private AnnouncementViewModel announcementViewModel;
    private EventViewModel eventViewModel;
    private AdminViewModel adminViewModel;
    private ApprovalAdapter adapter;
    private SessionManager session;
    private List<Announcement> pendingAnnouncements = new ArrayList<>();
    private List<Event> pendingEvents = new ArrayList<>();
    private List<User> pendingUsers = new ArrayList<>();
    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminApprovalsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        session = new SessionManager(requireContext());
        announcementViewModel = new ViewModelProvider(requireActivity()).get(AnnouncementViewModel.class);
        eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
        adminViewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        setupRecyclerView();
        setupSearch();
        setupObservers();

        loadData();
    }

    private void loadData() {
        announcementViewModel.loadPendingAnnouncements();
        eventViewModel.loadPendingEvents();
        adminViewModel.loadPendingUsers();
    }

    @Override
    public void onResume() {
        super.onResume();
        // This tab's ViewPager2 page is kept alive (not recreated) while another tab is
        // shown, so without this, pending items submitted or actioned elsewhere (another
        // admin, another device) would sit stale here until the app is force-restarted.
        if (announcementViewModel != null && eventViewModel != null && adminViewModel != null) {
            loadData();
        }
    }

    private void setupRecyclerView() {
        adapter = new ApprovalAdapter(requireContext());
        adapter.setListener(new ApprovalAdapter.OnApprovalListener() {
            @Override
            public void onApprove(Object item) {
                confirmApprove(item);
            }

            @Override
            public void onReject(Object item) {
                showRejectDialog(item);
            }

            @Override
            public void onView(Object item) {
                Intent intent;
                if (item instanceof Event) {
                    intent = new Intent(requireContext(),
                            com.campus.campusmate.activities.EventDetailActivity.class);
                    intent.putExtra(Constants.EXTRA_EVENT_ID, ((Event) item).getId());
                } else {
                    intent = new Intent(requireContext(),
                            com.campus.campusmate.activities.AnnouncementDetailActivity.class);
                    intent.putExtra(Constants.EXTRA_ANNOUNCEMENT_ID, ((Announcement) item).getId());
                }
                startActivity(intent);
            }
        });
        binding.rvApprovals.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().toLowerCase().trim();
                submitMergedList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                UiUtils.hideKeyboard(v);
                submitMergedList();
                return true;
            }
            return false;
        });
    }

    private void setupObservers() {
        announcementViewModel.pendingAnnouncements.observe(getViewLifecycleOwner(), list -> {
            pendingAnnouncements = list != null ? list : new ArrayList<>();
            submitMergedList();
        });

        eventViewModel.pendingEvents.observe(getViewLifecycleOwner(), list -> {
            pendingEvents = list != null ? list : new ArrayList<>();
            submitMergedList();
        });

        adminViewModel.pendingUsers.observe(getViewLifecycleOwner(), list -> {
            pendingUsers = list != null ? list : new ArrayList<>();
            submitMergedList();
        });

        announcementViewModel.successMessage.observe(getViewLifecycleOwner(), msgEvent -> {
            String msg = msgEvent != null ? msgEvent.getContentIfNotHandled() : null;
            if (msg == null) return;
            UiUtils.showToast(requireContext(), msg);
            // The item was just approved/rejected — reload so it drops off this list
            // immediately instead of sitting here stale until the tab is revisited.
            announcementViewModel.loadPendingAnnouncements();
        });

        eventViewModel.successMessage.observe(getViewLifecycleOwner(), msgEvent -> {
            String msg = msgEvent != null ? msgEvent.getContentIfNotHandled() : null;
            if (msg == null) return;
            UiUtils.showToast(requireContext(), msg);
            eventViewModel.loadPendingEvents();
        });

        // The dashboard shows (and consumes) the message itself; here we only refresh the
        // pending list after any admin action succeeds, so don't consume the event.
        adminViewModel.successMessage.observe(getViewLifecycleOwner(), event -> {
            if (event != null) adminViewModel.loadPendingUsers();
        });
    }

    private void submitMergedList() {
        List<Object> merged = new ArrayList<>();
        
        for (Announcement a : pendingAnnouncements) {
            if (matchesQuery(a)) merged.add(a);
        }
        for (Event e : pendingEvents) {
            if (matchesQuery(e)) merged.add(e);
        }
        for (User u : pendingUsers) {
            if (matchesQuery(u)) merged.add(u);
        }

        merged.sort((a, b) -> Long.compare(getCreatedAt(a), getCreatedAt(b)));
        adapter.submitList(merged);
        binding.tvEmpty.setVisibility(merged.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean matchesQuery(Object item) {
        if (currentQuery.isEmpty()) return true;

        String title = "";
        String author = "";

        if (item instanceof Announcement) {
            Announcement a = (Announcement) item;
            title = a.getTitle() != null ? a.getTitle().toLowerCase() : "";
            author = a.getAuthorName() != null ? a.getAuthorName().toLowerCase() : "";
        } else if (item instanceof Event) {
            Event e = (Event) item;
            title = e.getTitle() != null ? e.getTitle().toLowerCase() : "";
            author = e.getAuthorName() != null ? e.getAuthorName().toLowerCase() : "";
        } else if (item instanceof User) {
            User u = (User) item;
            title = u.getFullName() != null ? u.getFullName().toLowerCase() : "";
            author = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
        }

        return title.contains(currentQuery) || author.contains(currentQuery);
    }

    private long getCreatedAt(Object item) {
        if (item instanceof Event) return ((Event) item).getCreatedAt();
        if (item instanceof User) return ((User) item).getCreatedAt();
        return ((Announcement) item).getCreatedAt();
    }

    private void confirmApprove(Object item) {
        boolean isUser = item instanceof User;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(isUser ? R.string.dialog_confirm_approve_user : R.string.dialog_confirm_approve)
                .setMessage(isUser ? R.string.dialog_confirm_approve_user_msg : R.string.dialog_confirm_approve_msg)
                .setPositiveButton(R.string.btn_approve, (d, w) -> {
                    if (item instanceof Event) {
                        eventViewModel.approveEvent(
                                ((Event) item).getId(), session.getUserId(), session.getUserName());
                    } else if (isUser) {
                        adminViewModel.approveUser(((User) item).getUid());
                    } else {
                        announcementViewModel.approveAnnouncement(
                                ((Announcement) item).getId(), session.getUserId(), session.getUserName());
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showRejectDialog(Object item) {
        if (item instanceof User) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dialog_confirm_reject_user)
                    .setMessage(R.string.dialog_confirm_reject_user_msg)
                    .setPositiveButton(R.string.btn_reject, (d, w) ->
                            adminViewModel.rejectUser(((User) item).getUid()))
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_reject_reason, null);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_confirm_reject)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_reject, (d, w) -> {
                    TextInputEditText etReason = dialogView.findViewById(R.id.et_reason);
                    String reason = etReason != null && etReason.getText() != null
                            ? etReason.getText().toString().trim() : "";
                    if (item instanceof Event) {
                        eventViewModel.rejectEvent(((Event) item).getId(),
                                session.getUserId(), session.getUserName(), reason);
                    } else {
                        announcementViewModel.rejectAnnouncement(((Announcement) item).getId(),
                                session.getUserId(), session.getUserName(), reason);
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
