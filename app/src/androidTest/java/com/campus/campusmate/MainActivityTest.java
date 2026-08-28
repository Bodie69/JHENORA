package com.campus.campusmate;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;

import com.campus.campusmate.activities.MainActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testNavigationToCalendar() {
        // Assuming BottomNavigationView has an item with id nav_calendar
        Espresso.onView(ViewMatchers.withId(R.id.nav_calendar))
                .perform(ViewActions.click());

        // Check if the calendar view is displayed
        Espresso.onView(ViewMatchers.withId(R.id.calendar_view))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testNavigationToAnnouncements() {
        Espresso.onView(ViewMatchers.withId(R.id.nav_announcements))
                .perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withId(R.id.rv_announcements))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }
}
