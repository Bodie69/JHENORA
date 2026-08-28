package com.campus.campusmate.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.repositories.UserRepository;
import com.campus.campusmate.utils.InputSanitizer;

public class ForgotPasswordViewModel extends AndroidViewModel {

    private final UserRepository userRepo;

    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public final MutableLiveData<Boolean> otpSent = new MutableLiveData<>();
    public final MutableLiveData<Boolean> otpVerified = new MutableLiveData<>();
    public final MutableLiveData<Boolean> passwordResetSuccess = new MutableLiveData<>();

    public ForgotPasswordViewModel(@NonNull Application application) {
        super(application);
        userRepo = UserRepository.getInstance();
        userRepo.init(application);
    }

    public void sendOtp(String email) {
        isLoading.setValue(true);
        userRepo.sendPasswordResetOtp(InputSanitizer.sanitizeEmail(email), new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                isLoading.postValue(false);
                otpSent.postValue(true);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void verifyOtp(String email, String otp) {
        isLoading.setValue(true);
        userRepo.verifyPasswordResetOtp(InputSanitizer.sanitizeEmail(email), otp,
                new UserRepository.OnCompleteListener() {
                    @Override
                    public void onSuccess() {
                        isLoading.postValue(false);
                        otpVerified.postValue(true);
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        errorMessage.postValue(message);
                    }
                });
    }

    public void resetPassword(String email, String newPassword) {
        isLoading.setValue(true);
        userRepo.resetPassword(InputSanitizer.sanitizeEmail(email), newPassword, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                isLoading.postValue(false);
                passwordResetSuccess.postValue(true);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }
}
