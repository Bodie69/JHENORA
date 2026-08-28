package com.campus.campusmate.models;

import org.junit.Test;
import static org.junit.Assert.*;

public class AnnouncementTest {

    @Test
    public void testAnnouncementStatus() {
        Announcement a = new Announcement();
        a.setStatus(Announcement.STATUS_PENDING);
        assertTrue(a.isPending());
        assertFalse(a.isApproved());

        a.setStatus(Announcement.STATUS_APPROVED);
        assertTrue(a.isApproved());
        assertFalse(a.isPending());
    }

    @Test
    public void testAnnouncementPriority() {
        Announcement a = new Announcement();
        a.setPriority(Announcement.PRIORITY_URGENT);
        assertTrue(a.isUrgent());

        a.setPriority(Announcement.PRIORITY_HIGH);
        assertTrue(a.isHighPriority());
        assertFalse(a.isUrgent());
    }

    @Test
    public void testAnnouncementVisibility() {
        Announcement a = new Announcement();
        a.setVisibility(Announcement.VISIBILITY_GENERAL);
        assertTrue(a.isGeneralVisibility());

        a.setVisibility(Announcement.VISIBILITY_DEPARTMENT);
        assertFalse(a.isGeneralVisibility());
    }
}
