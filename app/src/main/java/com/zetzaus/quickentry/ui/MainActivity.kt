package com.zetzaus.quickentry.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zetzaus.quickentry.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}