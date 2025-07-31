package com.cognivaa.amsapp

import com.journeyapps.barcodescanner.CaptureActivity
import android.content.pm.ActivityInfo
import android.os.Bundle

class MyCaptureActivity : CaptureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}