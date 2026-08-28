package com.campus.campusmate.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.campus.campusmate.R;

/**
 * Helper class for managing Android system notifications.
 * It handles the creation of notification channels (for API 26+) and provides
 * simplified methods for displaying announcements, events, and approval notifications.
 */
public class NotificationHelper {

    private final Context context;
    private final NotificationManager manager;

    /**
     * Initializes the NotificationHelper and sets up notification channels.
     *
     * @param context The application context.
     */
    public NotificationHelper(Context context) {
        this.context = context;
        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannels();
    }

    /**
     * Creates the required notification channels for the application.
     * Channels include Announcements, Events, Approvals, and General Updates.
     */
    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel announcements = new NotificationChannel(
                    Constants.CHANNEL_ANNOUNCEMENTS,
                    context.getString(R.string.notif_channel_announcements),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            announcements.setDescription("Campus announcements and bulletin posts");

            NotificationChannel events = new NotificationChannel(
                    Constants.CHANNEL_EVENTS,
                    context.getString(R.string.notif_channel_events),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            events.setDescription("Campus events and activities");

            NotificationChannel approvals = new NotificationChannel(
                    Constants.CHANNEL_APPROVALS,
                    context.getString(R.string.notif_channel_approvals),
                    NotificationManager.IMPORTANCE_HIGH
            );
            approvals.setDescription("Post approval results");

            NotificationChannel updates = new NotificationChannel(
                    Constants.CHANNEL_UPDATES,
                    "Updates",
                    NotificationManager.IMPORTANCE_LOW
            );

            manager.createNotificationChannel(announcements);
            manager.createNotificationChannel(events);
            manager.createNotificationChannel(approvals);
            manager.createNotificationChannel(updates);
        }
    }

    /**
     * Displays a notification in the Announcements channel.
     *
     * @param id      The unique notification ID.
     * @param title   The notification title.
     * @param message The notification message body.
     */
    public void showAnnouncement(int id, String title, String message) {
        show(id, title, message, Constants.CHANNEL_ANNOUNCEMENTS);
    }

    /**
     * Displays a notification in the Events channel.
     *
     * @param id      The unique notification ID.
     * @param title   The notification title.
     * @param message The notification message body.
     */
    public void showEvent(int id, String title, String message) {
        show(id, title, message, Constants.CHANNEL_EVENTS);
    }

    /**
     * Displays a notification in the Approvals channel.
     *
     * @param id      The unique notification ID.
     * @param title   The notification title.
     * @param message The notification message body.
     */
    public void showApproval(int id, String title, String message) {
        show(id, title, message, Constants.CHANNEL_APPROVALS);
    }

    /**
     * Internal helper method to build and show a notification.
     *
     * @param id        The notification ID.
     * @param title     The notification title.
     * @param message   The notification message.
     * @param channelId The target notification channel ID.
     */
    private void show(int id, String title, String message, String channelId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        manager.notify(id, builder.build());
    }

    /**
     * Cancels all currently active notifications from this application.
     */
    public void cancelAll() {
        manager.cancelAll();
    }
}
