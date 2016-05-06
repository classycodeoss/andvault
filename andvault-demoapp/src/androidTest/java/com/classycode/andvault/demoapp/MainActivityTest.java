package com.classycode.andvault.demoapp;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.RequiresDevice;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.classycode.andvault.Vault;
import com.classycode.andvault.VaultException;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2016
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
@RequiresDevice
// actually, requires a lock screen. restrict to real device as we currently can't set this up on the CI server
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void setUp() throws VaultException {
        Vault vault = new Vault(activityRule.getActivity().getApplication());
        vault.reset(activityRule.getActivity().getApplication());
    }

    @Test
    public void testAddCredential() throws VaultException {
        Espresso.onView(ViewMatchers.withText("testName")).check(ViewAssertions.doesNotExist());
        Espresso.onView(ViewMatchers.withId(R.id.add_credential_item)).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withId(R.id.credential_name_field)).perform(ViewActions.typeText("testName"));
        Espresso.onView(ViewMatchers.withId(R.id.credential_value_field)).perform(ViewActions.typeText("secret"));
        Espresso.onView(ViewMatchers.withText(R.string.add)).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withText("testName")).perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withText("<Value hidden>")).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withText("secret")).perform();
    }
}
