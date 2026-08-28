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
import com.campus.campusmate.activities.AnnouncementDetailActivity;
import com.campus.campusmate.adapters.AnnouncementAdapter;
import com.campus.campusmate.databinding.FragmentAdminContentBinding;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.viewmodels.AnnouncementViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class AdminAnnouncementsFragment extends Fragment {

    private FragmentAdminContentBinding binding;
    private AnnouncementViewModel viewModel;
    private AnnouncementAdapter adapter;
    private SessionManager session;
    private List<Announcement> allItems = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminContentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AnnouncementViewModel.class);
        session = new SessionManager(requireContext());

        setupRecyclerView();
        setupSearch();
        setupObservers();

        viewModel.loadAllAnnouncements();
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
            public void onItemLongClick(Announcement a, View anchor) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Admin Actions")
                        .setItems(new String[]{"Delete Announcement"}, (dialog, which) -> {
                            if (which == 0) confirmDelete(a);
                        })
                        .show();
            }
        });
        binding.rvContent.setAdapter(adapter);
    }

    private void confirmDelete(Announcement a) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_confirm_delete)
                .setMessage(R.string.dialog_confirm_delete_msg)
                .setPositiveButton(R.string.btn_delete, (d, w) -> {
                    viewModel.deleteAnnouncement(a.getId(), session.getUserId(), session.getUserName());
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void setupSearch() {
        binding.ivSearch.setOnClickListener(v -> {
            binding.etSearch.requestFocus();
            UiUtils.hideKeyboard(binding.etSearch);
            filter(binding.etSearch.getText().toString());
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                UiUtils.hideKeyboard(v);
                filter(v.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void setupObservers() {
        viewModel.announcements.observe(getViewLifecycleOwner(), list -> {
            allItems = list != null ? list : new ArrayList<>();
            filter(binding.etSearch.getText().toString());
        });
    }

    private void filter(String query) {
        if (query.isEmpty()) {
            adapter.submitList(allItems);
            binding.tvEmpty.setVisibility(allItems.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        List<Announcement> filtered = new ArrayList<>();
        String q = query.toLowerCase().trim();
        for (Announcement a : allItems) {
            if ((a.getTitle() != null && a.getTitle().toLowerCase().contains(q))
                    || (a.getAuthorName() != null && a.getAuthorName().toLowerCase().contains(q))) {
                filtered.add(a);
            }
        }
        adapter.submitList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
