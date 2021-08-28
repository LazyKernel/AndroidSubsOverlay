package com.lazykernel.subsoverlay.service.subtitle

import android.content.Context
import android.net.Uri
import java.io.File

abstract class ISubtitleParser(val context: Context) {
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

    abstract fun pollNewEventsForRange(rangeInSeconds: ClosedFloatingPointRange<Double>): List<SubtitleEvent>
    abstract fun parseSubtitlesFromUri(fileUri: Uri)

    fun getFileRows(fileUri: Uri): List<String> {
        if (fileUri.path == null) {
            return listOf()
        }

        val inputStream = context.contentResolver.openInputStream(fileUri)
        val listOfLines = mutableListOf<String>()
        inputStream?.bufferedReader()?.forEachLine { listOfLines.add(it) }
        inputStream?.close()
        return listOfLines
    }
}