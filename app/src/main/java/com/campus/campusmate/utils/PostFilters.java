package com.campus.campusmate.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.models.FeedFilter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Client-side matching for lists held in memory: the admin Bulletin/Events lists, and the
 * offline copy of the feeds. Mirrors the server-side rules in
 * {@code BaseRepository#applyFeedFilter} and the repositories' feed queries, so a filter
 * means the same thing on every screen and online or offline.
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

    /**
     * Applies an Announcements feed filter to {@code all}: scope, search, urgent-only and sort,
     * as {@code AnnouncementRepository#fetchApprovedAnnouncementsPage} does on the server.
     */
    public static List<Announcement> filterAnnouncementFeed(@NonNull List<Announcement> all,
                                                            @NonNull FeedFilter feed) {
        List<Announcement> result = new ArrayList<>();
        for (Announcement a : all) {
            if (!matches(null, a.getVisibility(), a.getDepartment(), null, feed.scope)) continue;
            if (feed.urgentOnly && !a.isUrgent()) continue;
            if (!matchesQuery(feed.query, a.getTitle(), a.getWhat(), a.getWhere(), a.getAuthorName())) continue;
            result.add(a);
        }
        Comparator<Announcement> oldestFirst = Comparator.comparingLong(Announcement::getCreatedAt)
                .thenComparing(Announcement::getId, Comparator.nullsFirst(Comparator.naturalOrder()));
        result.sort(FeedFilter.SORT_OLDEST.equals(feed.sort) ? oldestFirst : oldestFirst.reversed());
        return result;
    }

    /**
     * Applies an Events feed filter to {@code all}: scope, search and timeframe, ordered as
     * {@code EventRepository#fetchApprovedEventsPage} orders them on the server. An unset end
     * date (0) counts as "no end", so such an event is upcoming only if it hasn't started.
     *
     * @param now the current time in epoch millis
     */
    public static List<Event> filterEventFeed(@NonNull List<Event> all, @NonNull FeedFilter feed,
                                              long now) {
        boolean upcoming = FeedFilter.TIME_UPCOMING.equals(feed.timeframe);
        boolean past = FeedFilter.TIME_PAST.equals(feed.timeframe);
        List<Event> result = new ArrayList<>();
        for (Event e : all) {
            if (!matches(null, e.getVisibility(), e.getDepartment(), null, feed.scope)) continue;
            if (!matchesQuery(feed.query, e.getTitle(), e.getWhat(), e.getWhere(), e.getAuthorName())) continue;
            if (upcoming && e.getEndDate() < now && e.getStartDate() < now) continue;
            if (past && (e.getEndDate() >= now || e.getStartDate() >= now)) continue;
            result.add(e);
        }
        Comparator<Event> earliestFirst = Comparator.comparingLong(Event::getStartDate)
                .thenComparing(Event::getId, Comparator.nullsFirst(Comparator.naturalOrder()));
        result.sort(upcoming ? earliestFirst : earliestFirst.reversed());
        return result;
    }
}
