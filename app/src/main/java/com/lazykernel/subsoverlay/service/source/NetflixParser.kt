package com.lazykernel.subsoverlay.service.source

import android.view.accessibility.AccessibilityNodeInfo
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
                "com.netflix.mediaclient:id/label_time_remaining"
        )

    override var episodeName: String = ""
    override var secondsSinceStart: Int = 0

    private var totalLength: Int? = null
    private val timeRegex = Regex("([0-9]{0,2}):([0-9]{0,2}):([0-9]{1,2})|([0-9]{0,2}):([0-9]{1,2})")

    override fun updateState(node: AccessibilityNodeInfo?) {
        if (node == null)
            return

        val episodeNodes = node.findAccessibilityNodeInfosByViewId("com.netflix.mediaclient:id/player_title_label")
        val timeRemainingNodes = node.findAccessibilityNodeInfosByViewId("com.netflix.mediaclient:id/label_time_remaining")

        for (n in episodeNodes) {
            if (n.text.isNotBlank()) {
                episodeName = n.text.toString()
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
            if (secondsLeft != null) {
                secondsSinceStart = totalLength!! - secondsLeft
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
                                a + (v * (60.0.pow((values.size - i).toDouble()))).toInt()
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