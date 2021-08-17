package com.lazykernel.subsoverlay.service.subtitle

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannedString
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.view.WindowManager.LayoutParams
import androidx.core.text.clearSpans
import com.atilika.kuromoji.ipadic.Tokenizer
import com.lazykernel.subsoverlay.R
import com.lazykernel.subsoverlay.service.Utils

class SubtitleManager(private val applicationContext: Context) {
    var currentTimeInSeconds: Double = 0.0
    lateinit var subtitleLayout: LinearLayout
    val tokenizer = Tokenizer()

    fun buildSubtitleView(): Pair<LinearLayout, LayoutParams>  {
        subtitleLayout = LinearLayout(applicationContext)
        val textView = TextView(applicationContext)
        textView.apply {
            id = R.id.subsTextView
            text = SpannedString("何できるのかな～何できるのかな～何できるのかな～何できるのかな～何できるのかな～何できるのかな～何できるのかな～")
            textSize = 20F
            setTextColor(Color.WHITE)
            setBackgroundColor(0xAA000000.toInt())
            gravity = Gravity.CENTER_HORIZONTAL
        }
        subtitleLayout.addView(textView)

        val layoutParams = WindowManager.LayoutParams()
        layoutParams.apply {
            y = Utils.dpToPixels(50F).toInt()
            height = LayoutParams.WRAP_CONTENT
            width = LayoutParams.WRAP_CONTENT
            type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            format = PixelFormat.TRANSPARENT
            flags = LayoutParams.FLAG_NOT_FOCUSABLE
        }

        textView.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                (view as TextView)
                val (word, spanRange) = getWordFromTouchEvent(view, event)
                Log.i("SUBSOVERLAY", "word: $word span: $spanRange")
                if (word != null && spanRange != null) {
                    val spannableString = SpannableString(view.text)
                    spannableString.clearSpans()
                    spannableString.setSpan(
                        BackgroundColorSpan(Color.DKGRAY),
                        spanRange.start, spanRange.last,
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                    view.text = spannableString
                }
            }
            true
        }

        return Pair(subtitleLayout, layoutParams)
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
            val words = tokenizer.tokenize((view.text as SpannedString).toString())
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
}