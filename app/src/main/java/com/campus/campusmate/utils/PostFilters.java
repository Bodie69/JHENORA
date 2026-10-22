package com.campus.campusmate.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.campus.campusmate.models.FeedFilter;

import java.util.Locale;

/**
 * Client-side matching for the admin Bulletin/Events lists, which hold every post in memory.
 * Mirrors the server-side scope rules in {@code BaseRepository#applyFeedFilter} so a scope
 * chip means the same thing on every screen.
 */
public final class PostFilters {

    private PostFilters() {}

    /**
     * @param statusFilter required status, or {@code null} for any
     * @param scope        {@link FeedFilter#SCOPE_ALL}, {@link FeedFilter#SCOPE_GENERAL} or a
     *                     department code; a department matches department-scoped posts only,
     *                     since General posts also carry their author's department
     */
    public static boolean matches(@Nullable String status, @Nullable String visibility,
                                  @Nullable String department, @Nullable String statusFilter,
                                  @NonNull String scope) {
        if (statusFilter != null && !statusFilter.equals(status)) return false;
        boolean general = Constants.VISIBILITY_GENERAL.equals(visibility);
        if (FeedFilter.SCOPE_ALL.equals(scope)) return true;
        if (FeedFilter.SCOPE_GENERAL.equals(scope)) return general;
        return !general && scope.equalsIgnoreCase(department);
    }

    /** True when {@code query} is blank or appears (case-insensitively) in any of {@code fields}. */
    public static boolean matchesQuery(@Nullable String query, String... fields) {
        String q = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        if (q.isEmpty()) return true;
        for (String field : fields) {
            if (field != null && field.toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }
}
