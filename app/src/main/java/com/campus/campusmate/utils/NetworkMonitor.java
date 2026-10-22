package com.campus.campusmate.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * App-wide view of whether the device has a working internet connection. Screens showing the
 * offline copy of a feed observe {@link #isOnline()} to reload as soon as the connection returns.
 */
public final class NetworkMonitor {

    private static volatile NetworkMonitor instance;

    private final MutableLiveData<Boolean> online;
    private boolean lastPosted;

    private NetworkMonitor(Context context) {
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        lastPosted = hasInternet(cm.getNetworkCapabilities(cm.getActiveNetwork()));
        online = new MutableLiveData<>(lastPosted);
        cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
                setOnline(hasInternet(caps));
            }

            @Override
            public void onLost(@NonNull Network network) {
                setOnline(false);
            }
        });
    }

    public static NetworkMonitor getInstance(Context context) {
        if (instance == null) {
            synchronized (NetworkMonitor.class) {
                if (instance == null) instance = new NetworkMonitor(context.getApplicationContext());
            }
        }
        return instance;
    }

    /** Emits only on change, so observers can treat {@code true} as "just came back online". */
    public LiveData<Boolean> isOnline() {
        return online;
    }

    private synchronized void setOnline(boolean value) {
        // Callbacks arrive on a binder thread, and repeats (e.g. signal changes) are common.
        // Compare with the last value posted, not getValue(), which lags behind a pending post.
        if (value == lastPosted) return;
        lastPosted = value;
        online.postValue(value);
    }

    /** Validated means the system confirmed the network actually reaches the internet. */
    private static boolean hasInternet(NetworkCapabilities caps) {
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
