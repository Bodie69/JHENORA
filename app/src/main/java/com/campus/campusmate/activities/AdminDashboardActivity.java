package com.campus.campusmate.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.campus.campusmate.R;
import com.campus.campusmate.base.BaseActivity;
import com.campus.campusmate.databinding.ActivityAdminDashboardBinding;
import com.campus.campusmate.fragments.ProfileFragment;
import com.campus.campusmate.fragments.admin.AdminAnnouncementsFragment;
import com.campus.campusmate.fragments.admin.AdminApprovalsFragment;
import com.campus.campusmate.fragments.admin.AdminDepartmentsFragment;
import com.campus.campusmate.fragments.admin.AdminEventsFragment;
import com.campus.campusmate.fragments.admin.AdminHomeFragment;
import com.campus.campusmate.fragments.admin.AdminLiveDisplayFragment;
import com.campus.campusmate.fragments.admin.AdminOrganizationsFragment;
import com.campus.campusmate.fragments.admin.AdminStatisticsFragment;
import com.campus.campusmate.fragments.admin.AdminUsersFragment;
import com.campus.campusmate.notifications.NotificationHelper;
import com.campus.campusmate.viewmodels.AdminViewModel;

public class AdminDashboardActivity extends BaseActivity<ActivityAdminDashboardBinding> implements AdminHomeFragment.OnModuleSelectedListener {

    private AdminViewModel viewModel;

    private Fragment adminHomeFragment;
    private Fragment profileFragment;
    private Fragment activeFragment;

    private static final String[] TAB_TITLES = {
            "Stats", "Approvals", "Users", "Orgs", "Bulletin", "Events", "Depts", "Display"
    };

    @Override
    protected ActivityAdminDashboardBinding inflateBinding(LayoutInflater inflater) {
        return ActivityAdminDashboardBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        if (!session.isAdmin()) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        setupViewPager();
        setupFragments();
        setupBottomNav();
        setupBackNavigation();
        loadData();
        requestNotificationPermission();
    }

    @Override
    protected void setupObservers() {
        viewModel.successMessage.observe(this, msg -> {
            if (msg != null) {
                com.campus.campusmate.utils.UiUtils.showToast(this, msg);
            }
        });
    }

    private void setupFragments() {
        FragmentManager fm = getSupportFragmentManager();
        adminHomeFragment = fm.findFragmentByTag("admin_home");
        profileFragment = fm.findFragmentByTag("profile");

        if (adminHomeFragment == null) {
            adminHomeFragment = new AdminHomeFragment();
            profileFragment = new ProfileFragment();

            fm.beginTransaction()
                    .add(R.id.container_admin, adminHomeFragment, "admin_home")
                    .add(R.id.container_admin, profileFragment, "profile").hide(profileFragment)
                    .commit();
            activeFragment = adminHomeFragment;
            showDashboard(true);
        } else {
            // Restore state
            activeFragment = (profileFragment != null && !profileFragment.isHidden()) ? profileFragment : adminHomeFragment;
            showDashboard(activeFragment == adminHomeFragment && binding.viewPager.getVisibility() != View.VISIBLE);
        }
        
        if (adminHomeFragment instanceof AdminHomeFragment) {
            ((AdminHomeFragment) adminHomeFragment).setOnModuleSelectedListener(this);
        }
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.viewPager.getVisibility() == View.VISIBLE) {
                    showDashboard(true);
                } else if (activeFragment == profileFragment) {
                    switchBaseFragment(adminHomeFragment);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void showDashboard(boolean show) {
        if (show) {
            switchBaseFragment(adminHomeFragment);
            binding.tvHeaderTitle.setText("JHENORA");
        }
        binding.containerAdmin.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.viewPager.setVisibility(show ? View.GONE : View.VISIBLE);
        updateNavIcons(activeFragment);
    }

    private void switchBaseFragment(Fragment target) {
        if (target == null || target == activeFragment) return;
        
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .hide(activeFragment)
                .show(target)
                .commit();
        activeFragment = target;
        
        updateNavIcons(target);
    }

    private void setupBottomNav() {
        binding.btnNavStats.setOnClickListener(v -> onModuleSelected(0));
        binding.btnNavApprovals.setOnClickListener(v -> onModuleSelected(1));
        binding.btnNavHomeFab.setOnClickListener(v -> showDashboard(true));
        binding.btnNavUsers.setOnClickListener(v -> onModuleSelected(2));
        binding.btnNavProfile.setOnClickListener(v -> openProfile());
    }

    private void openProfile() {
        binding.containerAdmin.setVisibility(View.VISIBLE);
        binding.viewPager.setVisibility(View.GONE);
        switchBaseFragment(profileFragment);
        binding.tvHeaderTitle.setText("My Profile");
    }

    private void updateNavIcons(Fragment active) {
        boolean selectedStats = false;
        boolean selectedApprovals = false;
        boolean selectedUsers = false;
        boolean selectedProfile = (active == profileFragment);
        boolean selectedHome = (active == adminHomeFragment);

        if (binding.viewPager.getVisibility() == View.VISIBLE) {
            int current = binding.viewPager.getCurrentItem();
            selectedHome = false;
            if (current == 0) selectedStats = true;
            else if (current == 1) selectedApprovals = true;
            else if (current == 2) selectedUsers = true;
            else if (current == 3) selectedUsers = true;
            else if (current == 6) selectedUsers = true;
            else if (current == 7) selectedUsers = true;
            else if (current == 4) selectedProfile = true;
            else if (current == 5) selectedProfile = true;
        }

        setNavButtonState(binding.btnNavStats, selectedStats);
        setNavButtonState(binding.btnNavApprovals, selectedApprovals);
        setNavButtonState(binding.btnNavUsers, selectedUsers);
        setNavButtonState(binding.btnNavProfile, selectedProfile);
        setNavButtonState(binding.btnNavHomeFab, selectedHome);
    }

    private void setNavButtonState(android.widget.ImageButton button, boolean selected) {
        button.setSelected(selected);
        button.setAlpha(selected ? 1.0f : 0.6f);
    }

    @Override
    public void onModuleSelected(int position) {
        binding.containerAdmin.setVisibility(View.GONE);
        binding.viewPager.setVisibility(View.VISIBLE);
        binding.viewPager.setCurrentItem(position, false);
        binding.tvHeaderTitle.setText(TAB_TITLES[position]);
        
        // Update icons
        updateNavIcons(activeFragment);
    }

    private void requestNotificationPermission() {
        NotificationHelper.requestPermission(this);
    }

    private void setupViewPager() {
        AdminPagerAdapter adapter = new AdminPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setOffscreenPageLimit(4);

        binding.viewPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                binding.tvHeaderTitle.setText(TAB_TITLES[position]);
                updateNavIcons(activeFragment);
            }
        });
    }

    private void loadData() {
        viewModel.loadAllUsers();
        viewModel.loadPendingUsers();
        viewModel.loadOrganizations();
    }

    private static class AdminPagerAdapter extends FragmentStateAdapter {
        AdminPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public int getItemCount() { return TAB_TITLES.length; }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new AdminStatisticsFragment();
                case 1: return new AdminApprovalsFragment();
                case 2: return new AdminUsersFragment();
                case 3: return new AdminOrganizationsFragment();
                case 4: return new AdminAnnouncementsFragment();
                case 5: return new AdminEventsFragment();
                case 6: return new AdminDepartmentsFragment();
                case 7: return new AdminLiveDisplayFragment();
                default: return new AdminStatisticsFragment();
            }
        }
    }
}
