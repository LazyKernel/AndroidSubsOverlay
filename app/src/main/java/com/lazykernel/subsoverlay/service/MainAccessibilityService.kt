package com.lazykernel.subsoverlay.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.preference.PreferenceManager
import com.lazykernel.subsoverlay.AudioCaptureService
import com.lazykernel.subsoverlay.R
import com.lazykernel.subsoverlay.application.DummyActivity
import com.lazykernel.subsoverlay.service.source.CrunchyrollParser
import com.lazykernel.subsoverlay.service.source.IDataParser
import com.lazykernel.subsoverlay.service.source.NetflixParser
import com.lazykernel.subsoverlay.service.subtitle.SubtitleManager
import com.lazykernel.subsoverlay.service.subtitle.SubtitleTimingTask
import com.lazykernel.subsoverlay.utils.Utils
import java.util.*


class MainAccessibilityService : AccessibilityService() {

    var mSettingsModalOpen: Boolean = false
    lateinit var mSettingsModalLayout: ConstraintLayout
    lateinit var mSubtitleManager: SubtitleManager
    lateinit var mSettingsLayout: LinearLayout
    lateinit var mSettingsLayoutParams: LayoutParams
    private var mServiceRunning: Boolean = false
    private val mTimer = Timer()
    lateinit var mSubtitleTimingTask: SubtitleTimingTask
    private var mDataParser: IDataParser = CrunchyrollParser()
    private val mAvailableParsers = mapOf("com.netflix.mediaclient" to NetflixParser::class.java, "com.crunchyroll.crunchyroid" to CrunchyrollParser::class.java)
    private val mMainThreadHandler: Handler = Handler(Looper.getMainLooper())
    private var mSelectedFile: String? = null

    override fun onServiceConnected() {
        serviceInfo.apply {
                        // Used for detecting entering netflix player and exiting netflix completely
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    // Used for detecting pause button clicks
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    // Used for parsing current time and detecting when going back to netflix menu
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    // Used for parsing current time
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
            if (this::mSubtitleTimingTask.isInitialized) {
                mSubtitleTimingTask.cancel()
            }
            // Run subtitle timing task every 0.5s
            mSubtitleTimingTask = SubtitleTimingTask(mDataParser, mSubtitleManager, mMainThreadHandler)
            mTimer.scheduleAtFixedRate(mSubtitleTimingTask, 0, 500)
            mSubtitleTimingTask.mOffsetInMilliseconds = preferences.getInt("subtitleOffset", 0)
            mServiceRunning = true
        }
    }

    private val mPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "accessibilityServiceRunning") {
                if (sharedPreferences.getBoolean("accessibilityServiceRunning", false)) {
                    // Create a new timing task and make it run every 0.5s
                    if (this::mSubtitleTimingTask.isInitialized) {
                        mSubtitleTimingTask.cancel()
                    }
                    mSubtitleTimingTask = SubtitleTimingTask(mDataParser, mSubtitleManager, mMainThreadHandler)
                    mTimer.scheduleAtFixedRate(mSubtitleTimingTask, 0, 250)
                    val preferences = PreferenceManager.getDefaultSharedPreferences(this@MainAccessibilityService)
                    mSubtitleTimingTask.mOffsetInMilliseconds = preferences.getInt("subtitleOffset", 0)
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

        if (event.eventType == TYPE_WINDOWS_CHANGED) {
            val packageOpen = mAvailableParsers.keys.find { IDataParser.isPackageWindowOpen(it, this) }
            // Is one of our target windows open and different from the current parser
            if (packageOpen != null && packageOpen != mDataParser.packageName) {
                mDataParser = mAvailableParsers[packageOpen]!!.newInstance()
                mSubtitleTimingTask.mDataParser = mDataParser
            }
        }

        mDataParser.updateState(event, this)

        if (mDataParser.isInMediaPlayerChanged) {
            mDataParser.isInMediaPlayerChanged = false
            if (mDataParser.isInMediaPlayer) {
                openDefaultViews()
            }
            else {
                closeAll()
            }
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
            y = Utils.dpToPixels(75F).toInt()
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
            width = resources.getDimension(R.dimen._275sdp).toInt()
            height = resources.getDimension(R.dimen._160sdp).toInt()
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

        val subOffsetTextView = mSettingsModalLayout.findViewById<TextView>(R.id.sub_offset)
        subOffsetTextView.text = mSubtitleTimingTask.mOffsetInMilliseconds.toString()

        if (mSelectedFile != null) {
            val filenameLabel = mSettingsModalLayout.findViewById<TextView>(R.id.subFilenameLabel)
            filenameLabel.text = mSelectedFile
        }

        mSettingsModalLayout.findViewById<Button>(R.id.offset_minus_100_btn).setOnTouchListener { view, event ->
            if (event.action == ACTION_UP) {
                mSubtitleTimingTask.mOffsetInMilliseconds -= 100
                subOffsetTextView.text = mSubtitleTimingTask.mOffsetInMilliseconds.toString()
                val preferences = PreferenceManager.getDefaultSharedPreferences(this@MainAccessibilityService)
                preferences.edit().putInt("subtitleOffset", mSubtitleTimingTask.mOffsetInMilliseconds).apply()
            }
            true
        }

        mSettingsModalLayout.findViewById<Button>(R.id.offset_plus_100_btn).setOnTouchListener { view, event ->
            if (event.action == ACTION_UP) {
                mSubtitleTimingTask.mOffsetInMilliseconds += 100
                subOffsetTextView.text = mSubtitleTimingTask.mOffsetInMilliseconds.toString()
                val preferences = PreferenceManager.getDefaultSharedPreferences(this@MainAccessibilityService)
                preferences.edit().putInt("subtitleOffset", mSubtitleTimingTask.mOffsetInMilliseconds).apply()
            }
            true
        }

        mSettingsModalLayout.findViewById<Button>(R.id.temp_start_record_btn).setOnTouchListener { view, event ->
            if (event.action == ACTION_UP) {
                tempStartRecording()
            }
            true
        }

        mSettingsModalLayout.findViewById<Button>(R.id.temp_stop_btn).setOnTouchListener { view, event ->
            if (event.action == ACTION_UP) {
                tempStopRecording()
            }
            true
        }

        mSettingsModalLayout

        try {
            windowManager.addView(mSettingsModalLayout, layoutParams)
        }
        catch (ex: Exception) {
            Log.e("SUBSOVERLAY", "adding subs view failed", ex)
        }
    }

    private val mSubListener = object : DummyActivity.ResultListener() {
        override fun onSuccess(type: DummyActivity.Actions, data: Intent?) {
            Log.i("SUBSOVERLAY", "Loaded $data")
            if (data?.data != null) {
                mSubtitleManager.loadSubtitlesFromUri(data.data!!)
                val filenameLabel = mSettingsModalLayout.findViewById<TextView>(R.id.subFilenameLabel)
                // TODO: get actual filename or something
                mSelectedFile = "File selected"
                filenameLabel.text = "File selected"
            }
            openSettingsModal()
        }

        override fun onFailure(type: DummyActivity.Actions, data: Intent?) {
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
            if (!mSettingsLayout.isAttachedToWindow) {
                windowManager.addView(mSettingsLayout, mSettingsLayoutParams)
            }
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

        if (mSettingsLayout.isAttachedToWindow) {
            windowManager.removeView(mSettingsLayout)
        }
        mSubtitleManager.closeAll()
    }

    private val mProjectionListener = object : DummyActivity.ResultListener() {
        override fun onSuccess(type: DummyActivity.Actions, data: Intent?) {
            Log.i("SUBSOVERLAY", "Loaded $data")
            if (data != null) {
                val intent = Intent(this@MainAccessibilityService, AudioCaptureService::class.java)
                intent.action = AudioCaptureService.Action.ACTION_START.name
                intent.putExtras(data)
                startService(intent)
            }
        }

        override fun onFailure(type: DummyActivity.Actions, data: Intent?) {
            Log.i("SUBSOVERLAY", "Sub file selecting cancelled")
        }
    }

    private fun tempStartRecording() {
        DummyActivity.mResultListener = mProjectionListener

        val bundle = Bundle()
        bundle.putInt("action", DummyActivity.Actions.ACTION_MEDIA_PROJECTION.ordinal)

        val intent = Intent()
        intent.apply {
            setClass(this@MainAccessibilityService, DummyActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtras(bundle)
        }
        startActivity(intent)
        closeSettingsModal()
    }

    private fun tempStopRecording() {
        val intent = Intent(this@MainAccessibilityService, AudioCaptureService::class.java)
        intent.action = AudioCaptureService.Action.ACTION_STOP.name
        startService(intent)
    }
}