package com.campus.campusmate.activities;

import android.content.Intent;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;

import androidx.annotation.RequiresApi;

import com.campus.campusmate.R;
import com.campus.campusmate.base.BaseActivity;
import com.campus.campusmate.databinding.ActivitySplashBinding;
import com.campus.campusmate.notifications.NotificationHelper;
import com.campus.campusmate.utils.Constants;

import timber.log.Timber;

public class SplashActivity extends BaseActivity<ActivitySplashBinding> {

    /** How long the static logo shows when the animation can't play. */
    private static final long SPLASH_DELAY = 2500L;
    /** Safety net: route even if the animation never reports that it ended (~6s long). */
    private static final long ANIMATION_TIMEOUT = 8000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean routed;

    @Override
    protected ActivitySplashBinding inflateBinding(LayoutInflater inflater) {
        return ActivitySplashBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        NotificationHelper.requestPermission(this);
        binding.layoutLoading.animate().alpha(1f).setStartDelay(300L).setDuration(400L).start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && animationsEnabled() && playLogoAnimation()) {
            handler.postDelayed(this::routeOnce, ANIMATION_TIMEOUT);
        } else {
            // Android 8.x, "Remove animations" on, or decode failure: static logo from the layout.
            handler.postDelayed(this::routeOnce, SPLASH_DELAY);
        }
    }

    /**
     * Plays the animated JHENORA logo (res/raw/splash_logo_anim.webp) once, then routes on.
     *
     * @return {@code false} if the animation couldn't be decoded, so the caller falls back.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private boolean playLogoAnimation() {
        try {
            Drawable drawable = ImageDecoder.decodeDrawable(
                    ImageDecoder.createSource(getResources(), R.raw.splash_logo_anim));
            if (!(drawable instanceof AnimatedImageDrawable)) return false;
            AnimatedImageDrawable logo = (AnimatedImageDrawable) drawable;
            logo.setRepeatCount(0); // play once, ending on the finished logo
            logo.registerAnimationCallback(new Animatable2.AnimationCallback() {
                @Override
                public void onAnimationEnd(Drawable d) {
                    routeOnce();
                }
            });
            binding.ivLogo.setImageDrawable(logo);
            logo.start();
            return true;
        } catch (Exception e) {
            Timber.w(e, "Splash animation unavailable; showing static logo");
            return false;
        }
    }

    private boolean animationsEnabled() {
        return Settings.Global.getFloat(getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
    }

    /** Routes exactly once, whichever of animation-end or the timeout fires first. */
    private void routeOnce() {
        if (routed || isFinishing()) return;
        routed = true;
        handler.removeCallbacksAndMessages(null);
        checkSession();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    protected void setupObservers() {
        // No ViewModel state to observe; routing happens when the logo animation ends (or on a
        // timed fallback) in setupViews().
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
