package com.campus.campusmate.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.campus.campusmate.database.OfflineCache;
import com.campus.campusmate.models.User;

import java.io.IOException;
import java.security.GeneralSecurityException;

import timber.log.Timber;

/**
 * Manager class for handling user sessions and authentication tokens.
 * It uses EncryptedSharedPreferences to persist sensitive user information locally.
 */
public class SessionManager {

    private final Context appContext;
    private SharedPreferences prefs;

    /**
     * Initializes the SessionManager with a Context using EncryptedSharedPreferences.
     *
     * @param context The application context.
     */
    public SessionManager(Context context) {
        appContext = context.getApplicationContext();
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    context,
                    Constants.PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Timber.e(e, "Error initializing EncryptedSharedPreferences");
            // Fallback to regular SharedPreferences if encryption fails (though not ideal for security)
            prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    /**
     * Saves user details (ID, role, email, name, department) to SharedPreferences.
     *
     * @param user The user object containing session data.
     */
    public void saveSession(User user) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.PREF_USER_ID, user.getUid());
        editor.putString(Constants.PREF_USER_ROLE, user.getRole());
        editor.putString(Constants.PREF_USER_EMAIL, user.getEmail());
        editor.putString(Constants.PREF_USER_NAME, user.getFullName());
        editor.putString(Constants.PREF_USER_DEPT, user.getDepartment());
        editor.putString(Constants.PREF_USER_ORG_ID, user.getOrganizationId());
        editor.apply();
    }

    /**
     * Saves user details along with authentication tokens.
     *
     * @param user         The user object.
     * @param accessToken  The Supabase access token (JWT).
     * @param refreshToken The Supabase refresh token.
     */
    public void saveSession(User user, String accessToken, String refreshToken) {
        saveSession(user);
        saveTokens(accessToken, refreshToken);
    }

    /**
     * Saves the "Remember Me" preference and the associated email.
     *
     * @param remember True to remember the user, false otherwise.
     * @param email    The email to remember.
     */
    public void saveRememberMe(boolean remember, String email) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.PREF_REMEMBER_ME, remember);
        if (remember) {
            editor.putString(Constants.PREF_USER_EMAIL, email);
        }
        editor.apply();
    }

    /**
     * Checks if the "Remember Me" option is enabled.
     *
     * @return True if "Remember Me" is enabled, false otherwise.
     */
    public boolean isRememberMe() {
        return prefs.getBoolean(Constants.PREF_REMEMBER_ME, false);
    }

    /**
     * Retrieves the saved email from the "Remember Me" preference.
     *
     * @return The saved email, or an empty string if not found.
     */
    public String getSavedEmail() {
        return prefs.getString(Constants.PREF_USER_EMAIL, "");
    }

    /**
     * Clears the current user session data from SharedPreferences, and the offline copy of
     * posts, which holds the signed-out user's department posts.
     * If "Remember Me" is enabled, it preserves the email and preference.
     */
    public void clearSession() {
        String email = getSavedEmail();
        boolean rememberMe = isRememberMe();
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        if (rememberMe) {
            editor.putBoolean(Constants.PREF_REMEMBER_ME, true);
            editor.putString(Constants.PREF_USER_EMAIL, email);
        }
        editor.apply();
        OfflineCache.getInstance(appContext).clear();
    }

    /**
     * Saves the access and refresh tokens.
     *
     * @param accessToken  The access token string.
     * @param refreshToken The refresh token string.
     */
    public void saveTokens(String accessToken, String refreshToken) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.PREF_ACCESS_TOKEN, accessToken);
        editor.putString(Constants.PREF_REFRESH_TOKEN, refreshToken);
        editor.apply();
    }

    /**
     * Retrieves the saved access token.
     *
     * @return The access token, or null if not found.
     */
    public String getAccessToken() {
        return prefs.getString(Constants.PREF_ACCESS_TOKEN, null);
    }

    /**
     * Retrieves the saved refresh token.
     *
     * @return The refresh token, or null if not found.
     */
    public String getRefreshToken() {
        return prefs.getString(Constants.PREF_REFRESH_TOKEN, null);
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @return True if logged in, false otherwise.
     */
    public boolean isLoggedIn() {
        return prefs.getString(Constants.PREF_USER_ID, null) != null;
    }

    /**
     * Retrieves the current user's unique ID.
     *
     * @return The user ID, or null if not logged in.
     */
    public String getUserId() {
        return prefs.getString(Constants.PREF_USER_ID, null);
    }

    /**
     * Retrieves the current user's role.
     *
     * @return The user role (e.g., student, faculty, admin).
     */
    public String getUserRole() {
        return prefs.getString(Constants.PREF_USER_ROLE, null);
    }

    /**
     * Retrieves the current user's email.
     *
     * @return The user email.
     */
    public String getUserEmail() {
        return prefs.getString(Constants.PREF_USER_EMAIL, null);
    }

    /**
     * Retrieves the current user's full name.
     *
     * @return The user's full name.
     */
    public String getUserName() {
        return prefs.getString(Constants.PREF_USER_NAME, null);
    }

    /**
     * Retrieves the current user's department.
     *
     * @return The user's department (e.g., BSIT, BTLED).
     */
    public String getUserDepartment() {
        return prefs.getString(Constants.PREF_USER_DEPT, null);
    }

    /**
     * Retrieves the ID of the organization the current user leads, if any.
     *
     * @return The organization ID, or null if the user doesn't lead one.
     */
    public String getUserOrganizationId() {
        return prefs.getString(Constants.PREF_USER_ORG_ID, null);
    }

    /**
     * Checks if the current user has the ADMIN role.
     *
     * @return True if admin, false otherwise.
     */
    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equals(getUserRole());
    }

    /**
     * Checks if the current user has the STUDENT role.
     *
     * @return True if student, false otherwise.
     */
    public boolean isStudent() {
        return Constants.ROLE_STUDENT.equals(getUserRole());
    }

    /**
     * Checks if the current user has the FACULTY role.
     *
     * @return True if faculty, false otherwise.
     */
    public boolean isFaculty() {
        return Constants.ROLE_FACULTY.equals(getUserRole());
    }

    /**
     * Checks if the current user has the ORGANIZATION role.
     *
     * @return True if organization leader, false otherwise.
     */
    public boolean isOrganization() {
        return Constants.ROLE_ORGANIZATION.equals(getUserRole());
    }

    /**
     * Checks if the current user is a LIVE DISPLAY account.
     *
     * @return True if live display, false otherwise.
     */
    public boolean isLiveDisplay() {
        return Constants.ROLE_LIVE_DISPLAY.equals(getUserRole());
    }

    /**
     * Checks if the current user role is authorized to create posts.
     * Authorized roles: FACULTY, ORGANIZATION, ADMIN.
     *
     * @return True if the user can post, false otherwise.
     */
    public boolean canPost() {
        String role = getUserRole();
        return Constants.ROLE_FACULTY.equals(role)
                || Constants.ROLE_ORGANIZATION.equals(role)
                || Constants.ROLE_ADMIN.equals(role);
    }

    /**
     * Enables or disables notifications for a specific channel.
     *
     * @param channel The notification channel ID.
     * @param enabled True to enable, false to disable.
     * @return True if the change was successfully committed.
     */
    public boolean setNotificationEnabled(String channel, boolean enabled) {
        return prefs.edit().putBoolean(channel, enabled).commit();
    }

    /**
     * Checks if notifications are enabled for a specific channel.
     * Defaults to true if no preference is saved.
     *
     * @param channel The notification channel ID.
     * @return True if enabled, false otherwise.
     */
    public boolean isNotificationEnabled(String channel) {
        return prefs.getBoolean(channel, true);
    }
}
