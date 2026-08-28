package com.campus.campusmate.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for date and time formatting and manipulation.
 * Uses SimpleDateFormat for various display formats and provides relative time logic.
 */
public final class DateUtils {

    private DateUtils() {}

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private static final SimpleDateFormat DATE_TIME_FORMAT =
            new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private static final SimpleDateFormat MONTH_YEAR_FORMAT =
            new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    /**
     * Formats a timestamp into a standard date string (e.g., Jan 01, 2023).
     *
     * @param timestamp The timestamp in milliseconds.
     * @return The formatted date string, or an empty string if timestamp is 0.
     */
    public static String formatDate(long timestamp) {
        if (timestamp == 0) return "";
        return DATE_FORMAT.format(new Date(timestamp));
    }

    /**
     * Formats a timestamp into a date and time string (e.g., Jan 01, 2023 • 10:00 AM).
     *
     * @param timestamp The timestamp in milliseconds.
     * @return The formatted date-time string, or an empty string if timestamp is 0.
     */
    public static String formatDateTime(long timestamp) {
        if (timestamp == 0) return "";
        return DATE_TIME_FORMAT.format(new Date(timestamp));
    }

    /**
     * Formats a timestamp into a time string (e.g., 10:00 AM).
     *
     * @param timestamp The timestamp in milliseconds.
     * @return The formatted time string, or an empty string if timestamp is 0.
     */
    public static String formatTime(long timestamp) {
        if (timestamp == 0) return "";
        return TIME_FORMAT.format(new Date(timestamp));
    }

    /**
     * Formats a timestamp into a month and year string (e.g., January 2023).
     *
     * @param timestamp The timestamp in milliseconds.
     * @return The formatted month-year string, or an empty string if timestamp is 0.
     */
    public static String formatMonthYear(long timestamp) {
        if (timestamp == 0) return "";
        return MONTH_YEAR_FORMAT.format(new Date(timestamp));
    }

    /**
     * Returns a relative time string (e.g., "Just now", "5 minutes ago", "2 days ago").
     * If the difference is more than 7 days, it returns the formatted date.
     *
     * @param timestamp The timestamp in milliseconds.
     * @return The relative time string.
     */
    public static String getRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Just now";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long mins = TimeUnit.MILLISECONDS.toMinutes(diff);
            return mins + (mins == 1 ? " minute ago" : " minutes ago");
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (diff < TimeUnit.DAYS.toMillis(7)) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + (days == 1 ? " day ago" : " days ago");
        } else {
            return formatDate(timestamp);
        }
    }

    /**
     * Extracts the day of the month from a timestamp.
     *
     * @param timestamp The timestamp in milliseconds.
     * @return The day of the month as a string.
     */
    public static String getDayOfMonth(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Extracts the short month name (e.g., Jan) from a timestamp.
     *
     * @param timestamp The timestamp in milliseconds.
     * @return The short month name.
     */
    public static String getShortMonthName(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return new SimpleDateFormat("MMM", Locale.getDefault()).format(cal.getTime());
    }

    /**
     * Checks if two timestamps represent the same calendar day.
     *
     * @param timestamp1 The first timestamp.
     * @param timestamp2 The second timestamp.
     * @return True if they are on the same day, false otherwise.
     */
    public static boolean isSameDay(long timestamp1, long timestamp2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(timestamp1);
        Calendar c2 = Calendar.getInstance();
        c2.setTimeInMillis(timestamp2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Returns the timestamp for the start of the day (00:00:00.000) for a given timestamp.
     *
     * @param timestamp The original timestamp.
     * @return The start-of-day timestamp.
     */
    public static long getStartOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Returns the timestamp for the end of the day (23:59:59.999) for a given timestamp.
     *
     * @param timestamp The original timestamp.
     * @return The end-of-day timestamp.
     */
    public static long getEndOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }
}
