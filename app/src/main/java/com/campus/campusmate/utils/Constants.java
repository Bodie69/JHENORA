package com.campus.campusmate.utils;

/**
 * Global constants used throughout the application.
 * Includes Supabase configuration, database table names, storage paths,
 * intent extras, preference keys, notification settings, and domain-specific enums.
 */
public final class Constants {

    private Constants() {}

    // --- Supabase Configuration ---
    public static final String SUPABASE_URL = "https://fqctgkwgfkdjiamtyevt.supabase.co";
    public static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZxY3Rna3dnZmtkamlhbXR5ZXZ0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODc0MzA5MjYsImV4cCI6MjEwMzAwNjkyNn0.MgOeImTovCxwxOWgjbn5VE9Y3oTWUzDKPCwmp7dn9TI";
    /** The main storage bucket name. */
    public static final String STORAGE_BUCKET = "campusmate";

    // --- Supabase Table Names ---
    public static final String DB_USERS = "users";
    public static final String DB_ANNOUNCEMENTS = "announcements";
    public static final String DB_EVENTS = "events";
    public static final String DB_ORGANIZATIONS = "organizations";
    public static final String DB_DEPARTMENTS = "departments";
    public static final String DB_AUDIT_LOGS = "audit_logs";

    // --- Storage Folder Paths (within bucket) ---
    public static final String STORAGE_ANNOUNCEMENTS = "announcements";
    public static final String STORAGE_EVENTS = "events";
    public static final String STORAGE_PROFILES = "profiles";
    public static final String STORAGE_ORGANIZATIONS = "organizations";
    public static final String STORAGE_DEPARTMENTS = "departments";

    // --- Intent Extras ---
    public static final String EXTRA_ANNOUNCEMENT_ID = "announcement_id";
    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_EDIT_MODE = "edit_mode";
    public static final String EXTRA_IMAGE_URL = "image_url";

    // --- SharedPreferences Keys ---
    public static final String PREF_NAME = "campusmate_prefs";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_ROLE = "user_role";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_USER_DEPT = "user_department";
    public static final String PREF_USER_ORG_ID = "user_organization_id";
    public static final String PREF_REMEMBER_ME = "remember_me";
    public static final String PREF_ACCESS_TOKEN = "access_token";
    public static final String PREF_REFRESH_TOKEN = "refresh_token";
    public static final String PREF_NOTIF_ANNOUNCEMENTS = "notif_announcements";
    public static final String PREF_NOTIF_EVENTS = "notif_events";
    public static final String PREF_NOTIF_APPROVALS = "notif_approvals";

    // --- Notification Channels ---
    public static final String CHANNEL_ANNOUNCEMENTS = "channel_announcements";
    public static final String CHANNEL_EVENTS = "channel_events";
    public static final String CHANNEL_APPROVALS = "channel_approvals";
    public static final String CHANNEL_UPDATES = "channel_updates";
    public static final String CHANNEL_REALTIME = "channel_realtime_sync";

    // --- Notification IDs ---
    public static final int NOTIF_ID_ANNOUNCEMENT = 1001;
    public static final int NOTIF_ID_EVENT = 1002;
    public static final int NOTIF_ID_APPROVAL = 1003;

    // --- Pagination and Limits ---
    /** Page size for infinite-scroll feeds (Announcements, Events). */
    public static final int PAGE_SIZE = 20;
    /** Row count per request when a repository needs "the whole table" (stats, live display,
     *  client-filtered admin lists) — walked in chunks instead of one unbounded request. */
    public static final int BULK_FETCH_CHUNK_SIZE = 200;
    public static final int HOME_PREVIEW_COUNT = 5;

    // --- Image Processing ---
    public static final int IMAGE_QUALITY = 80;
    public static final int MAX_IMAGE_DIMENSION = 1080;

    // --- Departments ---
    public static final String DEPT_BSIT = "BSIT";
    public static final String DEPT_BTLED = "BTLED";
    public static final String DEPT_EDUC = "EDUC";
    public static final String DEPT_GENERAL = "General";

    // --- User Roles ---
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_FACULTY = "faculty";
    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_ORGANIZATION = "organization";
    public static final String ROLE_LIVE_DISPLAY = "live_display";

    // --- Post Status ---
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_ARCHIVED = "archived";

    // --- Post Priority ---
    public static final String PRIORITY_NORMAL = "normal";
    public static final String PRIORITY_HIGH = "high";
    public static final String PRIORITY_URGENT = "urgent";

    // --- Scope/Visibility ---
    public static final String VISIBILITY_GENERAL = "general";
    public static final String VISIBILITY_DEPARTMENT = "department";

    // --- Live Display Settings ---
    // Minimum time each card stays up; also the full dwell time when its description fits
    // without scrolling. A card whose description needs to auto-scroll stays up longer (see
    // LIVE_DISPLAY_SCROLL_*), so the whole thing is readable instead of being cut off.
    public static final long LIVE_DISPLAY_SLIDE_INTERVAL_MS = 8000L;
    public static final long LIVE_DISPLAY_REFRESH_INTERVAL_MS = 60000L;
    // "Time bomb": an announcement has no explicit expiry field, so it drops out of the
    // rotation this long after being posted. Events instead use their own endDate (see
    // Event#isPast()) since that's an explicit, author-set expiry.
    public static final long LIVE_DISPLAY_ANNOUNCEMENT_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000;
    // Auto-scroll pacing for an overflowing description: a slow, easy-to-read speed, plus a
    // pause at the top (let the reader notice the card first) and at the bottom (let them
    // finish reading the last line before the card rotates out).
    public static final float LIVE_DISPLAY_SCROLL_DP_PER_SEC = 24f;
    public static final long LIVE_DISPLAY_SCROLL_HOLD_TOP_MS = 1500L;
    public static final long LIVE_DISPLAY_SCROLL_HOLD_BOTTOM_MS = 2000L;

    // --- Animation Durations (ms) ---
    public static final int ANIM_DURATION_SHORT = 200;
    public static final int ANIM_DURATION_MEDIUM = 350;
    public static final int ANIM_DURATION_LONG = 500;
}
