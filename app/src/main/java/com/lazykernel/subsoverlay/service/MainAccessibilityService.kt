package com.lazykernel.subsoverlay.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent.ACTION_UP
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOWS_CHANGED
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import com.lazykernel.subsoverlay.R
import com.lazykernel.subsoverlay.application.DummyActivity
import com.lazykernel.subsoverlay.service.source.IDataParser
import com.lazykernel.subsoverlay.service.source.NetflixParser
import com.lazykernel.subsoverlay.service.subtitle.SubtitleManager
import com.lazykernel.subsoverlay.service.subtitle.SubtitleTimingTask
import com.lazykernel.subsoverlay.utils.Utils
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor


class MainAccessibilityService : AccessibilityService() {

    var mSettingsModalOpen: Boolean = false
    lateinit var mSettingsModalLayout: ConstraintLayout
    lateinit var mSubtitleManager: SubtitleManager
    lateinit var mSettingsLayout: LinearLayout
    lateinit var mSettingsLayoutParams: LayoutParams
    private var mServiceRunning: Boolean = false
    private val mTimer = Timer()
    lateinit var mSubtitleTimingTask: SubtitleTimingTask
    private val mDataParser = NetflixParser()
    private val mMainThreadHandler: Handler = Handler(Looper.getMainLooper())

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

        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        preferences.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener)

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val settingsPair = buildSettingsButtonView()
        mSettingsLayout = settingsPair.first
        mSettingsLayoutParams = settingsPair.second

        mSubtitleManager = SubtitleManager(applicationContext, windowManager)
        mSubtitleManager.buildSubtitleView()

        if (preferences.getBoolean("accessibilityServiceRunning", false)) {
            try {
                windowManager.addView(mSettingsLayout, mSettingsLayoutParams)
            }
            catch (ex: Exception) {
                Log.e("SUBSOVERLAY", "adding settings icon view failed", ex)
            }

            mSubtitleManager.openDefaultViews()
            if (this::mSubtitleTimingTask.isInitialized) {
                mSubtitleTimingTask.cancel()
            }
            // Run subtitle timing task every 0.5s
            mSubtitleTimingTask = SubtitleTimingTask(mDataParser, mSubtitleManager, mMainThreadHandler)
            mTimer.scheduleAtFixedRate(mSubtitleTimingTask, 0, 500)
            mServiceRunning = true
        }
    }

    private val mPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "accessibilityServiceRunning") {
                if (sharedPreferences.getBoolean("accessibilityServiceRunning", false)) {
                    openDefaultViews()
                    // Create a new timing task and make it run every 0.5s
                    if (this::mSubtitleTimingTask.isInitialized) {
                        mSubtitleTimingTask.cancel()
                    }
                    mSubtitleTimingTask = SubtitleTimingTask(mDataParser, mSubtitleManager, mMainThreadHandler)
                    mTimer.scheduleAtFixedRate(mSubtitleTimingTask, 0, 500)
                    mServiceRunning = true
                }
                else {
                    closeAll()
                    mSubtitleTimingTask.cancel()
                    mServiceRunning = false
                }
            }
        }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!mServiceRunning) {
            return
        }

        if (event == null) {
            return
        }

        mDataParser.updateState(event)
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
            if (data?.data != null) {
                mSubtitleManager.loadSubtitlesFromUri(data.data!!)
                val filenameLabel = mSettingsModalLayout.findViewById<TextView>(R.id.subFilenameLabel)
                // TODO: get actual filename or something
                filenameLabel.text = "File selected"
            }
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

    private fun openDefaultViews() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        mSubtitleManager.openDefaultViews()

        try {
            windowManager.addView(mSettingsLayout, mSettingsLayoutParams)
        }
        catch (ex: Exception) {
            Log.e("SUBSOVERLAY", "adding settings icon view failed", ex)
        }
    }

    private fun closeAll() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (mSettingsModalOpen) {
            closeSettingsModal()
        }

        windowManager.removeView(mSettingsLayout)
        mSubtitleManager.closeAll()
    }
}