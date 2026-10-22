package com.campus.campusmate.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class EventTest {

    @Test
    public void contentIsDeliveredOnlyOnce() {
        Event<String> event = new Event<>("Logo updated for BSIT");
        assertEquals("Logo updated for BSIT", event.getContentIfNotHandled());
        assertNull("replay to a re-attached observer is ignored", event.getContentIfNotHandled());
    }

    @Test
    public void peekAlwaysReturnsContent() {
        Event<String> event = new Event<>("x");
        event.getContentIfNotHandled();
        assertEquals("x", event.peekContent());
    }
}
