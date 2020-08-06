package com.zetzaus.quickentry.ui

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.zetzaus.quickentry.R
import org.hamcrest.CoreMatchers.`is`
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val activityRule = ActivityTestRule<MainActivity>(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(*MainActivity.PERMISSIONS)

    @Test
    fun floatingActionButtonNavigatesToScanFragment() {
        val fakeNav = TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
            setGraph(R.navigation.main_navigation)
        }

        val mainScenario = launchFragmentInContainer<MainFragment>(themeResId = R.style.AppTheme)

        mainScenario.onFragment {
            Navigation.setViewNavController(it.requireView(), fakeNav)
        }

        Espresso.onView(withId(R.id.scanFloatingActionButton))
            .perform(click())

        assertThat(fakeNav.currentDestination?.id, `is`(R.id.scanFragment))
    }
}