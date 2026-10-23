package com.factory.expenses;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.factory.expenses.ui.MainActivity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Main navigation flow; needs no backend because every screen has an error state. */
@RunWith(AndroidJUnit4.class)
public class MainFlowTest {

  @Rule
  public ActivityScenarioRule<MainActivity> activityRule =
      new ActivityScenarioRule<>(MainActivity.class);

  @Test
  public void listScreenIsShownFirstAndFabOpensEditor() {
    onView(withId(R.id.list_root)).check(matches(isDisplayed()));

    onView(withId(R.id.fab_add)).perform(click());

    onView(withId(R.id.edit_root)).check(matches(isDisplayed()));
  }

  @Test
  public void bottomNavigationReachesSummaryAndSettings() {
    onView(withId(R.id.summaryFragment)).perform(click());
    onView(withId(R.id.summary_root)).check(matches(isDisplayed()));

    onView(withId(R.id.settingsFragment)).perform(click());
    onView(withId(R.id.settings_root)).check(matches(isDisplayed()));
    onView(withId(R.id.url_input)).check(matches(isDisplayed()));
  }
}
