package com.campus.campusmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.campus.campusmate.databinding.ActivitySplashBinding;
import com.campus.campusmate.notifications.NotificationHelper;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2500L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySplashBinding binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NotificationHelper.requestPermission(this);

        new Handler(Looper.getMainLooper()).postDelayed(this::checkSession, SPLASH_DELAY);
    }

    private void checkSession() {
        SessionManager session = new SessionManager(this);

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
