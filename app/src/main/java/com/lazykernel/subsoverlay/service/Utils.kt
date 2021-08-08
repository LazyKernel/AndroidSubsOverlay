package com.lazykernel.subsoverlay.service

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.util.*

class Utils {
    companion object {
        @JvmStatic
        fun printAllViews(nodeInfo: AccessibilityNodeInfo?) {
            val nodeQueue: Queue<Pair<AccessibilityNodeInfo?, Int>> = LinkedList(listOf(Pair(nodeInfo, 0)))
            while (nodeQueue.isNotEmpty()) {
                val (node, debugDepth) = nodeQueue.poll() ?: continue
                node ?: continue

                var log = ""
                for (i in 0 until debugDepth) {
                    log += "."
                }
                // Range info works for seek bar
                log += "(${node.text} <-- ${node.viewIdResourceName}) ${node.rangeInfo ?: "no range"}"
                Log.d("SUBSOVERLAY", log)

                for (i in 0 until node.childCount) {
                    nodeQueue.add(Pair(node.getChild(i), debugDepth + 1))
                }
            }
        }
    }
}