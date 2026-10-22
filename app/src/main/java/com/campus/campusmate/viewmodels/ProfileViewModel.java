package com.campus.campusmate.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.base.BaseViewModel;
import com.campus.campusmate.models.User;
import com.campus.campusmate.repositories.UserRepository;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.utils.SupabaseStorage;

public class ProfileViewModel extends BaseViewModel {

    private final UserRepository userRepo;
    private final SessionManager session;

    public final MutableLiveData<User> userProfile = new MutableLiveData<>();
    public final MutableLiveData<com.campus.campusmate.utils.Event<String>> successMessage = new MutableLiveData<>();
    /**
     * Whether a profile photo upload/change is in progress. Separate from {@link #isLoading},
     * which also flips on every background profile reload.
     */
    public final MutableLiveData<Boolean> photoBusy = new MutableLiveData<>(false);

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        userRepo = UserRepository.getInstance();
        userRepo.init(application);
        session = new SessionManager(application);
    }

    public void loadProfile(String uid) {
        if (uid == null) return;
        setLoading(true);
        userRepo.fetchUserProfile(uid, new UserRepository.OnUserFetchedListener() {
            @Override
            public void onFetched(User user) {
                setLoading(false);
                userProfile.postValue(user);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                setError(message);
            }
        });
    }

    public void observeProfile() {
        String uid = session.getUserId();
        if (uid == null) return;
        userRepo.observeUserProfile(uid, userProfile);
    }

    public void updateProfile(User user) {
        setLoading(true);
        userRepo.updateUserProfile(user.getUid(), user, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                session.saveSession(user);
                setLoading(false);
                successMessage.postValue(new com.campus.campusmate.utils.Event<>("Profile updated"));
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    /**
     * Compresses and uploads {@code uri} as the signed-in user's photo, points their profile at
     * it, then deletes the photo it replaced. Ignored while another photo change is running.
     */
    public void updateProfileImage(Uri uri) {
        String uid = session.getUserId();
        if (uid == null) {
            setError("You need to be signed in to change your photo");
            return;
        }
        if (Boolean.TRUE.equals(photoBusy.getValue())) return;
        photoBusy.setValue(true);

        SupabaseStorage.uploadProfilePhoto(getApplication(), uri, uid, new SupabaseStorage.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                savePhotoUrl(uid, publicUrl, "Profile photo updated");
            }

            @Override
            public void onFailure(String errorMessage) {
                photoBusy.setValue(false);
                setError("Photo upload failed: " + errorMessage);
            }
        });
    }

    /** Clears the signed-in user's photo and deletes the stored file. */
    public void removeProfileImage() {
        String uid = session.getUserId();
        if (uid == null || Boolean.TRUE.equals(photoBusy.getValue())) return;
        photoBusy.setValue(true);
        savePhotoUrl(uid, null, "Profile photo removed");
    }

    private void savePhotoUrl(String uid, @Nullable String newUrl, String successText) {
        User current = userProfile.getValue();
        String previousUrl = current != null ? current.getProfileImageUrl() : null;
        userRepo.updateProfileImageUrl(uid, newUrl, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                if (previousUrl != null && !previousUrl.isEmpty() && !previousUrl.equals(newUrl)) {
                    SupabaseStorage.deleteByPublicUrl(getApplication(), previousUrl);
                }
                User latest = userProfile.getValue();
                if (latest != null) {
                    latest.setProfileImageUrl(newUrl);
                    userProfile.postValue(latest);
                }
                photoBusy.postValue(false);
                successMessage.postValue(new com.campus.campusmate.utils.Event<>(successText));
            }

            @Override
            public void onError(String message) {
                // The profile still points at the old photo, so the new upload is orphaned.
                if (newUrl != null) SupabaseStorage.deleteByPublicUrl(getApplication(), newUrl);
                photoBusy.postValue(false);
                setError("Couldn't save your photo: " + message);
            }
        });
    }

    public SessionManager getSession() {
        return session;
    }
}
