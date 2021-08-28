package com.lazykernel.subsoverlay.service.source

import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.*
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

class NetflixParser : IDataParser {
    // Episode name and time remaining always seem to display for sure
    // also playback speed probably, but not implementing that yet
    // Estimate start time by looking at the seek bar and time remaining
    // Episode name: com.netflix.mediaclient:id/player_title_label
    // Time remaining: com.netflix.mediaclient:id/label_time_remaining
    // Playback speed: com.netflix.mediaclient:id/player_speed_button
    // Seek bar: com.netflix.mediaclient:id/timeline_seekbar
    // Pause button: com.netflix.mediaclient:id/player_pause_btn
    // Subtitles container: com.netflix.mediaclient:id/player_subtitles_container

    override val includedIds: List<String>
        get() = listOf(
                "com.netflix.mediaclient:id/player_title_label",
                "com.netflix.mediaclient:id/label_time_remaining",
                "com.netflix.mediaclient:id/player_pause_btn"
        )

    override var episodeName: String = ""
    override var episodeChanged: Boolean = false
    override var secondsSinceStart: Double = 0.0
    override var secondsChanged: Boolean = false
    // Never seen netflix start paused (starting a movie / coming from "alt-tab")
    override var isPaused: Boolean = false

    private var totalLength: Int? = null
    private val timeRegex = Regex("([0-9]{0,2}):([0-9]{0,2}):([0-9]{1,2})|([0-9]{0,2}):([0-9]{1,2})")

    fun Double.equalsDelta(other: Double) = abs(this - other) < max(Math.ulp(this), Math.ulp(other)) * 2

    override fun updateState(event: AccessibilityEvent?) {
        Log.i("SUBSOVERLAY", "$event\n${event?.source}")
        if (event?.eventType == TYPE_WINDOWS_CHANGED) {
            // Assuming netflix window has been closed, pausing
            //Log.i("SUBSOVERLAY", "assuming closed, pausing\n${event.windowChanges}\n$event\n${event.source}")
            isPaused = true
            return
        }

        if (event?.eventType == TYPE_WINDOW_STATE_CHANGED && event.className == "com.netflix.mediaclient.ui.player.PlayerActivity") {
            // Assuming player entered, unpause
            Log.i("SUBSOVERLAY", "assuming entered, unpausing")
            isPaused = false
        }

        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && event.source?.viewIdResourceName == "com.netflix.mediaclient:id/player_pause_btn") {
            // Pause button was clicked, toggle paused state
            isPaused = !isPaused
        }

        val node = event?.source ?: return

        val episodeNodes = node.findAccessibilityNodeInfosByViewId("com.netflix.mediaclient:id/player_title_label")
        val timeRemainingNodes = node.findAccessibilityNodeInfosByViewId("com.netflix.mediaclient:id/label_time_remaining")

        for (n in episodeNodes) {
            if (n.text.isNotBlank()) {
                if (episodeName != n.text.toString()) {
                    episodeName = n.text.toString()
                    episodeChanged = true
                }
            }
        }

        var rangeInfo: AccessibilityNodeInfo.RangeInfo? = null
        if (totalLength == null) {
            val seekBarNode = node.findAccessibilityNodeInfosByViewId("com.netflix.mediaclient:id/timeline_seekbar")
            for (n in seekBarNode) {
                if (n != null) {
                    rangeInfo = n.rangeInfo
                }
            }

            if (rangeInfo != null) {
                val secondsLeft = getSecondsLeft(timeRemainingNodes)
                if (secondsLeft != null) {
                    val normalizedCur = getNormalizedRangeCurrent(rangeInfo)
                    totalLength = ((secondsLeft * normalizedCur / (1 - normalizedCur)) + secondsLeft).toInt()
                }
            }
        }

        if (totalLength != null) {
            val secondsLeft = getSecondsLeft(timeRemainingNodes)
            Log.v("SUBSOVERLAY", "seconds left: $secondsLeft")
            if (secondsLeft != null) {
                if (!secondsSinceStart.equalsDelta((totalLength!! - secondsLeft).toDouble())) {
                    secondsSinceStart = (totalLength!! - secondsLeft).toDouble()
                    secondsChanged = true
                }
            }
        }
    }

    private fun getSecondsLeft(nodes: List<AccessibilityNodeInfo?>): Int? {
        for (n in nodes) {
            if (n != null && n.text != null) {
                val match = timeRegex.matchEntire(n.text)
                if (match != null) {
                    val values = match.groups.filterNotNull()
                    return values.slice(IntRange(1, values.size - 1)).map { it.value.toInt() }
                            .reduceRightIndexed { i, v, a ->
                                // accumulated + value * 60s ^ index -> seconds left
                                // -2 because seconds are used as accumulator
                                a + (v * (60.0.pow((values.size - 2 - i).toDouble()))).toInt()
                            }
                }
            }
        }

        return null
    }

    private fun getNormalizedRangeCurrent(rangeInfo: AccessibilityNodeInfo.RangeInfo): Float {
        return when (rangeInfo.type) {
            AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_PERCENT -> rangeInfo.current / 100.0F
            else -> {
                rangeInfo.current / (rangeInfo.max - rangeInfo.min)
            }
        }
    }
}