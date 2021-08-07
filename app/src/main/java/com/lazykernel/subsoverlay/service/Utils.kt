package com.lazykernel.subsoverlay.service

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.util.*

class Utils {
    companion object {
        @JvmStatic
        fun printAllViews(nodeInfo: AccessibilityNodeInfo?) {
            var debugDepth = 0
            val nodeQueue: Queue<AccessibilityNodeInfo?> = LinkedList(listOf(nodeInfo))
            while (nodeQueue.isNotEmpty()) {
                val node = nodeQueue.poll() ?: continue

                var log = ""
                for (i in 0 until debugDepth) {
                    log += "."
                }
                log += "(${node.text} <-- ${node.viewIdResourceName}) ${node.rangeInfo ?: "no range"}"
                Log.d("SUBSOVERLAY", log)
                debugDepth++
                for (i in 0 until node.childCount) {
                    nodeQueue.add(node.getChild(i))
                }
                debugDepth--
            }
        }
    }
}