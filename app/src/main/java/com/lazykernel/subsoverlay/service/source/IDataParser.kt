package com.lazykernel.subsoverlay.service.source

import android.view.accessibility.AccessibilityNodeInfo

interface IDataParser {
    val includedIds: List<String>

    var episodeName: String
    var secondsSinceStart: Int

    fun updateState(node: AccessibilityNodeInfo?)

    fun getAllNodes(node: AccessibilityNodeInfo?): Iterator<AccessibilityNodeInfo> {
        return AccessibilityNodeIterator(node)
    }
}