package com.campus.campusmate.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.base.BaseViewModel;
import com.campus.campusmate.models.User;
import com.campus.campusmate.repositories.ImageStorageRepository;
import com.campus.campusmate.repositories.UserRepository;
import com.campus.campusmate.utils.SessionManager;

public class ProfileViewModel extends BaseViewModel {

    private final UserRepository userRepo;
    private final SessionManager session;
    private final ImageStorageRepository storageRepo;

    public final MutableLiveData<User> userProfile = new MutableLiveData<>();
    public final MutableLiveData<String> successMessage = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        userRepo = UserRepository.getInstance();
        userRepo.init(application);
        session = new SessionManager(application);
        storageRepo = ImageStorageRepository.getInstance();
        storageRepo.init(application);
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
                successMessage.postValue("Profile updated");
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    public void updateProfileImage(Uri uri) {
        String uid = session.getUserId();
        if (uid == null) return;

        setLoading(true);
        String fileName = "profile_" + uid + ".jpg";
        storageRepo.uploadImage(uri, "profiles", fileName, new ImageStorageRepository.OnUploadListener() {
            @Override
            public void onSuccess(String url) {
                performUpdateImage(uid, url);
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    private void performUpdateImage(String uid, String url) {
        User current = userProfile.getValue();
        if (current == null) return;
        
        current.setProfileImageUrl(url);
        userRepo.updateUserProfile(uid, current, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                session.saveSession(current);
                userProfile.postValue(current);
                setLoading(false);
                successMessage.postValue("Profile photo updated");
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    public SessionManager getSession() {
        return session;
    }
}
