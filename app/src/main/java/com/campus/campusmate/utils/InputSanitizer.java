package com.campus.campusmate.utils;

/**
 * Utility class for sanitizing user input to prevent XSS and other injection attacks.
 * It provides methods for trimming whitespace and removing potentially harmful characters/tags.
 */
public final class InputSanitizer {

    private InputSanitizer() {}

    /**
     * Strip leading/trailing whitespace and remove HTML-like tags and potentially harmful characters.
     *
     * @param input The raw input string.
     * @return The sanitized string.
     */
    public static String sanitize(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("<[^>]*>", "").replaceAll("[<>\"']", "");
    }

    /**
     * Sanitize but preserve newlines for multi-line fields.
     * Removes HTML-like tags and potentially harmful characters.
     *
     * @param input The raw multi-line input string.
     * @return The sanitized string.
     */
    public static String sanitizeMultiline(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("<[^>]*>", "").replaceAll("[<>\"']", "");
    }

    /**
     * Sanitize a name field - allows only letters, spaces, hyphens, and apostrophes.
     *
     * @param input The raw name input string.
     * @return The sanitized name string.
     */
    public static String sanitizeName(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("[^a-zA-Z\\s\\-']", "");
    }

    /**
     * Sanitize an email by lowercasing and trimming leading/trailing whitespace.
     *
     * @param input The raw email input string.
     * @return The sanitized email string.
     */
    public static String sanitizeEmail(String input) {
        if (input == null) return "";
        return input.trim().toLowerCase();
    }

    /**
     * Sanitize a URL by trimming leading/trailing whitespace.
     *
     * @param input The raw URL input string.
     * @return The sanitized URL string.
     */
    public static String sanitizeUrl(String input) {
        if (input == null) return "";
        return input.trim();
    }

    /**
     * Limit text to a maximum number of characters.
     *
     * @param input    The input string.
     * @param maxChars The maximum number of characters allowed.
     * @return The truncated string if it exceeds maxChars, otherwise the original string.
     */
    public static String limit(String input, int maxChars) {
        if (input == null) return "";
        return input.length() > maxChars ? input.substring(0, maxChars) : input;
    }
}
