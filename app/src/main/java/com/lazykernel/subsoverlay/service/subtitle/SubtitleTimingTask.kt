package com.lazykernel.subsoverlay.service.subtitle

import android.os.Handler
import android.util.Log
import com.lazykernel.subsoverlay.service.source.IDataParser
import java.util.*

class SubtitleTimingTask(private val mDataParser: IDataParser, private val mSubtitleManager: SubtitleManager, private val mHandler: Handler) : TimerTask() {
    // Might not last over invocations / timer triggers
    var mLastTimestamp: Long = 0
    var mCurrentTimerInSeconds: Double = 0.0
    // Default offset of 0ms
    var mOffsetInMilliseconds: Int = 0

    // TODO: Stop when service is destroyed, only run when in netflix media player view
    override fun run() {
        val currentTimestamp = System.currentTimeMillis()

        if (mDataParser.isPaused || !mDataParser.isInMediaPlayer) {
            mLastTimestamp = currentTimestamp
            return
        }

        if (mLastTimestamp == 0L) {
            mLastTimestamp = currentTimestamp
        }

        mCurrentTimerInSeconds += (currentTimestamp - mLastTimestamp) / 1000.0
        mLastTimestamp = currentTimestamp

        if (mDataParser.secondsChanged) {
            mDataParser.secondsChanged = false
            mCurrentTimerInSeconds = mDataParser.secondsSinceStart
        }

        mSubtitleManager.currentTimeInSeconds = mCurrentTimerInSeconds + (mOffsetInMilliseconds / 1000.0)
        mHandler.post {
            mSubtitleManager.runSubtitleUpdate()
        }
    }
}