package com.campus.campusmate;

import android.app.Application;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.campus.campusmate.notifications.NotificationHelper;
import com.campus.campusmate.repositories.RepositoryProvider;
import com.campus.campusmate.services.SyncWorker;

import java.util.concurrent.TimeUnit;

public class JhenoraApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        RepositoryProvider.init(this);
        NotificationHelper.createChannels(this);
        scheduleSync();
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
