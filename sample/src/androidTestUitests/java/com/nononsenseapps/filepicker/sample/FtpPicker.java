package com.nononsenseapps.filepicker.sample;


import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.nononsenseapps.filepicker.sample.PermissionGranter.allowPermissionsIfNeeded;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class FtpPicker {

    @Rule
    public ActivityTestRule<NoNonsenseFilePickerTest> mActivityTestRule =
            new ActivityTestRule<>(NoNonsenseFilePickerTest.class);

    @Before
    public void allowPermissions() {
        allowPermissionsIfNeeded(mActivityTestRule.getActivity());
    }

    @Test
    public void selectDir() throws IOException {
        ViewInteraction radioButton = onView(
                allOf(withId(R.id.radioDir), withText("Select directory"),
                        withParent(withId(R.id.radioGroup)),
                        isDisplayed()));
        radioButton.perform(click());

        onView(withId(R.id.button_ftp)).perform(ViewActions.scrollTo());

        ViewInteraction button = onView(
                allOf(withId(R.id.button_ftp), isDisplayed()));
        button.perform(click());

        ViewInteraction recyclerView = onView(
                allOf(withId(android.R.id.list), isDisplayed()));

        // press pub
        recyclerView.perform(actionOnItemAtPosition(1, click()));

        ViewInteraction okButton = onView(
                allOf(withId(R.id.nnf_button_ok),
                        withParent(allOf(withId(R.id.nnf_button_container),
                                withParent(withId(R.id.nnf_buttons_container)))),
                        isDisplayed()));
        // Click ok
        okButton.perform(click());

        ViewInteraction textView = onView(withId(R.id.text));
        textView.check(matches(withText("ftp://anonymous:anonymous@debian.simnet.is:21/pub")));
    }
}
