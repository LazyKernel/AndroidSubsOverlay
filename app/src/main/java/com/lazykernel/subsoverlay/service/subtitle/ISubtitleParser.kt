package com.lazykernel.subsoverlay.service.subtitle

import android.net.Uri
import java.io.File

interface ISubtitleParser {
    class Subtitle {
        var id: Int = -1
        var text: String = ""
        var startTime: Double = -1.0
        var endTime: Double = -1.0
    }

    enum class SubtitleEventType {
        SUBTITLE_UNKNOWN,
        SUBTITLE_SHOW,
        SUBTITLE_REMOVE
    }

    class SubtitleEvent {
        var subtitle = Subtitle()
        var type = SubtitleEventType.SUBTITLE_UNKNOWN
    }

    fun pollNewEventsForRange(rangeInSeconds: ClosedFloatingPointRange<Double>): List<SubtitleEvent>
    fun parseSubtitlesFromUri(fileUri: Uri)

    fun getFileRows(fileUri: Uri): List<String> {
        if (fileUri.path == null) {
            return listOf()
        }

        val inputStream = File(fileUri.path).inputStream()
        val listOfLines = mutableListOf<String>()
        inputStream.bufferedReader().forEachLine { listOfLines.add(it) }
        return listOfLines
    }
}