package com.lazykernel.subsoverlay.service.source

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.*
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.pow

class NetflixParser : IDataParser() {
    // Episode name and time remaining always seem to display for sure
    // also playback speed probably, but not implementing that yet
    // Estimate start time by looking at the seek bar and time remaining
    // Episode name: com.netflix.mediaclient:id/player_title_label
    // Time remaining: com.netflix.mediaclient:id/label_time_remaining
    // Playback speed: com.netflix.mediaclient:id/player_speed_button
    // Seek bar: com.netflix.mediaclient:id/timeline_seekbar
    // Pause button: com.netflix.mediaclient:id/player_pause_btn
    // Subtitles container: com.netflix.mediaclient:id/player_subtitles_container

    override val packageName: String = "com.netflix.mediaclient"
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
    override var isPaused: Boolean = false
    override var isInMediaPlayer: Boolean = false
    override var isInMediaPlayerChanged: Boolean = false

    private var totalLength: Int? = null

    override fun updateState(event: AccessibilityEvent?, service: AccessibilityService) {
        if (!checkApplicationOpen(event, service)) {
            return
        }

        // Detecting moving back to (any?) netflix page from media player
        // Extremely janky solution, explanation:
        // With some limited testing, it seems like the only time TYPE_WINDOW_CONTENT_CHANGED event
        // is fired with className androidx.recyclerview.widget.RecyclerView, is when you go back to
        // any (or at least quite a few) of netflix main pages which makes sense. The only time this
        // happens in the player is when you click to view all episodes which mega conveniently also
        // happens to give us automatic pausing for that case.
        // (Note: this seems to only happen once initially, and then only when the list is scrolled.
        //  Good enough for now...)
        // Additionally, when we come back from episode selection, we get a
        // TYPE_WINDOW_STATE_CHANGED with className com.netflix.mediaclient.ui.player.PlayerActivity
        // which, again, very conveniently also gives us unpausing for free.
        // Forgive me Father, for I have sinned...
        if (event?.eventType == TYPE_WINDOW_CONTENT_CHANGED && event.packageName == packageName && event.className == "androidx.recyclerview.widget.RecyclerView") {
            // Assuming netflix media player window has been closed or episodes view has been entered
            isPaused = true
            if (isInMediaPlayer) {
                isInMediaPlayer = false
                isInMediaPlayerChanged = true
            }
            return
        }

        if (event?.eventType == TYPE_WINDOW_STATE_CHANGED && event.className == "com.netflix.mediaclient.ui.player.PlayerActivity") {
            // Assuming player entered
            if (!isInMediaPlayer) {
                isInMediaPlayer = true
                isInMediaPlayerChanged = true
            }
            isPaused = false
        }

        if (event?.eventType == TYPE_VIEW_CLICKED && event.source?.viewIdResourceName == "com.netflix.mediaclient:id/player_pause_btn") {
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