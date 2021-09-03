package com.lazykernel.subsoverlay.service.source

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
import kotlin.math.pow

/*
Play button clicked: classname = android.widget.ImageButton, text = Play
Current time: com.crunchyroll.crunchyroid:id/current_time TYPE_WINDOW_CONTENT_CHANGED (6:45)
Enter: com.ellation.crunchyroll.presentation.content.WatchPageActivity TYPE_WINDOW_STATE_CHANGED? with the same thing as netflix with overlay closing
( works even when coming to full screen)

For episode:
I/SUBSOVERLAY: event: EventType: TYPE_VIEW_CLICKED; EventTime: 918269; PackageName: com.crunchyroll.crunchyroid; MovementGranularity: 0; Action: 0; ContentChangeTypes: []; WindowChangeTypes: [] [ ClassName: android.view.ViewGroup; Text: [S1 E1 - The Strongest Maid in History, Tohru! (Well, She is a Dragon), 23m]; ContentDescription: null; ItemCount: -1; CurrentItemIndex: -1; Enabled: true; Password: false; Checked: false; FullScreen: false; Scrollable: false; BeforeText: null; FromIndex: -1; ToIndex: -1; ScrollX: 0; ScrollY: 0; MaxScrollX: 0; MaxScrollY: 0; ScrollDeltaX: -1; ScrollDeltaY: -1; AddedCount: -1; RemovedCount: -1; ParcelableData: null ]; recordCount: 0
 */
class CrunchyrollParser : IDataParser() {
    override val includedIds: List<String>
        get() = listOf(
                "com.crunchyroll.crunchyroid:id/current_time"
        )
    // Don't know how to get from Crunchyroll
    override var episodeName: String = ""
    override var episodeChanged: Boolean = false

    override var secondsSinceStart: Double = 0.0
    override var secondsChanged: Boolean = false
    override var isPaused: Boolean = true

    override var isInMediaPlayer: Boolean = true
    override var isInMediaPlayerChanged: Boolean = false

    override fun updateState(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && event.packageName != "com.crunchyroll.crunchyroid"
                && event.packageName != "com.lazykernel.subsoverlay"
                && event.contentChangeTypes != AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED) {
            // Assuming crunchyroll window has been closed
            isPaused = true
            if (isInMediaPlayer) {
                isInMediaPlayer = false
                isInMediaPlayerChanged = true
            }
            return
        }

        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.className == "com.ellation.crunchyroll.presentation.content.WatchPageActivity") {
            // Assuming player entered
            if (!isInMediaPlayer) {
                isInMediaPlayer = true
                isInMediaPlayerChanged = true
            }
        }

        if (event?.eventType == TYPE_VIEW_CLICKED && event.className == "android.widget.ImageButton") {
            // Pause button was clicked, set paused state
            if (event.text.toString() == "[Pause]") {
                // unpaused
                isPaused = false
            }
            else if (event.text.toString() == "[Play]") {
                // paused
                isPaused = true
            }
        }

        if (event?.eventType == TYPE_WINDOW_CONTENT_CHANGED && event.source?.viewIdResourceName == "com.crunchyroll.crunchyroid:id/current_time") {
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