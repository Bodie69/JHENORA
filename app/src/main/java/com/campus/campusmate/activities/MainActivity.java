package com.campus.campusmate.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.LayoutInflater;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.campus.campusmate.R;
import com.campus.campusmate.base.BaseActivity;
import com.campus.campusmate.databinding.ActivityMainBinding;
import com.campus.campusmate.fragments.AnnouncementFragment;
import com.campus.campusmate.fragments.CalendarFragment;
import com.campus.campusmate.fragments.EventFragment;
import com.campus.campusmate.fragments.HomeFragment;
import com.campus.campusmate.fragments.ProfileFragment;
import com.campus.campusmate.notifications.NotificationHelper;

public class MainActivity extends BaseActivity<ActivityMainBinding> {

    private Fragment homeFragment;
    private Fragment announcementFragment;
    private Fragment eventFragment;
    private Fragment calendarFragment;
    private Fragment profileFragment;
    private Fragment activeFragment;

    @Override
    protected ActivityMainBinding inflateBinding(LayoutInflater inflater) {
        return ActivityMainBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        if (!session.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        setupFragments();
        setupBottomNav();
        requestNotificationPermission();
    }

    @Override
    protected void setupObservers() {
        // No global observers for MainActivity yet
    }

    private void requestNotificationPermission() {
        NotificationHelper.requestPermission(this);
    }

    private void setupFragments() {
        FragmentManager fm = getSupportFragmentManager();
        homeFragment = fm.findFragmentByTag("home");
        announcementFragment = fm.findFragmentByTag("announcements");
        eventFragment = fm.findFragmentByTag("events");
        calendarFragment = fm.findFragmentByTag("calendar");
        profileFragment = fm.findFragmentByTag("profile");

        if (homeFragment == null) {
            homeFragment = new HomeFragment();
            announcementFragment = new AnnouncementFragment();
            eventFragment = new EventFragment();
            calendarFragment = new CalendarFragment();
            profileFragment = new ProfileFragment();

            fm.beginTransaction()
                    .add(R.id.fragment_container, homeFragment, "home")
                    .add(R.id.fragment_container, announcementFragment, "announcements").hide(announcementFragment)
                    .add(R.id.fragment_container, eventFragment, "events").hide(eventFragment)
                    .add(R.id.fragment_container, calendarFragment, "calendar").hide(calendarFragment)
                    .add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment)
                    .commit();
            activeFragment = homeFragment;
        } else {
            // Restore active fragment if possible, or default to home
            activeFragment = homeFragment; // Simplified; could track in savedInstanceState
            if (announcementFragment != null && !announcementFragment.isHidden()) activeFragment = announcementFragment;
            else if (eventFragment != null && !eventFragment.isHidden()) activeFragment = eventFragment;
            else if (calendarFragment != null && !calendarFragment.isHidden()) activeFragment = calendarFragment;
            else if (profileFragment != null && !profileFragment.isHidden()) activeFragment = profileFragment;
        }
        
        binding.tvHeaderTitle.setText(titleFor(activeFragment));
        updateNavIcons(activeFragment);
    }

    private void setupBottomNav() {
        binding.btnNavHome.setOnClickListener(v -> switchFragment(homeFragment));
        binding.btnNavAnnouncements.setOnClickListener(v -> switchFragment(announcementFragment));
        binding.btnNavEvents.setOnClickListener(v -> switchFragment(eventFragment));
        binding.btnNavCalendar.setOnClickListener(v -> switchFragment(calendarFragment));
        binding.btnNavProfile.setOnClickListener(v -> switchFragment(profileFragment));
    }

    private void switchFragment(Fragment target) {
        if (target == null || target == activeFragment) return;
        
        binding.tvHeaderTitle.setText(titleFor(target));

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .hide(activeFragment)
                .show(target)
                .commit();
        activeFragment = target;

        // Update icon states
        updateNavIcons(target);
    }

    private String titleFor(Fragment target) {
        if (target == announcementFragment) return getString(R.string.nav_announcements);
        if (target == eventFragment) return getString(R.string.nav_events);
        if (target == calendarFragment) return getString(R.string.nav_calendar);
        if (target == profileFragment) return "My Profile";
        return "JHENORA";
    }

    private void updateNavIcons(Fragment active) {
        setNavButtonState(binding.btnNavHome, active == homeFragment);
        setNavButtonState(binding.btnNavAnnouncements, active == announcementFragment);
        setNavButtonState(binding.btnNavEvents, active == eventFragment);
        setNavButtonState(binding.btnNavCalendar, active == calendarFragment);
        setNavButtonState(binding.btnNavProfile, active == profileFragment);
    }

    private void setNavButtonState(android.widget.ImageButton button, boolean selected) {
        button.setSelected(selected);
        button.setAlpha(selected ? 1.0f : 0.6f);
    }

    public void navigateToAnnouncements() {
        switchFragment(announcementFragment);
    }

    public void navigateToEvents() {
        switchFragment(eventFragment);
    }

    public void navigateToCalendar() {
        switchFragment(calendarFragment);
    }

    public void navigateToProfile() {
        switchFragment(profileFragment);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
