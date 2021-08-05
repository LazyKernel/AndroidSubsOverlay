package com.lazykernel.subsoverlay.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import java.lang.Exception
import kotlin.random.Random

class MainAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val layout = LinearLayout(applicationContext)
        layout.setBackgroundColor(Color.GREEN and 0x55FFFFFF)

        val layoutParams = LayoutParams()
        layoutParams.apply {
            y = 500
            x = 400
            width = 300
            height = 300
            type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.TOP or Gravity.START
            format = PixelFormat.TRANSPARENT
            flags = LayoutParams.FLAG_NOT_FOCUSABLE
        }

        try {
            windowManager.addView(layout, layoutParams)
        }
        catch (ex: Exception) {
            Log.e("SUBSOVERLAY", "adding view failed", ex)
        }

        val random = Random(System.currentTimeMillis())
        layout.setOnTouchListener { view, event ->
            Log.i("SUBSOVERLAY", "event $event")
            layout.setBackgroundColor((random.nextInt() or 0xFF000000.toInt()) and 0x55FFFFFF)
            true
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.i("SUBSOVERLAY", "accessibility event $event")
    }

    override fun onInterrupt() {
        Log.i("SUBSOVERLAY", "interrupt")
    }
}