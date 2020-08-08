package com.zetzaus.quickentry.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ActivityNavigator
import com.zetzaus.quickentry.R
import com.zetzaus.quickentry.extensions.TAG

class WebActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        WebActivityArgs.fromBundle(intent.extras!!).apply {
            if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
                supportFragmentManager.beginTransaction()
                    .add(
                        R.id.fragmentContainer,
                        WebFragment.create(url, shouldPersist, snapLocation)
                    )
                    .commit()
            }
        }

        // Enable the Up button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onBackPressed() {
        supportFragmentManager.findFragmentById(R.id.fragmentContainer).run {
            if (!(this as WebFragment).onBackPressed()) {
                Log.d(TAG, "Calling the usual back behavior")
                super.onBackPressed()
            } else {
                Log.d(TAG, "Calling back on the web view")
            }
        }
    }

    override fun finish() {
        super.finish()
        ActivityNavigator.applyPopAnimationsToPendingTransition(this)
    }
}