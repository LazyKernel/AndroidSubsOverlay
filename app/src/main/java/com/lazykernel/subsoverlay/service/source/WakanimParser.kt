package com.lazykernel.subsoverlay.service.source

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlin.math.pow

/*
Pause button clicked, text seems to always be [Pause]
I/WAKANIM: EventType: TYPE_VIEW_CLICKED; EventTime: 1157066; PackageName: wakanimapp.wakanimapp; MovementGranularity: 0; Action: 0; ContentChangeTypes: []; WindowChangeTypes: [] [ ClassName: android.widget.ImageButton; Text: [Pause]; ContentDescription: Pause; ItemCount: -1; CurrentItemIndex: -1; Enabled: true; Password: false; Checked: false; FullScreen: false; Scrollable: false; BeforeText: null; FromIndex: -1; ToIndex: -1; ScrollX: 0; ScrollY: 0; MaxScrollX: 0; MaxScrollY: 0; ScrollDeltaX: -1; ScrollDeltaY: -1; AddedCount: -1; RemovedCount: -1; ParcelableData: null ]; recordCount: 0

Seconds since start
I/WAKANIM: EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 1149221; PackageName: wakanimapp.wakanimapp; MovementGranularity: 0; Action: 0; ContentChangeTypes: [CONTENT_CHANGE_TYPE_TEXT]; WindowChangeTypes: [] [ ClassName: android.widget.TextView; Text: []; ContentDescription: null; ItemCount: -1; CurrentItemIndex: -1; Enabled: true; Password: false; Checked: false; FullScreen: false; Scrollable: false; BeforeText: null; FromIndex: -1; ToIndex: -1; ScrollX: 0; ScrollY: 0; MaxScrollX: 0; MaxScrollY: 0; ScrollDeltaX: -1; ScrollDeltaY: -1; AddedCount: -1; RemovedCount: -1; ParcelableData: null ]; recordCount: 0
    android.view.accessibility.AccessibilityNodeInfo@126720; boundsInParent: Rect(0, 0 - 121, 46); boundsInScreen: Rect(0, 1021 - 121, 1067); packageName: wakanimapp.wakanimapp; className: android.widget.TextView; text: 00:13; error: null; maxTextLength: -1; stateDescription: null; contentDescription: null; tooltipText: null; viewIdResName: wakanimapp.wakanimapp:id/exo_position; checkable: false; checked: false; focusable: false; focused: false; selected: false; clickable: false; longClickable: false; contextClickable: false; enabled: true; password: false; scrollable: false; importantForAccessibility: true; visible: true; actions: [AccessibilityAction: ACTION_SELECT - null, AccessibilityAction: ACTION_CLEAR_SELECTION - null, AccessibilityAction: ACTION_ACCESSIBILITY_FOCUS - null, AccessibilityAction: ACTION_NEXT_AT_MOVEMENT_GRANULARITY - null, AccessibilityAction: ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY - null, AccessibilityAction: ACTION_SET_SELECTION - null, AccessibilityAction: ACTION_SHOW_ON_SCREEN - null]

Entered the player
I/WAKANIM: EventType: TYPE_WINDOW_STATE_CHANGED; EventTime: 1129726; PackageName: wakanimapp.wakanimapp; MovementGranularity: 0; Action: 0; ContentChangeTypes: []; WindowChangeTypes: [] [ ClassName: com.wakanim.wakanimapp.test.VideoActivity; Text: [Wakanim]; ContentDescription: null; ItemCount: -1; CurrentItemIndex: -1; Enabled: true; Password: false; Checked: false; FullScreen: true; Scrollable: false; BeforeText: null; FromIndex: -1; ToIndex: -1; ScrollX: 0; ScrollY: 0; MaxScrollX: 0; MaxScrollY: 0; ScrollDeltaX: -1; ScrollDeltaY: -1; AddedCount: -1; RemovedCount: -1; ParcelableData: null ]; recordCount: 0
    android.view.accessibility.AccessibilityNodeInfo@80006cd6; boundsInParent: Rect(0, 0 - 2280, 1080); boundsInScreen: Rect(0, 0 - 2280, 1080); packageName: wakanimapp.wakanimapp; className: android.widget.FrameLayout; text: null; error: null; maxTextLength: -1; stateDescription: null; contentDescription: null; tooltipText: null; viewIdResName: null; checkable: false; checked: false; focusable: false; focused: false; selected: false; clickable: false; longClickable: false; contextClickable: false; enabled: true; password: false; scrollable: false; importantForAccessibility: true; visible: true; actions: [AccessibilityAction: ACTION_SELECT - null, AccessibilityAction: ACTION_CLEAR_SELECTION - null, AccessibilityAction: ACTION_ACCESSIBILITY_FOCUS - null, AccessibilityAction: ACTION_SHOW_ON_SCREEN - null]

 */
class WakanimParser : IDataParser() {
    override val packageName: String = "wakanimapp.wakanimapp"
    override val includedIds: List<String>
        get() = listOf(
                "wakanimapp.wakanimapp:id/exo_position"
        )
    // Don't know how to get from Wakanim
    override var episodeName: String = ""
    override var episodeChanged: Boolean = false

    override var secondsSinceStart: Double = 0.0
    override var secondsChanged: Boolean = false
    // Seems to always start unpaused
    override var isPaused: Boolean = false

    override var isInMediaPlayer: Boolean = true
    override var isInMediaPlayerChanged: Boolean = false

    private val exitActivityClassNames = listOf(
            "com.wakanim.wakanimapp.test.MainActivity",
            "com.wakanim.wakanimapp.test.SearchResultActivity",
            "com.wakanim.wakanimapp.test.ShowDetailActivity"
    )

    override fun updateState(event: AccessibilityEvent?, service: AccessibilityService) {
        // Return instantly if just closed
        if (!checkApplicationOpen(event, service)) {
            return
        }

        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && exitActivityClassNames.any { event.className == it }) {
            // Assuming player closed
            if (isInMediaPlayer) {
                isInMediaPlayer = false
                isInMediaPlayerChanged = true
                isPaused = true
            }
            return
        }

        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.className == "com.wakanim.wakanimapp.test.VideoActivity") {
            // Assuming player entered
            if (!isInMediaPlayer) {
                isInMediaPlayer = true
                isInMediaPlayerChanged = true
                isPaused = false
            }
        }

        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && event.className == "android.widget.ImageButton") {
            // Pause button was clicked, set paused state
            if (event.text.toString() == "[Pause]") {
                // paused
                isPaused = true
            }
            else if (event.text.toString() == "[Play]") {
                // unpaused
                isPaused = false
            }
        }

        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && event.source?.viewIdResourceName == "wakanimapp.wakanimapp:id/exo_position") {
            val source = event.source
            val match = timeRegex.matchEntire(source.text)
            if (match != null) {
                val values = match.groups.filterNotNull()
                val newSeconds = values.slice(IntRange(1, values.size - 1)).map { it.value.toInt() }
                        .reduceRightIndexed { i, v, a ->
                            // accumulated + value * 60s ^ index -> seconds left
                            // -2 because seconds are used as accumulator
                            a + (v * (60.0.pow((values.size - 2 - i).toDouble()))).toInt()
                        }

                if (!secondsSinceStart.equalsDelta(newSeconds.toDouble())) {
                    secondsSinceStart = newSeconds.toDouble()
                    secondsChanged = true
                }
            }
        }
    }
}