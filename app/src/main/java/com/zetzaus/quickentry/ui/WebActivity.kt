package com.zetzaus.quickentry.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.zetzaus.quickentry.R

class WebActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        val url = WebActivityArgs.fromBundle(intent.extras!!).url

        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, WebFragment.create(url))
                .commit()
        }
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

    companion object {
        val TAG = WebActivity::class.simpleName
    }
}