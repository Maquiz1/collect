package org.odk.collect.android.support.pages

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.odk.collect.android.R

class FormsDownloadResultPage : Page<FormsDownloadResultPage>() {

    override fun assertOnPage(): FormsDownloadResultPage {
        onView(withText(org.odk.collect.strings.R.string.ok)).inRoot(RootMatchers.isDialog()).check(matches(isDisplayed()))
        return this
    }

    fun assertMessage(message: String): FormsDownloadResultPage {
        assertText(message)
        return this
    }

    fun showDetails(): ErrorPage {
        onView(withText(getTranslatedString(org.odk.collect.strings.R.string.show_details))).perform(click())
        return ErrorPage().assertOnPage()
    }
}
