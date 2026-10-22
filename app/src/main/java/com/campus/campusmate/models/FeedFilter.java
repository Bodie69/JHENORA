package com.campus.campusmate.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.campus.campusmate.utils.Constants;

import java.util.Objects;

/**
 * Immutable filter, search and sort state for the Announcements and Events feeds. It is
 * applied server-side (see AnnouncementRepository / EventRepository), so matches beyond the
 * first loaded page are found. Each {@code with*} method returns a copy with one field changed.
 */
public final class FeedFilter {

    /** No scope restriction: everything the user's audience filter already allows. */
    public static final String SCOPE_ALL = "all";
    /** General (campus-wide) posts only. */
    public static final String SCOPE_GENERAL = Constants.VISIBILITY_GENERAL;

    public static final String SORT_NEWEST = "newest";
    public static final String SORT_OLDEST = "oldest";

    /** Events that have not ended yet, soonest first. */
    public static final String TIME_UPCOMING = "upcoming";
    /** Events that have already ended, most recent first. */
    public static final String TIME_PAST = "past";
    /** Every event, latest start first. */
    public static final String TIME_ALL = "all";

    /** {@link #SCOPE_ALL}, {@link #SCOPE_GENERAL}, or a department code (department-scoped posts). */
    @NonNull public final String scope;
    /** Trimmed search text; empty for no search. */
    @NonNull public final String query;
    /** Announcements only: {@link #SORT_NEWEST} or {@link #SORT_OLDEST}. */
    @NonNull public final String sort;
    /** Announcements only: restrict to urgent priority. */
    public final boolean urgentOnly;
    /** Events only: {@link #TIME_UPCOMING}, {@link #TIME_PAST} or {@link #TIME_ALL}. */
    @NonNull public final String timeframe;

    private FeedFilter(@NonNull String scope, @NonNull String query, @NonNull String sort,
                       boolean urgentOnly, @NonNull String timeframe) {
        this.scope = scope;
        this.query = query;
        this.sort = sort;
        this.urgentOnly = urgentOnly;
        this.timeframe = timeframe;
    }

    /** All scopes, no search, newest first, upcoming events. */
    public static FeedFilter defaults() {
        return new FeedFilter(SCOPE_ALL, "", SORT_NEWEST, false, TIME_UPCOMING);
    }

    public FeedFilter withScope(@NonNull String scope) {
        return new FeedFilter(scope, query, sort, urgentOnly, timeframe);
    }

    public FeedFilter withQuery(@Nullable String query) {
        return new FeedFilter(scope, query != null ? query.trim() : "", sort, urgentOnly, timeframe);
    }

    public FeedFilter withSort(@NonNull String sort) {
        return new FeedFilter(scope, query, sort, urgentOnly, timeframe);
    }

    public FeedFilter withUrgentOnly(boolean urgentOnly) {
        return new FeedFilter(scope, query, sort, urgentOnly, timeframe);
    }

    public FeedFilter withTimeframe(@NonNull String timeframe) {
        return new FeedFilter(scope, query, sort, urgentOnly, timeframe);
    }

    /** True when {@link #scope} names a department rather than All/General. */
    public boolean isDepartmentScope() {
        return !SCOPE_ALL.equals(scope) && !SCOPE_GENERAL.equals(scope);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FeedFilter)) return false;
        FeedFilter f = (FeedFilter) o;
        return urgentOnly == f.urgentOnly && scope.equals(f.scope) && query.equals(f.query)
                && sort.equals(f.sort) && timeframe.equals(f.timeframe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope, query, sort, urgentOnly, timeframe);
    }
}
