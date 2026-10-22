package com.campus.campusmate.base;

import android.app.Application;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.utils.Event;

/**
 * Base ViewModel class providing common state handling for loading and errors.
 */
public abstract class BaseViewModel extends AndroidViewModel {

    protected final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    /**
     * One-time error messages. Wrapped in {@link Event} so a message is shown once: LiveData
     * replays its last value to re-attached observers (e.g. after rotation), which otherwise
     * made old errors pop up again.
     */
    protected final MutableLiveData<Event<String>> _errorMessage = new MutableLiveData<>();
    public final LiveData<Event<String>> errorMessage = _errorMessage;

    public BaseViewModel(@NonNull Application application) {
        super(application);
    }

    /** Safe to call from any thread (repository callbacks may run off the main thread). */
    protected void setLoading(boolean loading) {
        if (isMainThread()) _isLoading.setValue(loading);
        else _isLoading.postValue(loading);
    }

    /** Safe to call from any thread (repository callbacks may run off the main thread). */
    protected void setError(String message) {
        if (isMainThread()) {
            _errorMessage.setValue(new Event<>(message));
            _isLoading.setValue(false);
        } else {
            _errorMessage.postValue(new Event<>(message));
            _isLoading.postValue(false);
        }
    }

    protected void clearError() {
        if (isMainThread()) _errorMessage.setValue(null);
        else _errorMessage.postValue(null);
    }

    private static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
}
