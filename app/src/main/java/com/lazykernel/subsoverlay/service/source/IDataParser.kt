package com.lazykernel.subsoverlay.service.source

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.abs
import kotlin.math.max

abstract class IDataParser {
    abstract val packageName: String
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

    abstract fun updateState(event: AccessibilityEvent?, service: AccessibilityService)

    fun getAllNodes(node: AccessibilityNodeInfo?): Iterator<AccessibilityNodeInfo> {
        return AccessibilityNodeIterator(node)
    }

    /*
    Returns false if just closed (or other window closed and the target package's window not open), true otherwise
     */
    fun checkApplicationOpen(event: AccessibilityEvent?, service: AccessibilityService): Boolean {
        // If we have access to window change types, use those to slightly cut down on events
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            if (event?.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED && event.windowChanges == AccessibilityEvent.WINDOWS_CHANGE_REMOVED && !isPackageWindowOpen(packageName, service)) {
                if (isInMediaPlayer) {
                    isInMediaPlayer = false
                    isInMediaPlayerChanged = true
                }
                return false
            }
        }
        else {
            if (event?.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED && !isPackageWindowOpen(packageName, service)) {
                if (isInMediaPlayer) {
                    isInMediaPlayer = false
                    isInMediaPlayerChanged = true
                }
                return false
            }
        }
        return true
    }

    companion object {
        fun isPackageWindowOpen(packageName: String, service: AccessibilityService): Boolean {
            return service.windows.any { it?.root?.packageName == packageName }
        }
    }
}