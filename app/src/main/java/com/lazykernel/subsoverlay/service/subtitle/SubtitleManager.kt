package com.lazykernel.subsoverlay.service.subtitle

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannedString
import android.text.style.BackgroundColorSpan
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
import com.lazykernel.subsoverlay.utils.Utils

class SubtitleManager(private val applicationContext: Context, private val windowManager: WindowManager) {
    var currentTimeInSeconds: Double = 0.0
    lateinit var subtitleLayout: LinearLayout
    val tokenizer = Tokenizer()
    lateinit var mSubtitleTextView: TextView
    lateinit var mSpanRange: IntRange

    fun buildSubtitleView()  {
        subtitleLayout = LinearLayout(applicationContext)
        mSubtitleTextView = TextView(applicationContext)
        mSubtitleTextView.apply {
            id = R.id.subsTextView
            text = SpannedString("何できるのかな～何できるのかな～何できるのかな～何できるのかな～何できるのかな～何できるのかな～何できるのかな～")
            textSize = 20F
            setTextColor(Color.WHITE)
            setBackgroundColor(0xAA000000.toInt())
            gravity = Gravity.CENTER_HORIZONTAL
        }
        subtitleLayout.addView(mSubtitleTextView)

        val layoutParams = LayoutParams()
        layoutParams.apply {
            y = Utils.dpToPixels(50F).toInt()
            height = LayoutParams.WRAP_CONTENT
            width = LayoutParams.WRAP_CONTENT
            type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            format = PixelFormat.TRANSPARENT
            flags = LayoutParams.FLAG_NOT_FOCUSABLE
        }

        mSubtitleTextView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val (word, spanRange) = getWordFromTouchEvent(mSubtitleTextView, event)
                Log.i("SUBSOVERLAY", "word: $word span: $spanRange")
                if (word != null && spanRange != null) {
                    setTextSpan(word, spanRange)
                    openSubtitleAdjustWindow(event.rawX.toInt(), event.rawY.toInt())
                }
            }
            true
        }

        try {
            windowManager.addView(subtitleLayout, layoutParams)
        }
        catch (ex: Exception) {
            Log.e("SUBSOVERLAY", "adding subs view failed", ex)
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

    fun openSubtitleAdjustWindow(xPos: Int, yPos: Int) {
        val subtitleAdjustLayout = View.inflate(applicationContext, R.layout.subtitle_adjust, null) as ConstraintLayout
        subtitleAdjustLayout.apply {
            setBackgroundColor(Color.WHITE)
        }

        val layoutParams = LayoutParams()

        val coords = IntArray(2)
        mSubtitleTextView.getLocationOnScreen(coords)

        layoutParams.apply {
            y = coords[1] - Utils.dpToPixels(50F).toInt()
            width = Utils.dpToPixels(300F).toInt()
            height = Utils.dpToPixels(50F).toInt()
            type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSPARENT
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            flags = LayoutParams.FLAG_NOT_FOCUSABLE
        }

        subtitleAdjustLayout.findViewById<ImageButton>(R.id.subtitle_select_left).setOnTouchListener{ _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                if (mSpanRange.first <= 0) {
                    return@setOnTouchListener true
                }

                val newSpan = IntRange(mSpanRange.first - 1, mSpanRange.last - 1)
                val word = mSubtitleTextView.text.subSequence(newSpan.first, newSpan.last)
                Log.i("SUBSOVERLAY", "word is $word")
                setTextSpan((word as SpannedString).toString(), newSpan)
            }
            true
        }

        subtitleAdjustLayout.findViewById<ImageButton>(R.id.subtitle_select_right).setOnTouchListener{ _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                if (mSpanRange.last >= mSubtitleTextView.text.length) {
                    return@setOnTouchListener true
                }

                val newSpan = IntRange(mSpanRange.first + 1, mSpanRange.last + 1)
                val word = mSubtitleTextView.text.subSequence(newSpan.first, newSpan.last)
                Log.i("SUBSOVERLAY", "word is $word")
                setTextSpan((word as SpannedString).toString(), newSpan)
            }
            true
        }

        subtitleAdjustLayout.findViewById<Button>(R.id.subtitle_select_more).setOnTouchListener{ _, event ->
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
                Log.i("SUBSOVERLAY", "word is $word")
                setTextSpan((word as SpannedString).toString(), newSpan)
            }
            true
        }

        subtitleAdjustLayout.findViewById<Button>(R.id.subtitle_select_less).setOnTouchListener{ _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                if (mSpanRange.last - mSpanRange.first > 1) {
                    val newSpan = IntRange(mSpanRange.first, mSpanRange.last - 1)
                    val word = mSubtitleTextView.text.subSequence(newSpan.first, newSpan.last)
                    Log.i("SUBSOVERLAY", "word is $word")
                    setTextSpan((word as SpannedString).toString(), newSpan)
                }
            }
            true
        }

        try {
            windowManager.addView(subtitleAdjustLayout, layoutParams)
        }
        catch (ex: Exception) {
            Log.e("SUBSOVERLAY", "adding subs adjust view failed", ex)
        }
    }

    fun setTextSpan(word: String, spanRange: IntRange) {
        val spannableString = SpannableString(mSubtitleTextView.text)
        spannableString.clearSpans()
        spannableString.setSpan(
            BackgroundColorSpan(Color.DKGRAY),
            spanRange.first, spanRange.last,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        mSpanRange = spanRange
        mSubtitleTextView.text = spannableString
    }
}