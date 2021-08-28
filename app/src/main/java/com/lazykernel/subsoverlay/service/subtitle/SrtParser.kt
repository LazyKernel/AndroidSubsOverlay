package com.lazykernel.subsoverlay.service.subtitle

import android.content.Context
import android.net.Uri
import android.util.Log

class SrtParser(context: Context) : ISubtitleParser(context) {
    private enum class ParsingState {
        COUNTER_LINE,
        TIMING_LINE,
        SUBTITLE_LINES
    }

    private val timingRegex = Regex(
        "(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3}) --> (\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})"
    )

    private val subtitleLines = mutableListOf<Subtitle>()

    override fun pollNewEventsForRange(rangeInSeconds: ClosedFloatingPointRange<Double>): List<SubtitleEvent> {
        // There probably aren't going to be enough subtitles for this to be a problem
        val subtitleEvents = mutableListOf<SubtitleEvent>()
        subtitleLines.forEach lit@{
            if (rangeInSeconds.contains(it.startTime)) {
                // Subtitle start between range, add show event
                val event = SubtitleEvent()
                event.apply {
                    subtitle = it
                    type = SubtitleEventType.SUBTITLE_SHOW
                }
                subtitleEvents.add(event)
            }

            if (rangeInSeconds.contains(it.endTime)) {
                // Subtitle end between range, add remove event
                val event = SubtitleEvent()
                event.apply {
                    subtitle = it
                    type = SubtitleEventType.SUBTITLE_REMOVE
                }
                subtitleEvents.add(event)
            }
        }
        return subtitleEvents
    }

    override fun parseSubtitlesFromUri(fileUri: Uri) {
        val rows = getFileRows(fileUri)
        var parsingState = ParsingState.COUNTER_LINE
        lateinit var currentSubtitle: Subtitle

        rows.forEach { row ->
            when (parsingState) {
                ParsingState.COUNTER_LINE -> {
                    // Create new subtitle
                    currentSubtitle = Subtitle()
                    currentSubtitle.id = row.toInt()
                    parsingState = ParsingState.TIMING_LINE
                }
                ParsingState.TIMING_LINE -> {
                    // Parse timing
                    val match = timingRegex.find(row)
                    if (match != null) {
                        val values = match.groups.drop(1).map { it?.value?.toInt() ?: 0 }
                        currentSubtitle.apply {
                            startTime = values[0] * 3600 + values[1] * 60 + values[2] + values[3] / 1000.0
                            endTime = values[4] * 3600 + values[5] * 60 + values[6] + values[7] / 1000.0
                        }
                    }
                    parsingState = ParsingState.SUBTITLE_LINES
                }
                ParsingState.SUBTITLE_LINES -> {
                    if (row.trim().isEmpty()) {
                        subtitleLines.add(currentSubtitle)
                        parsingState = ParsingState.COUNTER_LINE
                    }
                    else {
                        currentSubtitle.text += row
                    }
                }
            }
        }
        Log.i("SUBSOVERLAY", "sub lines: ${subtitleLines.map { "${it.startTime}..${it.endTime}:  ${it.text}" }}")
    }
}