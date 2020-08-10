package com.zetzaus.quickentry.ui

import android.app.Application
import com.bugfender.sdk.Bugfender
import com.zetzaus.quickentry.BuildConfig
import com.zetzaus.quickentry.R

class LoggedApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.BUILD_TYPE == "release") {
            Bugfender.init(this, getString(R.string.bugfender_api_key), BuildConfig.DEBUG)
            Bugfender.enableCrashReporting()
            Bugfender.enableUIEventLogging(this)
            Bugfender.enableLogcatLogging()
        }
    }
}