package com.lazykernel.subsoverlay.service.source

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.abs
import kotlin.math.max

abstract class IDataParser {
    abstract val includedIds: List<String>

    abstract var episodeName: String
    abstract var episodeChanged: Boolean
    abstract var secondsSinceStart: Double
    abstract var secondsChanged: Boolean
    abstract var isPaused: Boolean
    abstract var isInMediaPlayer: Boolean
    abstract var isInMediaPlayerChanged: Boolean

    fun Double.equalsDelta(other: Double) = abs(this - other) < max(Math.ulp(this), Math.ulp(other)) * 2

    protected val timeRegex = Regex("([0-9]{0,2}):([0-9]{0,2}):([0-9]{1,2})|([0-9]{0,2}):([0-9]{1,2})")

    abstract fun updateState(event: AccessibilityEvent?)

    fun getAllNodes(node: AccessibilityNodeInfo?): Iterator<AccessibilityNodeInfo> {
        return AccessibilityNodeIterator(node)
    }
}