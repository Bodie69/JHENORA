package com.campus.campusmate.base;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Base ViewModel class providing common state handling for loading and errors.
 */
public abstract class BaseViewModel extends AndroidViewModel {

    protected final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    protected final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    public BaseViewModel(@NonNull Application application) {
        super(application);
    }

    protected void setLoading(boolean loading) {
        _isLoading.setValue(loading);
    }

    protected void setError(String message) {
        _errorMessage.setValue(message);
        _isLoading.setValue(false);
    }

    protected void clearError() {
        _errorMessage.setValue(null);
    }
}
