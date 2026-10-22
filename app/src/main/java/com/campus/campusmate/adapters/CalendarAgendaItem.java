package com.campus.campusmate.adapters;

import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.utils.DateUtils;

/**
 * A single row in the Calendar agenda — wraps either an {@link Announcement} or an
 * {@link Event} behind one shape so the day list and date-strip counts can treat
 * both content types uniformly.
 */
public class CalendarAgendaItem {

    public enum Type { ANNOUNCEMENT, EVENT }

    public final Type type;
    public final String id;
    public final String title;
    public final String subtitle;
    public final String authorName;
    /** Start-of-day bucket used to group this item under a date-strip day. */
    public final long dayTimestamp;
    /** Exact moment used to order items within a day (event start time / post time). */
    public final long sortTimestamp;

    private CalendarAgendaItem(Type type, String id, String title, String subtitle,
                                String authorName, long dayTimestamp, long sortTimestamp) {
        this.type = type;
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.authorName = authorName;
        this.dayTimestamp = dayTimestamp;
        this.sortTimestamp = sortTimestamp;
    }

    public static CalendarAgendaItem fromEvent(Event e) {
        String start = DateUtils.formatTime(e.getStartDate());
        String end = DateUtils.formatTime(e.getEndDate());
        String subtitle;
        if (!start.isEmpty() && !end.isEmpty()) {
            subtitle = start + " – " + end;
        } else {
            subtitle = start;
        }
        return new CalendarAgendaItem(Type.EVENT, e.getId(), e.getTitle(), subtitle,
                e.getAuthorName(), DateUtils.getStartOfDay(e.getStartDate()), e.getStartDate());
    }

    public static CalendarAgendaItem fromAnnouncement(Announcement a) {
        // Bucket/sort by the announcement's own real event_date when it has one (set via
        // CreateAnnouncementActivity's date/time picker), same as an event's start date —
        // otherwise fall back to when it was posted, for rows from before event_date existed.
        boolean hasEventDate = a.getEventDate() > 0;
        long when = hasEventDate ? a.getEventDate() : a.getCreatedAt();
        String subtitle = hasEventDate
                ? DateUtils.formatTime(a.getEventDate())
                : (a.getWhen() != null && !a.getWhen().trim().isEmpty())
                        ? a.getWhen()
                        : "Posted " + DateUtils.formatTime(a.getCreatedAt());
        return new CalendarAgendaItem(Type.ANNOUNCEMENT, a.getId(), a.getTitle(), subtitle,
                a.getAuthorName(), DateUtils.getStartOfDay(when), when);
    }
}
