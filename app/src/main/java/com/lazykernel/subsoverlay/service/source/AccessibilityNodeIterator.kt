package com.lazykernel.subsoverlay.service.source

import android.view.accessibility.AccessibilityNodeInfo
import java.util.*

class AccessibilityNodeIterator(start: AccessibilityNodeInfo?) : Iterator<AccessibilityNodeInfo> {
    private val nodeQueue: Queue<AccessibilityNodeInfo?> = LinkedList(listOf(start))

    private lateinit var nextNode: AccessibilityNodeInfo

    override fun hasNext(): Boolean {
        while (nodeQueue.isNotEmpty()) {
            val node = nodeQueue.poll() ?: continue

            for (i in 0 until node.childCount) {
                nodeQueue.add(node.getChild(i))
            }

            nextNode = node
            return true
        }
        return false
    }

    override fun next(): AccessibilityNodeInfo {
        return nextNode
    }
}