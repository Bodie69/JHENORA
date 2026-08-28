package com.campus.campusmate.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.campus.campusmate.R;
import com.campus.campusmate.activities.AnnouncementDetailActivity;
import com.campus.campusmate.activities.EventDetailActivity;
import com.campus.campusmate.activities.LoginActivity;
import com.campus.campusmate.adapters.AnnouncementAdapter;
import com.campus.campusmate.adapters.EventAdapter;
import com.campus.campusmate.base.BaseFragment;
import com.campus.campusmate.databinding.FragmentProfileBinding;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.models.User;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.viewmodels.AnnouncementViewModel;
import com.campus.campusmate.viewmodels.AuthViewModel;
import com.campus.campusmate.viewmodels.EventViewModel;
import com.campus.campusmate.viewmodels.ProfileViewModel;

public class ProfileFragment extends BaseFragment<FragmentProfileBinding> {

    private ProfileViewModel profileViewModel;
    private AnnouncementViewModel announcementViewModel;
    private EventViewModel eventViewModel;
    private AuthViewModel authViewModel;

    private AnnouncementAdapter historyAdapter;
    private EventAdapter eventHistoryAdapter;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    profileViewModel.updateProfileImage(uri);
                }
            }
    );

    @Override
    protected FragmentProfileBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        announcementViewModel = new ViewModelProvider(this).get(AnnouncementViewModel.class);
        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupPostHistory();
        setupEventHistory();
        setupPhotoUpload();
        setupEditProfile();
        setupLogout();

        String uid = authViewModel.getSession().getUserId();
        if (uid != null) {
            profileViewModel.loadProfile(uid);
            announcementViewModel.loadUserAnnouncements(uid);
            eventViewModel.loadUserEvents(uid);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        String uid = authViewModel.getSession().getUserId();
        if (uid != null) {
            profileViewModel.loadProfile(uid);
        }
    }

    private void setupPostHistory() {
        historyAdapter = new AnnouncementAdapter(requireContext());
        historyAdapter.setListener(new AnnouncementAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Announcement a) {
                Intent intent = new Intent(requireContext(), AnnouncementDetailActivity.class);
                intent.putExtra(Constants.EXTRA_ANNOUNCEMENT_ID, a.getId());
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(Announcement a, View anchor) {}
        });
        binding.rvPostHistory.setAdapter(historyAdapter);
    }

    private void setupEventHistory() {
        eventHistoryAdapter = new EventAdapter(requireContext());
        eventHistoryAdapter.setListener(new EventAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Event e) {
                Intent intent = new Intent(requireContext(), EventDetailActivity.class);
                intent.putExtra(Constants.EXTRA_EVENT_ID, e.getId());
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(Event e, View anchor) {}
        });
        binding.rvEventHistory.setAdapter(eventHistoryAdapter);
    }

    private void setupPhotoUpload() {
        binding.btnEditPhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    private void setupEditProfile() {
        binding.btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.campus.campusmate.activities.EditProfileActivity.class);
            startActivity(intent);
        });
    }

    private void setupLogout() {
        binding.btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        authViewModel.logout();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    protected void setupObservers() {
        profileViewModel.userProfile.observe(getViewLifecycleOwner(), this::bindUser);

        announcementViewModel.userAnnouncements.observe(getViewLifecycleOwner(), list -> {
            historyAdapter.submitList(list);
            binding.tvNoHistory.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        eventViewModel.userEvents.observe(getViewLifecycleOwner(), list -> {
            eventHistoryAdapter.submitList(list);
            binding.tvNoEventHistory.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        profileViewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            // No progress bar in this layout version apparently, or it's named differently
            // Checking the XML again: it doesn't have a ProgressBar.
        });
    }

    private void bindUser(User user) {
        if (user == null) return;

        binding.tvName.setText(user.getFullName());
        binding.tvEmail.setText(user.getEmail());
        
        String role = user.getRole();
        if (role != null && !role.isEmpty()) {
            binding.rowRole.setVisibility(View.VISIBLE);
            binding.tvRole.setText(capitalize(role));
        } else {
            binding.rowRole.setVisibility(View.GONE);
        }
        
        String dept = user.getDepartment();
        if (dept != null && !dept.isEmpty()) {
            binding.rowDepartment.setVisibility(View.VISIBLE);
            binding.tvDepartment.setText(dept);
        } else {
            binding.rowDepartment.setVisibility(View.GONE);
        }

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            binding.ivProfile.setVisibility(View.VISIBLE);
            binding.tvInitials.setVisibility(View.GONE);
            Glide.with(this).load(user.getProfileImageUrl()).into(binding.ivProfile);
        } else {
            binding.ivProfile.setVisibility(View.GONE);
            binding.tvInitials.setVisibility(View.VISIBLE);
            binding.tvInitials.setText(getInitials(user.getFullName()));

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(Color.parseColor("#E0E0E0"));
            binding.tvInitials.setBackground(drawable);
        }

        if (user.isStudent()) {
            binding.tvYearBlock.setVisibility(View.VISIBLE);
            binding.tvYearBlock.setText(String.format("%s - %s", user.getYearLevel(), user.getBlock()));
            binding.tvProfession.setVisibility(View.GONE);
            binding.tvOrganization.setVisibility(View.GONE);
        } else if (user.isOrganization()) {
            binding.tvYearBlock.setVisibility(View.GONE);
            binding.tvProfession.setVisibility(View.GONE);
            binding.tvOrganization.setVisibility(View.VISIBLE);
            binding.tvOrganization.setText(user.getOrganizationName());
        } else if (user.isFaculty()) {
            binding.tvYearBlock.setVisibility(View.GONE);
            binding.tvProfession.setVisibility(View.VISIBLE);
            binding.tvProfession.setText(user.getProfession());
            binding.tvOrganization.setVisibility(View.GONE);
        } else {
            // Admin or other role
            binding.tvYearBlock.setVisibility(View.GONE);
            binding.tvProfession.setVisibility(View.GONE);
            binding.tvOrganization.setVisibility(View.GONE);
        }

        binding.badgeAdmin.setVisibility(user.isAdmin() ? View.VISIBLE : View.GONE);
        binding.sectionPostHistory.setVisibility(user.canPost() ? View.VISIBLE : View.GONE);
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.split(" ");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < Math.min(parts.length, 2); i++) {
            if (!parts[i].isEmpty()) initials.append(parts[i].charAt(0));
        }
        return initials.toString().toUpperCase();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
