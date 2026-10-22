package com.campus.campusmate.utils;

import static org.junit.Assert.assertEquals;

import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.models.FeedFilter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The offline feed filters must match what the server-side feed queries return. */
public class PostFiltersFeedTest {

    private static final long NOW = 1_000_000L;

    private static Announcement announcement(String id, long createdAt, String visibility,
                                             String department, String priority, String title) {
        Announcement a = new Announcement();
        a.setId(id);
        a.setCreatedAt(createdAt);
        a.setVisibility(visibility);
        a.setDepartment(department);
        a.setPriority(priority);
        a.setTitle(title);
        return a;
    }

    private static Event event(String id, long start, long end, String visibility, String department) {
        Event e = new Event();
        e.setId(id);
        e.setTitle("Event " + id);
        e.setStartDate(start);
        e.setEndDate(end);
        e.setVisibility(visibility);
        e.setDepartment(department);
        return e;
    }

    private static List<String> announcementIds(List<Announcement> list) {
        List<String> ids = new ArrayList<>();
        for (Announcement a : list) ids.add(a.getId());
        return ids;
    }

    private static List<String> eventIds(List<Event> list) {
        List<String> ids = new ArrayList<>();
        for (Event e : list) ids.add(e.getId());
        return ids;
    }

    private final List<Announcement> announcements = Arrays.asList(
            announcement("a1", 100, "general", "BSIT", "normal", "Enrollment schedule"),
            announcement("a2", 300, "department", "BSIT", "urgent", "Lab closed"),
            announcement("a3", 200, "general", "EDUC", "urgent", "Typhoon class suspension"));

    @Test
    public void announcementsDefaultToNewestFirst() {
        assertEquals(Arrays.asList("a2", "a3", "a1"),
                announcementIds(PostFilters.filterAnnouncementFeed(announcements, FeedFilter.defaults())));
    }

    @Test
    public void announcementsCanSortOldestFirst() {
        FeedFilter oldest = FeedFilter.defaults().withSort(FeedFilter.SORT_OLDEST);
        assertEquals(Arrays.asList("a1", "a3", "a2"),
                announcementIds(PostFilters.filterAnnouncementFeed(announcements, oldest)));
    }

    @Test
    public void departmentScopeExcludesGeneralPostsFromThatDepartment() {
        FeedFilter bsit = FeedFilter.defaults().withScope("BSIT");
        assertEquals(Arrays.asList("a2"),
                announcementIds(PostFilters.filterAnnouncementFeed(announcements, bsit)));
    }

    @Test
    public void generalScopeUrgentOnlyAndSearchCombine() {
        FeedFilter filter = FeedFilter.defaults().withScope(FeedFilter.SCOPE_GENERAL)
                .withUrgentOnly(true).withQuery("TYPHOON");
        assertEquals(Arrays.asList("a3"),
                announcementIds(PostFilters.filterAnnouncementFeed(announcements, filter)));
    }

    private final List<Event> events = Arrays.asList(
            event("past", NOW - 500, NOW - 100, "general", "BSIT"),
            event("ongoing", NOW - 50, NOW + 50, "general", "BSIT"),
            event("soon", NOW + 10, 0, "general", "BSIT"),
            event("later", NOW + 900, NOW + 1000, "department", "EDUC"),
            event("startedNoEnd", NOW - 10, 0, "general", "BSIT"));

    @Test
    public void upcomingIsNotEndedYetSoonestFirst() {
        FeedFilter upcoming = FeedFilter.defaults().withTimeframe(FeedFilter.TIME_UPCOMING);
        assertEquals(Arrays.asList("ongoing", "soon", "later"),
                eventIds(PostFilters.filterEventFeed(events, upcoming, NOW)));
    }

    @Test
    public void pastIncludesStartedEventsWithNoEndLatestFirst() {
        FeedFilter past = FeedFilter.defaults().withTimeframe(FeedFilter.TIME_PAST);
        assertEquals(Arrays.asList("startedNoEnd", "past"),
                eventIds(PostFilters.filterEventFeed(events, past, NOW)));
    }

    @Test
    public void allTimeframesLatestStartFirst() {
        FeedFilter all = FeedFilter.defaults().withTimeframe(FeedFilter.TIME_ALL);
        assertEquals(Arrays.asList("later", "soon", "startedNoEnd", "ongoing", "past"),
                eventIds(PostFilters.filterEventFeed(events, all, NOW)));
    }
}
