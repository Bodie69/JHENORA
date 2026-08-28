package com.campus.campusmate.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.base.BaseViewModel;
import com.campus.campusmate.models.User;
import com.campus.campusmate.repositories.UserRepository;
import com.campus.campusmate.utils.SessionManager;

public class AuthViewModel extends BaseViewModel {

    private final UserRepository userRepo;
    private final SessionManager session;

    public final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    public final MutableLiveData<Boolean> registerSuccess = new MutableLiveData<>();
    public final MutableLiveData<User> currentUser = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        userRepo = UserRepository.getInstance();
        userRepo.init(application);
        session = new SessionManager(application);
    }

    public void login(String email, String password) {
        setLoading(true);
        userRepo.loginUser(email, password, new UserRepository.OnLoginListener() {
            @Override
            public void onSuccess(User user) {
                session.saveSession(user);
                currentUser.postValue(user);
                setLoading(false);
                loginSuccess.postValue(true);
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    public void registerStudent(String email, String password, User user) {
        setLoading(true);
        user.setRole(User.ROLE_STUDENT);
        userRepo.registerUser(email, password, user, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                setLoading(false);
                registerSuccess.postValue(true);
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    public void registerFaculty(String email, String password, User user) {
        setLoading(true);
        user.setRole(User.ROLE_FACULTY);
        userRepo.registerUser(email, password, user, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                setLoading(false);
                registerSuccess.postValue(true);
            }

            @Override
            public void onError(String message) {
                setError(message);
            }
        });
    }

    public void logout() {
        userRepo.logout();
    }

    public boolean isLoggedIn() {
        return session.isLoggedIn();
    }

    public SessionManager getSession() {
        return session;
    }
}
