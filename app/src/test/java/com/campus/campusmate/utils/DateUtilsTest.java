package com.campus.campusmate.utils;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Calendar;

public class DateUtilsTest {

    @Test
    public void testFormatDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.OCTOBER, 25);
        long timestamp = cal.getTimeInMillis();
        
        String formatted = DateUtils.formatDate(timestamp);
        // DateUtils uses DATE_FORMAT: MMM dd, yyyy
        assertTrue(formatted.contains("Oct 25, 2023"));
    }

    @Test
    public void testIsSameDay() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2023, Calendar.OCTOBER, 25, 10, 0);
        
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2023, Calendar.OCTOBER, 25, 15, 30);
        
        Calendar cal3 = Calendar.getInstance();
        cal3.set(2023, Calendar.OCTOBER, 26, 10, 0);

        assertTrue(DateUtils.isSameDay(cal1.getTimeInMillis(), cal2.getTimeInMillis()));
        assertFalse(DateUtils.isSameDay(cal1.getTimeInMillis(), cal3.getTimeInMillis()));
    }

    @Test
    public void testGetStartOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.OCTOBER, 25, 14, 20, 10);
        cal.set(Calendar.MILLISECOND, 500);
        
        long startOfDay = DateUtils.getStartOfDay(cal.getTimeInMillis());
        
        Calendar result = Calendar.getInstance();
        result.setTimeInMillis(startOfDay);
        
        assertEquals(2023, result.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, result.get(Calendar.MONTH));
        assertEquals(25, result.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, result.get(Calendar.MINUTE));
        assertEquals(0, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.MILLISECOND));
    }
}
