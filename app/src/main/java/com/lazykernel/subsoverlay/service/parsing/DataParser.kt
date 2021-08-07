package com.lazykernel.subsoverlay.service.parsing

import android.view.accessibility.AccessibilityNodeInfo

interface DataParser {
    val includedIds: List<String>
    // Episode name and time remaining always seem to display for sure
    // also playback speed probably, but not implementing that yet
    // Estimate start time by looking at the seek bar and time remaining
    // Episode name: com.netflix.mediaclient:id/player_title_label
    // Time remaining: com.netflix.mediaclient:id/label_time_remaining
    // Playback speed: com.netflix.mediaclient:id/player_speed_button
    // Seek bar: com.netflix.mediaclient:id/timeline_seekbar
    // Pause button: com.netflix.mediaclient:id/player_pause_btn
    // Subtitles container: com.netflix.mediaclient:id/player_subtitles_container

    var episodeName: String

    fun getAllNodes(node: AccessibilityNodeInfo?): Iterator<AccessibilityNodeInfo> {
        return AccessibilityNodeIterator(node)
    }
}