package com.campus.campusmate.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.campus.campusmate.services.RealtimeService;
import com.campus.campusmate.utils.SessionManager;

/** Restarts {@link RealtimeService} after a reboot so approval notifications stay instant
 *  without requiring the user to reopen the app first. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        if (new SessionManager(context).isLoggedIn()) {
            RealtimeService.start(context);
        }
    }
}
