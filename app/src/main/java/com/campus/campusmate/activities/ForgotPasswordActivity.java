package com.campus.campusmate.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.campus.campusmate.R;
import com.campus.campusmate.base.BaseActivity;
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
public class ForgotPasswordActivity extends BaseActivity<ActivityForgotPasswordBinding> {

    private static final long RESEND_COOLDOWN_MS = 60_000L;
    private static final String KEY_STEP = "current_step";
    private static final String KEY_EMAIL = "current_email";

    private ForgotPasswordViewModel viewModel;

    private int currentStep = 1;
    private String currentEmail = "";
    private CountDownTimer resendTimer;
    /** True while the 60s resend cooldown is running; shared by both the step-1 "Send Code"
     *  button and the step-2 "Resend" link since they trigger the same sendOtp() call. */
    private boolean cooldownActive = false;

    /** Stashed here (before super.onCreate() runs setupViews()) so step/email survive a
     *  configuration change instead of silently resetting to step 1 with a blank email while
     *  the ViewModel's LiveData still reports the later step as already reached. */
    private Bundle pendingState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        pendingState = savedInstanceState;
        super.onCreate(savedInstanceState);
    }

    @Override
    protected ActivityForgotPasswordBinding inflateBinding(LayoutInflater inflater) {
        return ActivityForgotPasswordBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(ForgotPasswordViewModel.class);

        setupBackNavigation();
        setupListeners();
        UiUtils.setupPasswordToggle(binding.btnToggleNewPassword, binding.etNewPassword);
        UiUtils.setupPasswordToggle(binding.btnToggleConfirmNewPassword, binding.etConfirmNewPassword);
        restoreState();
    }

    private void restoreState() {
        if (pendingState == null) return;
        currentStep = pendingState.getInt(KEY_STEP, 1);
        currentEmail = pendingState.getString(KEY_EMAIL, "");
        pendingState = null;

        binding.etEmail.setText(currentEmail);
        if (currentStep >= 2) {
            binding.tvOtpSubtitle.setText(getString(R.string.subtitle_verify_code) + "\n" + currentEmail);
        }
        goToStep(currentStep);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_STEP, currentStep);
        outState.putString(KEY_EMAIL, currentEmail);
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

    @Override
    protected void setupObservers() {
        viewModel.isLoading.observe(this, loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            // btnSendCode also stays disabled through the resend cooldown, independent of
            // loading state (e.g. verifying the OTP briefly toggles isLoading and would
            // otherwise re-enable it while the cooldown is still running).
            binding.btnSendCode.setEnabled(!loading && !cooldownActive);
            binding.btnVerifyCode.setEnabled(!loading);
            binding.btnResetPassword.setEnabled(!loading);
        });

        viewModel.otpSent.observe(this, sent -> {
            if (!Boolean.TRUE.equals(sent)) return;
            UiUtils.showLongToast(this, getString(R.string.success_otp_sent));
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

        viewModel.errorMessage.observe(this, errorEvent -> {
            String error = errorEvent != null ? errorEvent.getContentIfNotHandled() : null;
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
        // Guards against bypassing the resend cooldown by backing out to step 1 and tapping
        // "Send Code" again — that hit the same sendOtp() call as the step-2 resend link, just
        // without the cooldown check, so a user could hammer Supabase's recovery-email endpoint
        // by going back and forth. The button is also disabled while cooldownActive is true;
        // this is a defensive second check against races/accessibility taps.
        if (cooldownActive) {
            UiUtils.showToast(this, getString(R.string.error_otp_cooldown_active));
            return;
        }
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
        cooldownActive = true;
        binding.tvResendCode.setEnabled(false);
        binding.btnSendCode.setEnabled(false);
        resendTimer = new CountDownTimer(RESEND_COOLDOWN_MS, 1_000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.tvResendCode.setText(
                        getString(R.string.btn_resend_code_countdown, millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                cooldownActive = false;
                binding.tvResendCode.setText(R.string.btn_resend_code);
                binding.tvResendCode.setEnabled(true);
                binding.btnSendCode.setEnabled(true);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) resendTimer.cancel();
    }
}
