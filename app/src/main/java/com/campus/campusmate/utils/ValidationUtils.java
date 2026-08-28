package com.campus.campusmate.utils;

import android.util.Patterns;

/**
 * Utility class for validating user input formats.
 * Provides checks for emails, passwords, URLs, and general non-empty strings.
 */
public final class ValidationUtils {

    private ValidationUtils() {}

    /**
     * Checks if a string is a valid email address using Android's email pattern.
     *
     * @param email The email string to validate.
     * @return True if valid, false otherwise.
     */
    public static boolean isValidEmail(String email) {
        return email != null
                && !email.trim().isEmpty()
                && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    /**
     * Validates a password based on a minimum length (8 characters).
     *
     * @param password The password string to validate.
     * @return True if valid, false otherwise.
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    /**
     * Checks if two password strings are identical.
     *
     * @param password The first password.
     * @param confirm  The second (confirmation) password.
     * @return True if they match, false otherwise.
     */
    public static boolean passwordsMatch(String password, String confirm) {
        return password != null && password.equals(confirm);
    }

    /**
     * Checks if a string is not null and not empty after trimming.
     *
     * @param value The string to check.
     * @return True if not empty, false otherwise.
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Validates a URL string. Returns true if the URL is empty or matches a web URL pattern.
     *
     * @param url The URL string to validate.
     * @return True if valid or empty, false otherwise.
     */
    public static boolean isValidUrl(String url) {
        return url == null
                || url.trim().isEmpty()
                || Patterns.WEB_URL.matcher(url.trim()).matches();
    }

    /**
     * Validates a name string (minimum length of 2 characters).
     *
     * @param name The name string to validate.
     * @return True if valid, false otherwise.
     */
    public static boolean isValidName(String name) {
        return name != null && name.trim().length() >= 2;
    }

    /**
     * Sanitizes a string for display by trimming leading and trailing whitespace.
     *
     * @param input The input string.
     * @return The trimmed string, or an empty string if input was null.
     */
    public static String sanitizeForDisplay(String input) {
        if (input == null) return "";
        return input.trim();
    }
}
