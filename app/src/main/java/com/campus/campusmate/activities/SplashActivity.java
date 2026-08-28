package com.campus.campusmate.activities;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;

import com.campus.campusmate.base.BaseActivity;
import com.campus.campusmate.databinding.ActivitySplashBinding;
import com.campus.campusmate.notifications.NotificationHelper;
import com.campus.campusmate.utils.Constants;

public class SplashActivity extends BaseActivity<ActivitySplashBinding> {

    private static final long SPLASH_DELAY = 2500L;

    @Override
    protected ActivitySplashBinding inflateBinding(LayoutInflater inflater) {
        return ActivitySplashBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        NotificationHelper.requestPermission(this);
        new Handler(Looper.getMainLooper()).postDelayed(this::checkSession, SPLASH_DELAY);
    }

    @Override
    protected void setupObservers() {
        // No ViewModel state to observe; session routing happens on a timed delay in setupViews().
    }

    private void checkSession() {
        if (session.isLoggedIn()) {
            routeByRole(session.getUserRole());
        } else {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        }
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
