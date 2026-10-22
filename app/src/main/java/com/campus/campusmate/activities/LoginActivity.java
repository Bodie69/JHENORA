package com.campus.campusmate.activities;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.campus.campusmate.R;
import com.campus.campusmate.base.BaseActivity;
import com.campus.campusmate.databinding.ActivityLoginBinding;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.InputSanitizer;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.utils.ValidationUtils;
import com.campus.campusmate.viewmodels.AuthViewModel;

public class LoginActivity extends BaseActivity<ActivityLoginBinding> {

    private AuthViewModel viewModel;

    @Override
    protected ActivityLoginBinding inflateBinding(LayoutInflater inflater) {
        return ActivityLoginBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupPrefilledEmail();
        setupListeners();
        UiUtils.setupPasswordToggle(binding.btnTogglePassword, binding.etPassword);
    }

    private void setupPrefilledEmail() {
        if (viewModel.getSession().isRememberMe()) {
            String email = viewModel.getSession().getSavedEmail();
            if (email != null && !email.isEmpty()) {
                binding.etEmail.setText(email);
                binding.cbRememberMe.setChecked(true);
            }
        }
    }

    @Override
    protected void setupObservers() {
        viewModel.isLoading.observe(this, loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnLogin.setEnabled(!loading);
        });

        viewModel.loginSuccess.observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                String role = viewModel.getSession().getUserRole();
                routeByRole(role);
            }
        });

        viewModel.errorMessage.observe(this, errorEvent -> {
            String error = errorEvent != null ? errorEvent.getContentIfNotHandled() : null;
            if (error == null) return;
            switch (error) {
                case "email_not_verified":
                    showSnackbar(getString(R.string.error_not_verified));
                    break;
                case "account_pending":
                    showSnackbar(getString(R.string.error_pending_approval));
                    break;
                case "account_rejected":
                    showSnackbar(getString(R.string.error_account_rejected));
                    break;
                case "network_error":
                    showSnackbar(getString(R.string.error_network));
                    break;
                case "server_error":
                    showSnackbar(getString(R.string.error_generic));
                    break;
                case "invalid_credentials":
                    showSnackbar(getString(R.string.error_login_failed));
                    break;
                default:
                    showSnackbar(getString(R.string.error_login_failed));
                    break;
            }
        });
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        binding.btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void attemptLogin() {
        String email = InputSanitizer.sanitizeEmail(binding.etEmail.getText().toString());
        String password = binding.etPassword.getText().toString();

        if (!ValidationUtils.isValidEmail(email)) {
            UiUtils.showToast(this, getString(R.string.error_invalid_email));
            return;
        }
        if (password.isEmpty()) {
            UiUtils.showToast(this, getString(R.string.error_field_required));
            return;
        }

        viewModel.getSession().saveRememberMe(binding.cbRememberMe.isChecked(), email);
        viewModel.login(email, password);
    }

    private void routeByRole(String role) {
        Intent intent;
        if (Constants.ROLE_ADMIN.equals(role)) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else if (Constants.ROLE_LIVE_DISPLAY.equals(role)) {
            intent = new Intent(this, LiveDisplayActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
