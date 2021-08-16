package com.lazykernel.subsoverlay.service.subtitle

import android.net.Uri

class SrtParser : ISubtitleParser {
    private enum class ParsingState {
        COUNTER_LINE,
        TIMING_LINE,
        SUBTITLE_LINES
    }

    private val timingRegex = Regex(
        "(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3}) --> (\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})"
    )

    private val subtitleLines = mutableListOf<ISubtitleParser.Subtitle>()

    override fun pollNewEventsForRange(rangeInSeconds: ClosedFloatingPointRange<Double>): List<ISubtitleParser.SubtitleEvent> {
        // There probably aren't going to be enough subtitles for this to be a problem
        val subtitleEvents = mutableListOf<ISubtitleParser.SubtitleEvent>()
        subtitleLines.forEach lit@{
            if (it.endTime > rangeInSeconds.endInclusive) {
                // Subs in order, no more subs in range
                return@lit
            }

            if (rangeInSeconds.contains(it.startTime)) {
                // Subtitle start between range, add show event
                val event = ISubtitleParser.SubtitleEvent()
                event.apply {
                    subtitle = it
                    type = ISubtitleParser.SubtitleEventType.SUBTITLE_SHOW
                }
                subtitleEvents.add(event)
            }

            if (rangeInSeconds.contains(it.endTime)) {
                // Subtitle end between range, add remove event
                val event = ISubtitleParser.SubtitleEvent()
                event.apply {
                    subtitle = it
                    type = ISubtitleParser.SubtitleEventType.SUBTITLE_REMOVE
                }
                subtitleEvents.add(event)
            }
        }
        return subtitleEvents
    }

    override fun parseSubtitlesFromUri(fileUri: Uri) {
        val rows = getFileRows(fileUri)
        var parsingState = ParsingState.COUNTER_LINE
        lateinit var currentSubtitle: ISubtitleParser.Subtitle

        rows.forEach { row ->
            when (parsingState) {
                ParsingState.COUNTER_LINE -> {
                    // Create new subtitle
                    currentSubtitle = ISubtitleParser.Subtitle()
                    currentSubtitle.id = row.toInt()
                    parsingState = ParsingState.TIMING_LINE
                }
                ParsingState.TIMING_LINE -> {
                    // Parse timing
                    val match = timingRegex.find(row)
                    if (match != null) {
                        val values = match.groups.map { it?.value?.toInt() ?: 0 }
                        currentSubtitle.apply {
                            startTime = values[1] * 3600 + values[2] * 60 + values[3] + values[4] / 1000.0
                            endTime = values[5] * 3600 + values[6] * 60 + values[7] + values[8] / 1000.0
                        }
                    }
                    parsingState = ParsingState.SUBTITLE_LINES
                }
                ParsingState.SUBTITLE_LINES -> {
                    if (row.trim().isEmpty()) {
                        subtitleLines.add(currentSubtitle)
                    }
                    else {
                        currentSubtitle.text += row
                    }
                }
            }
        }
    }
}