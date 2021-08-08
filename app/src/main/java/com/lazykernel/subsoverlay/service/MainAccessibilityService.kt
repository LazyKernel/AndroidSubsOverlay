package com.lazykernel.subsoverlay.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import com.lazykernel.subsoverlay.R
import java.util.*


class MainAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 0
        }

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val (pauseLayout, pauseLayoutParams) = buildPauseButtonView()

        val (subsLayout, subsLayoutParams) = buildSubsView()

//        try {
//            windowManager.addView(subsLayout, subsLayoutParams)
//        }
//        catch (ex: Exception) {
//            Log.e("SUBSOVERLAY", "adding subs view failed", ex)
//        }
    }



    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        //Log.i("SUBSOVERLAY", "accessibility event $event")
        if (event == null) {
            return
        }
        
        if (event.source == null) {
            //Log.i("SUBSOVERLAY", "source null")
            return
        }

        Utils.printAllViews(event.source)

        // Episode name and time remaining always seem to display for sure
        // also playback speed probably, but not implementing that yet
        // Estimate start time by looking at the seek bar and time remaining
        // Episode name: com.netflix.mediaclient:id/player_title_label
        // Time remaining: com.netflix.mediaclient:id/label_time_remaining
        // Playback speed: com.netflix.mediaclient:id/player_speed_button
        // Seek bar: com.netflix.mediaclient:id/timeline_seekbar
        // Pause button: com.netflix.mediaclient:id/player_pause_btn
        // Subtitles container: com.netflix.mediaclient:id/player_subtitles_container


        //Log.i("SUBSOVERLAY", "source ${event.source}")

//        if (event.source.text != null) {
//            Log.i("SUBSOVERLAY", "text ${event.source.text}")
//        }
//
//        var node = event.source.parent
//
//        while (node != null) {
//            if (node.text != null){
//                Log.i("SUBSOVERLAY", "parent text ${node.text}")
//            }
//            node = node.parent
//        }


        //Log.i("SUBSOVERLAY", "source $event.source")
    }

    override fun onInterrupt() {
        Log.i("SUBSOVERLAY", "interrupt")
    }

    fun buildPauseButtonView(): Pair<LinearLayout, LayoutParams> {
        val layout = LinearLayout(applicationContext)
        layout.setBackgroundColor(Color.GREEN and 0x55FFFFFF)

        val layoutParams = LayoutParams()
        layoutParams.apply {
            width = 300
            height = 300
            type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.CENTER
            format = PixelFormat.TRANSPARENT
            alpha = 0.0F
            flags = LayoutParams.FLAG_NOT_TOUCHABLE
        }

        layout.setOnTouchListener { view, event ->
            Log.i("SUBSOVERLAY", "pause event $event")
            false
        }

        return Pair(layout, layoutParams)
    }

    fun buildSubsView(): Pair<LinearLayout, LayoutParams> {
        val layout = LinearLayout(applicationContext)
        val textView = TextView(applicationContext)
        textView.apply {
            id = R.id.subsTextView
        }
        layout.addView(textView)

        val layoutParams = LayoutParams()
        layoutParams.apply {
            y = 200
            width = 600
            height = 300
            type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            format = PixelFormat.TRANSPARENT
            flags = LayoutParams.FLAG_NOT_FOCUSABLE
        }

        layout.apply {
            setBackgroundColor(0x88000000.toInt())
        }

        layout.setOnTouchListener { view, event ->
            Log.i("SUBSOVERLAY", "subs event $event")
            true
        }

        return Pair(layout, layoutParams)
    }

    fun buildSettingsButtonView() {

    }
}