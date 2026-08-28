package com.campus.campusmate.fragments.admin;

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
import com.campus.campusmate.adapters.UserAdapter;
import com.campus.campusmate.databinding.FragmentAdminUsersBinding;
import com.campus.campusmate.models.User;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.viewmodels.AdminViewModel;

import java.util.List;

public class AdminUsersFragment extends Fragment {

    private FragmentAdminUsersBinding binding;
    private AdminViewModel viewModel;
    private UserAdapter adapter;
    private String currentFilter = "";
    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        setupRecyclerView();
        setupFilters();
        setupSearch();
        setupObservers();
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter(requireContext());
        adapter.setListener(user -> {});
        binding.rvUsers.setAdapter(adapter);
    }

    private void setupFilters() {
        binding.chipAll.setOnClickListener(v -> {
            currentFilter = "";
            updateFilterUI();
            refreshList();
        });
        binding.chipStudents.setOnClickListener(v -> {
            currentFilter = User.ROLE_STUDENT;
            updateFilterUI();
            refreshList();
        });
        binding.chipFaculty.setOnClickListener(v -> {
            currentFilter = User.ROLE_FACULTY;
            updateFilterUI();
            refreshList();
        });
        binding.chipOrgs.setOnClickListener(v -> {
            currentFilter = User.ROLE_ORGANIZATION;
            updateFilterUI();
            refreshList();
        });

        // Initialize UI state
        updateFilterUI();
    }

    private void updateFilterUI() {
        setChipSelected(binding.chipAll, currentFilter.isEmpty());
        setChipSelected(binding.chipStudents, User.ROLE_STUDENT.equals(currentFilter));
        setChipSelected(binding.chipFaculty, User.ROLE_FACULTY.equals(currentFilter));
        setChipSelected(binding.chipOrgs, User.ROLE_ORGANIZATION.equals(currentFilter));
    }

    private void setChipSelected(android.widget.TextView chip, boolean isSelected) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setTextColor(requireContext().getColor(R.color.white));
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(requireContext().getColor(R.color.text_secondary));
            chip.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                refreshList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                UiUtils.hideKeyboard(v);
                refreshList();
                return true;
            }
            return false;
        });
    }

    private void setupObservers() {
        viewModel.allUsers.observe(getViewLifecycleOwner(), users -> refreshList(users));
        viewModel.successMessage.observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) UiUtils.showToast(requireContext(), msg);
        });
    }

    private void refreshList() {
        refreshList(viewModel.allUsers.getValue());
    }

    private void refreshList(List<User> all) {
        List<User> filtered = viewModel.filterUsers(all, currentFilter, currentQuery);
        adapter.submitList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
