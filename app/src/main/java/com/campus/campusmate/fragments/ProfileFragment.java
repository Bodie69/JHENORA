package com.campus.campusmate.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.campus.campusmate.BuildConfig;
import com.campus.campusmate.R;
import com.campus.campusmate.activities.LoginActivity;
import com.campus.campusmate.activities.MyEventsActivity;
import com.campus.campusmate.activities.PostHistoryActivity;
import com.campus.campusmate.base.BaseFragment;
import com.campus.campusmate.databinding.FragmentProfileBinding;
import com.campus.campusmate.models.User;
import com.campus.campusmate.utils.AvatarLoader;
import com.campus.campusmate.utils.DepartmentLogos;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.campus.campusmate.viewmodels.AuthViewModel;
import com.campus.campusmate.viewmodels.ProfileViewModel;

public class ProfileFragment extends BaseFragment<FragmentProfileBinding> {

    private ProfileViewModel profileViewModel;
    private AuthViewModel authViewModel;

    private static final String STATE_CAMERA_URI = "camera_uri";

    /** Where the camera app is writing the current capture; kept across process death. */
    @Nullable private Uri pendingCameraUri;

    /** System photo picker: no storage permission needed. */
    private final ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) profileViewModel.updateProfileImage(uri);
            }
    );

    private final ActivityResultLauncher<Uri> takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            saved -> {
                if (Boolean.TRUE.equals(saved) && pendingCameraUri != null) {
                    profileViewModel.updateProfileImage(pendingCameraUri);
                }
                pendingCameraUri = null;
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) pendingCameraUri = savedInstanceState.getParcelable(STATE_CAMERA_URI);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_CAMERA_URI, pendingCameraUri);
    }

    @Override
    protected FragmentProfileBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupHistoryNav();
        setupPhotoUpload();
        setupEditProfile();
        setupLogout();
        binding.tvVersion.setText(getString(R.string.app_version,
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        String uid = authViewModel.getSession().getUserId();
        if (uid != null) profileViewModel.loadProfile(uid);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Covers returning from Create/Edit Profile/Detail activities: the host
        // Activity resumes (and so do we) even while this tab is hidden.
        String uid = authViewModel.getSession().getUserId();
        if (uid != null) profileViewModel.loadProfile(uid);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // MainActivity keeps bottom-nav tabs alive via show()/hide() rather than
        // recreating them, so this covers switching back to this tab (onResume
        // above only fires on Activity-level resume, e.g. returning from another
        // screen — not on a same-Activity show()/hide() tab switch).
        if (!hidden) {
            String uid = authViewModel.getSession().getUserId();
            if (uid != null) profileViewModel.loadProfile(uid);
        }
    }

    /** "Post History" and "My Events" are dedicated pages (see {@link PostHistoryActivity},
     *  {@link MyEventsActivity}) rather than lists embedded directly in this scrolling page. */
    private void setupHistoryNav() {
        binding.rowPostHistory.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PostHistoryActivity.class)));
        binding.rowMyEvents.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MyEventsActivity.class)));
    }

    private void setupPhotoUpload() {
        binding.btnEditPhoto.setOnClickListener(v -> showPhotoOptions());
        binding.frameAvatar.setOnClickListener(v -> showPhotoOptions());
    }

    private void showPhotoOptions() {
        if (Boolean.TRUE.equals(profileViewModel.photoBusy.getValue())) return;
        User user = profileViewModel.userProfile.getValue();
        boolean hasPhoto = user != null && !TextUtils.isEmpty(user.getProfileImageUrl());

        List<String> options = new ArrayList<>();
        options.add(getString(R.string.profile_photo_gallery));
        options.add(getString(R.string.profile_photo_camera));
        if (hasPhoto) options.add(getString(R.string.profile_photo_remove));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_photo_title)
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        pickImageLauncher.launch(new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build());
                    } else if (which == 1) {
                        takePhoto();
                    } else {
                        profileViewModel.removeProfileImage();
                    }
                })
                .show();
    }

    private void takePhoto() {
        File dir = new File(requireContext().getCacheDir(), "camera");
        if (!dir.exists() && !dir.mkdirs()) {
            showSnackbar(getString(R.string.profile_photo_camera_unavailable));
            return;
        }
        // One reusable capture file: the photo is compressed and uploaded from it right away.
        File file = new File(dir, "profile_capture.jpg");
        pendingCameraUri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider", file);
        try {
            takePhotoLauncher.launch(pendingCameraUri);
        } catch (ActivityNotFoundException e) {
            pendingCameraUri = null;
            showSnackbar(getString(R.string.profile_photo_camera_unavailable));
        }
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
        profileViewModel.photoBusy.observe(getViewLifecycleOwner(), busy -> {
            boolean on = Boolean.TRUE.equals(busy);
            binding.progressPhoto.setVisibility(on ? View.VISIBLE : View.GONE);
            binding.btnEditPhoto.setEnabled(!on);
            binding.frameAvatar.setEnabled(!on);
        });
        profileViewModel.errorMessage.observe(getViewLifecycleOwner(), event -> {
            String message = event != null ? event.getContentIfNotHandled() : null;
            if (message != null) showSnackbar(message);
        });
        profileViewModel.successMessage.observe(getViewLifecycleOwner(), event -> {
            String message = event != null ? event.getContentIfNotHandled() : null;
            if (message != null) showSnackbar(message);
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
            DepartmentLogos.bind(binding.tvDepartment, dept);
        } else {
            binding.rowDepartment.setVisibility(View.GONE);
        }

        // Initials sit under the photo and stay as the fallback if it is missing or fails to load.
        binding.tvInitials.setText(getInitials(user.getFullName()));
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(Color.parseColor("#E0E0E0"));
        binding.tvInitials.setBackground(drawable);
        binding.frameAvatar.setContentDescription(getString(R.string.profile_photo_change));
        AvatarLoader.load(binding.ivProfile, binding.tvInitials, user.getProfileImageUrl());

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
