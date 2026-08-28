package com.campus.campusmate.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.campus.campusmate.R;
import com.campus.campusmate.databinding.ActivityForgotPasswordBinding;
import com.campus.campusmate.utils.InputSanitizer;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.utils.ValidationUtils;
import com.campus.campusmate.viewmodels.ForgotPasswordViewModel;

/**
 * Drives the in-app "Forgot Password" flow entirely via email OTP:
 * 1) request a verification code, 2) verify it, 3) set a new password.
 * No browser or deep-link reset is involved.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private static final long RESEND_COOLDOWN_MS = 60_000L;

    private ActivityForgotPasswordBinding binding;
    private ForgotPasswordViewModel viewModel;

    private int currentStep = 1;
    private String currentEmail = "";
    private CountDownTimer resendTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ForgotPasswordViewModel.class);

        setupBackNavigation();
        setupObservers();
        setupListeners();
        UiUtils.setupPasswordToggle(binding.btnToggleNewPassword, binding.etNewPassword);
        UiUtils.setupPasswordToggle(binding.btnToggleConfirmNewPassword, binding.etConfirmNewPassword);
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentStep > 1) {
                    goToStep(currentStep - 1);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void setupObservers() {
        viewModel.isLoading.observe(this, loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnSendCode.setEnabled(!loading);
            binding.btnVerifyCode.setEnabled(!loading);
            binding.btnResetPassword.setEnabled(!loading);
        });

        viewModel.otpSent.observe(this, sent -> {
            if (!Boolean.TRUE.equals(sent)) return;
            UiUtils.showToast(this, getString(R.string.success_otp_sent));
            if (currentStep == 1) {
                binding.tvOtpSubtitle.setText(getString(R.string.subtitle_verify_code) + "\n" + currentEmail);
                goToStep(2);
            }
            startResendCooldown();
        });

        viewModel.otpVerified.observe(this, verified -> {
            if (Boolean.TRUE.equals(verified)) goToStep(3);
        });

        viewModel.passwordResetSuccess.observe(this, success -> {
            if (!Boolean.TRUE.equals(success)) return;
            UiUtils.showLongToast(this, getString(R.string.success_password_reset_complete));
            finish();
        });

        viewModel.errorMessage.observe(this, error -> {
            if (error != null) UiUtils.showSnackbar(binding.getRoot(), error);
        });
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnSendCode.setOnClickListener(v -> attemptSendCode());
        binding.btnVerifyCode.setOnClickListener(v -> attemptVerifyCode());
        binding.btnResetPassword.setOnClickListener(v -> attemptResetPassword());
        binding.tvResendCode.setOnClickListener(v -> {
            if (binding.tvResendCode.isEnabled()) viewModel.sendOtp(currentEmail);
        });
    }

    private void attemptSendCode() {
        String email = InputSanitizer.sanitizeEmail(binding.etEmail.getText().toString());
        if (!ValidationUtils.isValidEmail(email)) {
            UiUtils.showToast(this, getString(R.string.error_invalid_email));
            return;
        }
        currentEmail = email;
        viewModel.sendOtp(email);
    }

    private void attemptVerifyCode() {
        String otp = binding.etOtp.getText().toString().trim();
        if (otp.isEmpty()) {
            UiUtils.showToast(this, getString(R.string.error_invalid_otp));
            return;
        }
        viewModel.verifyOtp(currentEmail, otp);
    }

    private void attemptResetPassword() {
        String password = binding.etNewPassword.getText().toString();
        String confirm = binding.etConfirmNewPassword.getText().toString();

        if (!ValidationUtils.isValidPassword(password)) {
            UiUtils.showToast(this, getString(R.string.error_password_short));
            return;
        }
        if (!ValidationUtils.passwordsMatch(password, confirm)) {
            UiUtils.showToast(this, getString(R.string.error_passwords_mismatch));
            return;
        }
        viewModel.resetPassword(currentEmail, password);
    }

    private void goToStep(int step) {
        currentStep = step;
        binding.layoutStepEmail.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        binding.layoutStepOtp.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        binding.layoutStepNewPassword.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
    }

    private void startResendCooldown() {
        if (resendTimer != null) resendTimer.cancel();
        binding.tvResendCode.setEnabled(false);
        resendTimer = new CountDownTimer(RESEND_COOLDOWN_MS, 1_000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.tvResendCode.setText(
                        getString(R.string.btn_resend_code_countdown, millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                binding.tvResendCode.setText(R.string.btn_resend_code);
                binding.tvResendCode.setEnabled(true);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) resendTimer.cancel();
    }
}
