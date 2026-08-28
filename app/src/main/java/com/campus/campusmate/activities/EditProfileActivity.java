package com.campus.campusmate.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.lifecycle.ViewModelProvider;

import com.campus.campusmate.R;
import com.campus.campusmate.base.BaseActivity;
import com.campus.campusmate.databinding.ActivityEditProfileBinding;
import com.campus.campusmate.models.User;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.viewmodels.ProfileViewModel;

public class EditProfileActivity extends BaseActivity<ActivityEditProfileBinding> {

    private ProfileViewModel viewModel;
    private User currentUser;

    @Override
    protected ActivityEditProfileBinding inflateBinding(LayoutInflater inflater) {
        return ActivityEditProfileBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        
        setupDropdowns();
        setupListeners();
        
        String uid = session.getUserId();
        if (uid != null) {
            viewModel.loadProfile(uid);
        }
    }

    private void setupDropdowns() {
        String[] depts = getResources().getStringArray(R.array.departments);
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, depts);
        binding.acDepartment.setAdapter(deptAdapter);

        String[] years = getResources().getStringArray(R.array.year_levels);
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, years);
        binding.acYear.setAdapter(yearAdapter);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> attemptSave());
    }

    @Override
    protected void setupObservers() {
        viewModel.userProfile.observe(this, user -> {
            if (user != null) {
                this.currentUser = user;
                prefillForm(user);
            } else {
                UiUtils.showSnackbar(binding.getRoot(), "Could not find your profile in the database.");
            }
        });

        viewModel.isLoading.observe(this, loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnSave.setEnabled(!loading);
        });

        viewModel.successMessage.observe(this, msg -> {
            if (msg != null) {
                UiUtils.showToast(this, msg);
                finish();
            }
        });

        viewModel.errorMessage.observe(this, error -> {
            if (error != null) {
                if ("user_not_found".equals(error)) {
                    handleStaleSession();
                } else {
                    UiUtils.showSnackbar(binding.getRoot(), error);
                }
            }
        });
    }

    private void handleStaleSession() {
        UiUtils.showToast(this, "Session expired or profile deleted. Please register again.");
        session.clearSession();
        android.content.Intent intent = new android.content.Intent(this, WelcomeActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void prefillForm(User user) {
        if (user == null) return;
        
        binding.etFirstName.setText(nonNull(user.getFirstName()));
        binding.etMiddleName.setText(nonNull(user.getMiddleName()));
        binding.etLastName.setText(nonNull(user.getLastName()));
        binding.acDepartment.setText(nonNull(user.getDepartment()), false);

        if (user.isStudent()) {
            binding.layoutStudentFields.setVisibility(View.VISIBLE);
            binding.acYear.setText(nonNull(user.getYearLevel()), false);
            binding.etBlock.setText(nonNull(user.getBlock()));
        } else if (user.isFaculty()) {
            binding.layoutFacultyFields.setVisibility(View.VISIBLE);
            binding.etProfession.setText(nonNull(user.getProfession()));
        } else if (user.isOrganization()) {
            binding.layoutOrganizationFields.setVisibility(View.VISIBLE);
            binding.etOrgName.setText(nonNull(user.getOrganizationName()));
            binding.etOrgDesc.setText(nonNull(user.getOrganizationDescription()));
        }
    }

    private String nonNull(String s) {
        return s != null ? s : "";
    }

    private void attemptSave() {
        if (currentUser == null) {
            UiUtils.showToast(this, "Profile not loaded yet. Please wait.");
            return;
        }

        hideKeyboard();

        String firstName = binding.etFirstName.getText().toString().trim();
        String lastName = binding.etLastName.getText().toString().trim();
        String dept = binding.acDepartment.getText().toString();

        if (firstName.isEmpty()) {
            UiUtils.showToast(this, "First name is required");
            return;
        }
        if (lastName.isEmpty()) {
            UiUtils.showToast(this, "Last name is required");
            return;
        }
        if (dept.isEmpty()) {
            UiUtils.showToast(this, "Department is required");
            return;
        }

        currentUser.setFirstName(firstName);
        currentUser.setMiddleName(binding.etMiddleName.getText().toString().trim());
        currentUser.setLastName(lastName);
        currentUser.setDepartment(dept);

        if (currentUser.isStudent()) {
            String year = binding.acYear.getText().toString();
            if (year.isEmpty()) {
                UiUtils.showToast(this, "Year level is required");
                return;
            }
            currentUser.setYearLevel(year);
            currentUser.setBlock(binding.etBlock.getText().toString().trim());
        } else if (currentUser.isFaculty()) {
            String prof = binding.etProfession.getText().toString().trim();
            if (prof.isEmpty()) {
                UiUtils.showToast(this, "Profession is required");
                return;
            }
            currentUser.setProfession(prof);
        } else if (currentUser.isOrganization()) {
            String orgName = binding.etOrgName.getText().toString().trim();
            if (orgName.isEmpty()) {
                UiUtils.showToast(this, "Organization name is required");
                return;
            }
            currentUser.setOrganizationName(orgName);
            currentUser.setOrganizationDescription(binding.etOrgDesc.getText().toString().trim());
        }

        viewModel.updateProfile(currentUser);
    }
}
