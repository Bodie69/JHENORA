package com.campus.campusmate.activities;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.campus.campusmate.R;
import com.campus.campusmate.databinding.ActivityRegisterBinding;
import com.campus.campusmate.models.User;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.utils.ValidationUtils;
import com.campus.campusmate.viewmodels.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel viewModel;
    private String selectedRole = User.ROLE_STUDENT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupDropdowns();
        setupRoleToggle();
        setupObservers();
        setupListeners();
        UiUtils.setupPasswordToggle(binding.btnTogglePassword, binding.etPassword);
        UiUtils.setupPasswordToggle(binding.btnToggleConfirmPassword, binding.etConfirmPassword);
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

    private void setupRoleToggle() {
        binding.btnStudent.setOnClickListener(v -> switchRole(User.ROLE_STUDENT));
        binding.btnFaculty.setOnClickListener(v -> switchRole(User.ROLE_FACULTY));
        switchRole(User.ROLE_STUDENT);
    }

    private void switchRole(String role) {
        selectedRole = role;
        boolean isStudent = User.ROLE_STUDENT.equals(role);
        
        binding.layoutStudentExtras.setVisibility(isStudent ? View.VISIBLE : View.GONE);
        binding.layoutFacultyExtras.setVisibility(isStudent ? View.GONE : View.VISIBLE);

        binding.btnStudent.setBackgroundColor(isStudent ? getColor(R.color.surface) : Color.TRANSPARENT);
        binding.btnStudent.setTextColor(isStudent ? getColor(R.color.text_primary) : getColor(R.color.text_secondary));

        binding.btnFaculty.setBackgroundColor(isStudent ? Color.TRANSPARENT : getColor(R.color.surface));
        binding.btnFaculty.setTextColor(isStudent ? getColor(R.color.text_secondary) : getColor(R.color.text_primary));
    }

    private void setupObservers() {
        viewModel.isLoading.observe(this, loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnRegister.setEnabled(!loading);
        });

        viewModel.registerSuccess.observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                UiUtils.showLongToast(this, getString(R.string.success_registration));
                finish();
            }
        });

        viewModel.errorMessage.observe(this, error -> {
            if (error != null) UiUtils.showSnackbar(binding.getRoot(), error);
        });
    }

    private void setupListeners() {
        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.tvLogin.setOnClickListener(v -> finish());
        binding.btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void attemptRegister() {
        String firstName = binding.etFirstName.getText().toString().trim();
        String middleName = binding.etMiddleName.getText().toString().trim();
        String lastName = binding.etLastName.getText().toString().trim();
        String dept = binding.acDepartment.getText().toString();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String confirmPassword = binding.etConfirmPassword.getText().toString();

        if (firstName.isEmpty()) {
            UiUtils.showToast(this, "First name required");
            return;
        }
        if (lastName.isEmpty()) {
            UiUtils.showToast(this, "Last name required");
            return;
        }
        if (dept.isEmpty()) {
            UiUtils.showToast(this, "Department required");
            return;
        }

        User user = new User();
        user.setFirstName(firstName);
        user.setMiddleName(middleName);
        user.setLastName(lastName);
        user.setDepartment(dept);
        user.setRole(selectedRole);

        if (User.ROLE_STUDENT.equals(selectedRole)) {
            String year = binding.acYear.getText().toString();
            String block = binding.etBlock.getText().toString().trim();
            if (year.isEmpty()) {
                UiUtils.showToast(this, "Year required");
                return;
            }
            user.setYearLevel(year);
            user.setBlock(block);
        } else {
            String prof = binding.etProfession.getText().toString().trim();
            if (prof.isEmpty()) {
                UiUtils.showToast(this, "Profession required");
                return;
            }
            user.setProfession(prof);
        }

        if (email.isEmpty()) {
            UiUtils.showToast(this, "Email required");
            return;
        }
        if (password.length() < 8) {
            UiUtils.showToast(this, "Password too short");
            return;
        }
        if (!password.equals(confirmPassword)) {
            UiUtils.showToast(this, "Passwords mismatch");
            return;
        }

        if (User.ROLE_STUDENT.equals(selectedRole)) {
            viewModel.registerStudent(email, password, user);
        } else {
            viewModel.registerFaculty(email, password, user);
        }
    }
}
