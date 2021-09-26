package com.lazykernel.subsoverlay

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat

// Seems to work for Crunchyroll but not for Netflix
class AudioCaptureService : Service() {
    lateinit var mProjectionManager: MediaProjectionManager
    lateinit var mMediaProjection: MediaProjection
    var mAudioRecord: AudioRecord? = null
    var mRecordTask: RecordWaveTask? = null

    enum class Action {
        ACTION_START,
        ACTION_STOP
    }

    private val mProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()

            mRecordTask?.cancel(false)
            mRecordTask = null
            mAudioRecord = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "001"
            val channelName = "myChannel"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
            channel.lightColor = Color.BLUE
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            manager.createNotificationChannel(channel)
            val notification: Notification = Notification.Builder(applicationContext, channelId)
                    .setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentTitle("Recording audio")
                    .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } else {
                startForeground(101, notification)
            }
        }
        else {
            startForeground(101, Notification())
        }
    }

    // look into https://github.com/saki4510t/ScreenRecordingSample/blob/master/app/src/main/java/com/serenegiant/screenrecordingsample/MainActivity.java
    // and https://github.com/saki4510t/ScreenRecordingSample/blob/master/app/src/main/java/com/serenegiant/service/ScreenRecorderService.java
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                Action.ACTION_START.name -> startCapture(intent)
                Action.ACTION_STOP.name -> stopCapture()
            }
        }

        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    fun startCapture(intent: Intent) {
        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mMediaProjection = mProjectionManager.getMediaProjection(Activity.RESULT_OK, intent)
        mMediaProjection.registerCallback(mProjectionCallback, null)
        val config = AudioPlaybackCaptureConfiguration.Builder(mMediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .build()

        val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(44100)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build()

        mAudioRecord = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioRecord.Builder()
                    .setAudioFormat(audioFormat)
                    .setAudioPlaybackCaptureConfig(config)
                    .build()
        } else {
            null
        }

        mAudioRecord?.startRecording()
        mRecordTask = RecordWaveTask()
        mRecordTask!!.setContext(applicationContext)
        mRecordTask!!.execute(mAudioRecord)
    }

    fun stopCapture() {
        mRecordTask?.cancel(false)
        mRecordTask = null
        mAudioRecord = null
    }
}