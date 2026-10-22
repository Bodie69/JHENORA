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
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.viewmodels.AdminViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class AdminUsersFragment extends Fragment {

    private FragmentAdminUsersBinding binding;
    private AdminViewModel viewModel;
    private UserAdapter adapter;
    private SessionManager session;
    private static final String STATE_ROLE = "role_filter";
    private static final String STATE_DEPT = "dept_filter";
    /** Role to show, or empty for every role. */
    private String currentFilter = "";
    /** Department to show, or empty for every department. */
    private String currentDept = "";
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
        session = new SessionManager(requireContext());

        if (savedInstanceState != null) {
            currentFilter = savedInstanceState.getString(STATE_ROLE, "");
            currentDept = savedInstanceState.getString(STATE_DEPT, "");
        }

        setupRecyclerView();
        setupFilters();
        setupSearch();
        setupObservers();

        viewModel.loadAllUsers();
    }

    @Override
    public void onResume() {
        super.onResume();
        // This tab's ViewPager2 page is kept alive (not recreated) while another tab is
        // shown, so without this, new registrations/verifications elsewhere would sit stale
        // here until the app is force-restarted.
        if (viewModel != null) viewModel.loadAllUsers();
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter(requireContext());
        adapter.setListener(this::showUserActions);
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
        binding.chipAdmins.setOnClickListener(v -> {
            currentFilter = User.ROLE_ADMIN;
            updateFilterUI();
            refreshList();
        });
        binding.chipDisplay.setOnClickListener(v -> {
            currentFilter = User.ROLE_LIVE_DISPLAY;
            updateFilterUI();
            refreshList();
        });
        binding.chipDeptAll.setOnClickListener(v -> setDeptFilter(""));
        binding.chipDeptBsit.setOnClickListener(v -> setDeptFilter(Constants.DEPT_BSIT));
        binding.chipDeptBtled.setOnClickListener(v -> setDeptFilter(Constants.DEPT_BTLED));
        binding.chipDeptEduc.setOnClickListener(v -> setDeptFilter(Constants.DEPT_EDUC));

        // Initialize UI state
        updateFilterUI();
    }

    private void updateFilterUI() {
        setChipSelected(binding.chipAll, currentFilter.isEmpty());
        setChipSelected(binding.chipStudents, User.ROLE_STUDENT.equals(currentFilter));
        setChipSelected(binding.chipFaculty, User.ROLE_FACULTY.equals(currentFilter));
        setChipSelected(binding.chipOrgs, User.ROLE_ORGANIZATION.equals(currentFilter));
        setChipSelected(binding.chipAdmins, User.ROLE_ADMIN.equals(currentFilter));
        setChipSelected(binding.chipDisplay, User.ROLE_LIVE_DISPLAY.equals(currentFilter));
        setChipSelected(binding.chipDeptAll, currentDept.isEmpty());
        setChipSelected(binding.chipDeptBsit, Constants.DEPT_BSIT.equals(currentDept));
        setChipSelected(binding.chipDeptBtled, Constants.DEPT_BTLED.equals(currentDept));
        setChipSelected(binding.chipDeptEduc, Constants.DEPT_EDUC.equals(currentDept));
    }

    private void setDeptFilter(String dept) {
        currentDept = dept;
        updateFilterUI();
        refreshList();
    }

    private void setChipSelected(android.widget.TextView chip, boolean isSelected) {
        // Lets TalkBack announce the active filter; the background swap below is visual only.
        chip.setSelected(isSelected);
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
    }

    /** Shows the user's details; non-admin accounts other than your own get a Delete action. */
    private void showUserActions(User user) {
        String name = displayName(user);
        StringBuilder details = new StringBuilder();
        if (user.getEmail() != null) details.append(user.getEmail());
        if (user.getRole() != null) details.append('\n').append(getString(R.string.user_detail_role, user.getRole()));
        if (user.getDepartment() != null && !user.getDepartment().isEmpty()) {
            details.append('\n').append(getString(R.string.user_detail_department, user.getDepartment()));
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(name)
                .setMessage(details.toString())
                .setNegativeButton(R.string.btn_close, null);
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            builder.setView(buildPhotoView(user, name));
        }

        boolean isSelf = user.getUid() != null && user.getUid().equals(session.getUserId());
        if (!user.isAdmin() && !isSelf) {
            builder.setPositiveButton(R.string.user_delete, (d, w) -> confirmDelete(user));
        }
        builder.show();
    }

    /** Large circular profile photo for the user detail dialog. */
    private View buildPhotoView(User user, String name) {
        int size = Math.round(96 * getResources().getDisplayMetrics().density);
        com.google.android.material.imageview.ShapeableImageView photo =
                new com.google.android.material.imageview.ShapeableImageView(requireContext());
        photo.setShapeAppearanceModel(photo.getShapeAppearanceModel().toBuilder()
                .setAllCornerSizes(new com.google.android.material.shape.RelativeCornerSize(0.5f))
                .build());
        photo.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        photo.setContentDescription(getString(R.string.profile_photo_of, name));
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(size, size);
        params.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        params.topMargin = Math.round(8 * getResources().getDisplayMetrics().density);
        photo.setLayoutParams(params);
        com.bumptech.glide.Glide.with(this).load(user.getProfileImageUrl()).centerCrop().into(photo);

        android.widget.FrameLayout container = new android.widget.FrameLayout(requireContext());
        container.addView(photo);
        return container;
    }

    private void confirmDelete(User user) {
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.user_delete_title, displayName(user)))
                .setMessage(R.string.user_delete_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.user_delete, (d, w) -> viewModel.deleteUser(user))
                .show();
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                .setTextColor(requireContext().getColor(R.color.danger));
    }

    private static String displayName(User user) {
        String name = user.getFullName();
        return name.isEmpty() ? (user.getEmail() != null ? user.getEmail() : "User") : name;
    }

    private void refreshList() {
        refreshList(viewModel.allUsers.getValue());
    }

    private void refreshList(List<User> all) {
        List<User> filtered = viewModel.filterUsers(all, currentFilter, currentDept, currentQuery);
        adapter.submitList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ROLE, currentFilter);
        outState.putString(STATE_DEPT, currentDept);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
