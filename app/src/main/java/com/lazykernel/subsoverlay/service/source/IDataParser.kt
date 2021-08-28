package com.lazykernel.subsoverlay.service.source

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

interface IDataParser {
    val includedIds: List<String>

    var episodeName: String
    var episodeChanged: Boolean
    var secondsSinceStart: Double
    var secondsChanged: Boolean
    var isPaused: Boolean

    fun updateState(event: AccessibilityEvent?)

    fun getAllNodes(node: AccessibilityNodeInfo?): Iterator<AccessibilityNodeInfo> {
        return AccessibilityNodeIterator(node)
    }
}