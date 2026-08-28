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
import com.campus.campusmate.activities.EventDetailActivity;
import com.campus.campusmate.adapters.EventAdapter;
import com.campus.campusmate.databinding.FragmentAdminContentBinding;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.viewmodels.EventViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class AdminEventsFragment extends Fragment {

    private FragmentAdminContentBinding binding;
    private EventViewModel viewModel;
    private EventAdapter adapter;
    private SessionManager session;
    private List<Event> allItems = new ArrayList<>();

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

        viewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
        session = new SessionManager(requireContext());

        setupRecyclerView();
        setupSearch();
        setupObservers();

        viewModel.loadAllEvents();
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(requireContext());
        adapter.setListener(new EventAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Event e) {
                Intent intent = new Intent(requireContext(), EventDetailActivity.class);
                intent.putExtra(Constants.EXTRA_EVENT_ID, e.getId());
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(Event e, View anchor) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Admin Actions")
                        .setItems(new String[]{"Delete Event"}, (dialog, which) -> {
                            if (which == 0) confirmDelete(e);
                        })
                        .show();
            }
        });
        binding.rvContent.setAdapter(adapter);
    }

    private void confirmDelete(Event e) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_confirm_delete)
                .setMessage(R.string.dialog_confirm_delete_msg)
                .setPositiveButton(R.string.btn_delete, (d, w) -> {
                    viewModel.deleteEvent(e.getId(), session.getUserId(), session.getUserName());
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
        viewModel.events.observe(getViewLifecycleOwner(), list -> {
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

        List<Event> filtered = new ArrayList<>();
        String q = query.toLowerCase().trim();
        for (Event e : allItems) {
            if ((e.getTitle() != null && e.getTitle().toLowerCase().contains(q))
                    || (e.getAuthorName() != null && e.getAuthorName().toLowerCase().contains(q))) {
                filtered.add(e);
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
