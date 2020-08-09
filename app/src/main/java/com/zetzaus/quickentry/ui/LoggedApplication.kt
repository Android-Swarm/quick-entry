package com.zetzaus.quickentry.ui

import android.app.Application
import com.bugfender.sdk.Bugfender
import com.zetzaus.quickentry.BuildConfig

class LoggedApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.BUILD_TYPE == "release") {
            Bugfender.init(this, "AjGfCRGc5iQh5TmlyL5ZRND6iG2qJ5O3", BuildConfig.DEBUG)
            Bugfender.enableCrashReporting()
            Bugfender.enableUIEventLogging(this)
            Bugfender.enableLogcatLogging()
        }
    }
}