package com.campus.campusmate;

import android.app.Application;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.campus.campusmate.notifications.NotificationHelper;
import com.campus.campusmate.repositories.RepositoryProvider;
import com.campus.campusmate.services.RealtimeService;
import com.campus.campusmate.services.SyncWorker;
import com.campus.campusmate.utils.SessionManager;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class JhenoraApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        RepositoryProvider.init(this);
        // Department logos shown on posts/profiles; the cached copy covers startup and offline.
        RepositoryProvider.getInstance().getDepartmentRepository().refreshLogoCache();
        NotificationHelper.createChannels(this);
        scheduleSync();

        // Covers the process being restarted (app reopened, OS respawn) while the user is
        // still logged in — first login and logout are handled directly in AuthViewModel.
        if (new SessionManager(this).isLoggedIn()) {
            RealtimeService.start(this);
        }
    }

    private void scheduleSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "jhenora_sync", ExistingPeriodicWorkPolicy.KEEP, request);
    }
}
