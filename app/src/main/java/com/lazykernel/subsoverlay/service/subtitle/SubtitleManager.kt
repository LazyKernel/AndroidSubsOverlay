package com.lazykernel.subsoverlay.service.subtitle

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannedString
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import android.view.WindowManager.LayoutParams
import android.widget.Button
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.clearSpans
import com.atilika.kuromoji.ipadic.Tokenizer
import com.lazykernel.subsoverlay.R
import com.lazykernel.subsoverlay.service.dictionary.DictionaryModal
import com.lazykernel.subsoverlay.utils.Utils

class SubtitleManager(private val applicationContext: Context, private val windowManager: WindowManager) {
    var currentTimeInSeconds: Double = 0.0
        set(value) {
            if (value < oldTimeInSeconds) {
                // Skipped backwards
                oldTimeInSeconds = value - 1.0
                field = value
            }
            else {
                oldTimeInSeconds = currentTimeInSeconds
                field = value
            }
        }
    var oldTimeInSeconds: Double = 0.0
    private val subtitlesShown = mutableListOf<ISubtitleParser.Subtitle>()
    lateinit var mSubtitleLayout: LinearLayout
    var mTokenizer: Tokenizer? = null
    lateinit var mSubtitleTextView: TextView
    lateinit var mSubtitleLayoutParams: LayoutParams
    lateinit var mSpanRange: IntRange
    private val mSubParser = SrtParser(applicationContext)
    var mSubtitleAdjustLayout: ConstraintLayout? = null
    private val mDictionaryModal = DictionaryModal(applicationContext, windowManager) {
        clearSubtitleView()
    }

    fun buildSubtitleView()  {
        mSubtitleLayout = LinearLayout(applicationContext)
        mSubtitleTextView = TextView(applicationContext)
        mSubtitleTextView.apply {
            id = R.id.subsTextView
            text = SpannedString("")
            textSize = applicationContext.resources.getDimension(R.dimen._6ssp)
            setTextColor(Color.WHITE)
            setShadowLayer(3F, 3F, 3F, Color.BLACK)
            setPadding(1, 1, 5, 5)
        }
        mSubtitleLayout.apply {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(mSubtitleTextView)
        }

        mSubtitleLayoutParams = LayoutParams()
        mSubtitleLayoutParams.apply {
            y = applicationContext.resources.getDimension(R.dimen._40sdp).toInt()
            height = LayoutParams.WRAP_CONTENT
            width = LayoutParams.MATCH_PARENT
            type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            format = PixelFormat.TRANSPARENT
            flags = LayoutParams.FLAG_NOT_FOCUSABLE
        }

        mSubtitleTextView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val (word, spanRange) = getWordFromTouchEvent(mSubtitleTextView, event)
                if (word != null && spanRange != null) {
                    setTextSpan(word, spanRange)
                    // Open subtitle adjust layout if it doesn't exist
                    if (mSubtitleAdjustLayout == null) {
                        openSubtitleAdjustWindow()
                    }
                }
            }
            true
        }
    }

    fun getWordFromTouchEvent(view: View, event: MotionEvent): Pair<String?, IntRange?> {
        // Span guaranteed to be in range, if not null
        val layout = (view as TextView).layout
        if (layout != null) {
            // This is somewhat inaccurate (even more pronounced for full width characters)
            // it'll do for now since were picking words and people usually click in the
            // middle of words they wanna see
            val line = layout.getLineForVertical(event.y.toInt())
            var offset = layout.getOffsetForHorizontal(line, event.x)
            if (offset >= view.text.length) {
                // Just don't show anything in the future?
                offset = view.text.length - 1
            }
            val words = mTokenizer!!.tokenize((view.text as SpannedString).toString())
            var selectedWord: String
            var spanIdx: IntRange
            words.forEachIndexed { idx, token ->
                if (token.position <= offset) {
                    val nextToken = if (idx < words.size - 1) words[idx + 1] else null

                    if (nextToken == null) {
                        // Last token, has to be the one
                        selectedWord = token.baseForm
                        spanIdx = IntRange(token.position, view.text.length)
                        return Pair(selectedWord, spanIdx)
                    }
                    else if (nextToken.position > offset) {
                        selectedWord = token.baseForm
                        spanIdx = IntRange(token.position, nextToken.position)
                        return Pair(selectedWord, spanIdx)
                    }
                }
            }
        }

        return Pair(null, null)
    }

    fun openSubtitleAdjustWindow() {
        mSubtitleAdjustLayout = View.inflate(applicationContext, R.layout.subtitle_adjust, null) as ConstraintLayout
        mSubtitleAdjustLayout?.apply {
            setBackgroundColor(Color.WHITE)
        }

        val layoutParams = LayoutParams()

        val coords = IntArray(2)
        mSubtitleTextView.getLocationOnScreen(coords)

        layoutParams.apply {
            y = coords[1] - applicationContext.resources.getDimension(R.dimen._40sdp).toInt()
            width = applicationContext.resources.getDimension(R.dimen._250sdp).toInt()
            height = applicationContext.resources.getDimension(R.dimen._40sdp).toInt()
            type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSPARENT
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            flags = LayoutParams.FLAG_NOT_FOCUSABLE
        }

        mSubtitleAdjustLayout?.findViewById<ImageButton>(R.id.subtitle_select_left)?.setOnTouchListener{ _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                if (mSpanRange.first <= 0) {
                    return@setOnTouchListener true
                }

                val newSpan = IntRange(mSpanRange.first - 1, mSpanRange.last - 1)
                val word = mSubtitleTextView.text.subSequence(newSpan.first, newSpan.last)
                setTextSpan((word as SpannedString).toString(), newSpan)
            }
            true
        }

        mSubtitleAdjustLayout?.findViewById<ImageButton>(R.id.subtitle_select_right)?.setOnTouchListener{ _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                if (mSpanRange.last >= mSubtitleTextView.text.length) {
                    return@setOnTouchListener true
                }

                val newSpan = IntRange(mSpanRange.first + 1, mSpanRange.last + 1)
                val word = mSubtitleTextView.text.subSequence(newSpan.first, newSpan.last)
                setTextSpan((word as SpannedString).toString(), newSpan)
            }
            true
        }

        mSubtitleAdjustLayout?.findViewById<Button>(R.id.subtitle_select_more)?.setOnTouchListener{ _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                var newSpan: IntRange = when {
                    mSpanRange.last < mSubtitleTextView.text.length -> {
                        IntRange(mSpanRange.first, mSpanRange.last + 1)
                    }
                    mSpanRange.first > 0 -> {
                        IntRange(mSpanRange.first - 1, mSpanRange.last)
                    }
                    else -> {
                        return@setOnTouchListener true
                    }
                }

                val word = mSubtitleTextView.text.subSequence(newSpan.first, newSpan.last)
                setTextSpan((word as SpannedString).toString(), newSpan)
            }
            true
        }

        mSubtitleAdjustLayout?.findViewById<Button>(R.id.subtitle_select_less)?.setOnTouchListener{ _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                if (mSpanRange.last - mSpanRange.first > 1) {
                    val newSpan = IntRange(mSpanRange.first, mSpanRange.last - 1)
                    val word = mSubtitleTextView.text.subSequence(newSpan.first, newSpan.last)
                    setTextSpan((word as SpannedString).toString(), newSpan)
                }
            }
            true
        }

        try {
            windowManager.addView(mSubtitleAdjustLayout, layoutParams)
        }
        catch (ex: Exception) {
            Log.e("SUBSOVERLAY", "adding subs adjust view failed", ex)
        }
    }

    fun runSubtitleUpdate() {
        val events = mSubParser.pollNewEventsForRange(oldTimeInSeconds..currentTimeInSeconds)
        events.forEach { event ->
            Log.i("SUBSOVERLAY", "event: ${event.type}, ${event.subtitle.text}")
            if (event.type == ISubtitleParser.SubtitleEventType.SUBTITLE_SHOW) {
                subtitlesShown.add(event.subtitle)
            }
            else if (event.type == ISubtitleParser.SubtitleEventType.SUBTITLE_REMOVE) {
                val idx = subtitlesShown.indexOfFirst { s -> s.id == event.subtitle.id }
                if (idx >= 0) {
                    subtitlesShown.removeAt(idx)
                }
            }
        }

        // quick sanity check, shouldn't take too much time
        // above breaks when skipping on the timeline
        // TODO: fix pollNewEventsForRange, probably rethink the entire system
        subtitlesShown.retainAll { sub ->
            sub.startTime <= currentTimeInSeconds && sub.endTime >= currentTimeInSeconds
        }

        // Join to string requires higher android version
        updateSubtitleLine(subtitlesShown.fold("") { acc, subtitle -> acc + subtitle.text })
    }

    private fun updateSubtitleLine(newLine: String) {
        mSubtitleTextView.text = SpannableString(newLine)
    }

    private fun setTextSpan(word: String, spanRange: IntRange) {
        val spannableString = SpannableString(mSubtitleTextView.text)
        spannableString.clearSpans()
        spannableString.setSpan(
            BackgroundColorSpan(Color.DKGRAY),
            spanRange.first, spanRange.last,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        mSpanRange = spanRange
        mSubtitleTextView.text = spannableString

        val coords = IntArray(2)
        mSubtitleTextView.getLocationOnScreen(coords)
        mDictionaryModal.buildDictionaryModal(word, coords[1])
    }

    private fun clearSubtitleView() {
        val spannableString = SpannableString(mSubtitleTextView.text)
        spannableString.clearSpans()
        mSubtitleTextView.text = spannableString

        if (mSubtitleAdjustLayout != null) {
            windowManager.removeView(mSubtitleAdjustLayout)
            mSubtitleAdjustLayout = null
        }
    }

    fun openDefaultViews() {
        // Handling creation here, since it takes a few seconds
        mTokenizer = Tokenizer()

        try {
            if (!mSubtitleLayout.isAttachedToWindow) {
                windowManager.addView(mSubtitleLayout, mSubtitleLayoutParams)
            }
        }
        catch (ex: Exception) {
            Log.e("SUBSOVERLAY", "adding subs view failed", ex)
        }
    }

    fun closeAll() {
        if (mSubtitleLayout.isAttachedToWindow) {
            windowManager.removeView(mSubtitleLayout)
        }

        if (mSubtitleAdjustLayout != null) {
            windowManager.removeView(mSubtitleAdjustLayout)
            mSubtitleAdjustLayout = null
        }
        // GC should delete
        mTokenizer = null
    }

    fun loadSubtitlesFromUri(fileUri: Uri) {
        mSubParser.parseSubtitlesFromUri(fileUri)
    }
}