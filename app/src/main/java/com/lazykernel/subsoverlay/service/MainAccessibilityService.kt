package com.lazykernel.subsoverlay.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent.ACTION_UP
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.accessibility.AccessibilityEvent
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import com.lazykernel.subsoverlay.R
import com.lazykernel.subsoverlay.application.DummyActivity
import com.lazykernel.subsoverlay.service.subtitle.SubtitleManager
import com.lazykernel.subsoverlay.utils.Utils


class MainAccessibilityService : AccessibilityService() {

    var mSettingsModalOpen: Boolean = false
    lateinit var mSettingsModalLayout: ConstraintLayout

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
        val (settingsLayout, settingsLayoutParams) = buildSettingsButtonView()

        try {
            windowManager.addView(settingsLayout, settingsLayoutParams)
        }
        catch (ex: Exception) {
            Log.e("SUBSOVERLAY", "adding settings icon view failed", ex)
        }

        val subManager = SubtitleManager(applicationContext, windowManager)
        subManager.buildSubtitleView()
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
    }

    override fun onInterrupt() {
        Log.i("SUBSOVERLAY", "interrupt")
    }

    fun buildSettingsButtonView(): Pair<LinearLayout, LayoutParams> {
        val layout = LinearLayout(applicationContext)
        val imageView = ImageView(applicationContext)
        imageView.setImageResource(R.drawable.ic_baseline_settings_white_24dp)
        layout.addView(imageView)
        layout.setBackgroundColor(0x00000000)

        val layoutParams = LayoutParams()
        layoutParams.apply {
            x = Utils.dpToPixels(10F).toInt()
            y = Utils.dpToPixels(10F).toInt()
            width = Utils.dpToPixels(20F).toInt()
            height = Utils.dpToPixels(20F).toInt()
            type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.TOP or Gravity.RIGHT
            format = PixelFormat.TRANSPARENT
            flags = LayoutParams.FLAG_NOT_FOCUSABLE
        }

        layout.setOnTouchListener { view, event ->
            if (!mSettingsModalOpen && event.action == ACTION_UP) {
                mSettingsModalOpen = true
                openSettingsModal()
            }
            true
        }

        return Pair(layout, layoutParams)
    }

    fun openSettingsModal() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        mSettingsModalLayout = View.inflate(applicationContext, R.layout.settings_modal, null) as ConstraintLayout
        mSettingsModalLayout.apply {
            setBackgroundColor(Color.WHITE)
            background = AppCompatResources.getDrawable(applicationContext, R.drawable.rounded_corners)
            clipToOutline = true
        }

        val layoutParams = LayoutParams()

        layoutParams.apply {
            width = Utils.dpToPixels(350F).toInt()
            height = Utils.dpToPixels(200F).toInt()
            type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.CENTER
            format = PixelFormat.TRANSPARENT
            flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
        }

        mSettingsModalLayout.findViewById<ImageButton>(R.id.buttonCloseModal).setOnTouchListener{ view, event ->
            if (event.action == ACTION_UP) {
                closeSettingsModal()
            }
            true
        }

        mSettingsModalLayout.findViewById<Button>(R.id.buttonSelectSubFile).setOnTouchListener{ view, event ->
            Log.i("SUBSOVERLAY", "clicked select subfile $event")
            if (event.action == ACTION_UP) {
                selectSubFile()
            }
            true
        }

        try {
            windowManager.addView(mSettingsModalLayout, layoutParams)
        }
        catch (ex: Exception) {
            Log.e("SUBSOVERLAY", "adding subs view failed", ex)
        }
    }

    private val mSubListener = object : DummyActivity.ResultListener() {
        override fun onSuccess(data: Intent?) {
            Log.i("SUBSOVERLAY", "Loaded $data")
            openSettingsModal()
        }

        override fun onFailure(data: Intent?) {
            Log.i("SUBSOVERLAY", "Sub file selecting cancelled")
            openSettingsModal()
        }
    }

    private fun selectSubFile() {
        DummyActivity.mResultListener = mSubListener

        val bundle = Bundle()
        bundle.putInt("action", DummyActivity.Actions.ACTION_PICK_SUB_FILE.ordinal)

        val intent = Intent()
        intent.apply {
            setClass(this@MainAccessibilityService, DummyActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtras(bundle)
        }
        startActivity(intent)
        closeSettingsModal()
    }

    private fun closeSettingsModal() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.removeView(mSettingsModalLayout)
        mSettingsModalOpen = false
    }
}