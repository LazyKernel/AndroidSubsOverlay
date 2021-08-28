package com.lazykernel.subsoverlay.service.dictionary

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lazykernel.subsoverlay.R
import com.lazykernel.subsoverlay.utils.Utils
import se.fekete.furiganatextview.furiganaview.FuriganaTextView
import java.lang.Exception

class DictionaryModal(private val context: Context, private val windowManager: WindowManager, private val onCloseDictModal: (() -> Unit)) {

    private val mDictManager = DictionaryManager(context)
    private var mDictionaryModalLayout: ConstraintLayout? = null

    fun buildDictionaryModal(searchTerm: String, yPos: Int) {
        if (mDictionaryModalLayout != null) {
            // TODO: Don't just recreate the thing, make it replace the content instead
            // Maybe use a proper list view type of a thing
            removeDictionaryModal()
        }

        mDictionaryModalLayout = View.inflate(context, R.layout.dictionary_modal, null) as ConstraintLayout
        mDictionaryModalLayout?.apply {
            setBackgroundColor(Color.WHITE)
            clipToOutline = true
        }

        mDictionaryModalLayout?.findViewById<ImageButton>(R.id.button_close_dictionary)?.setOnTouchListener { view, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                view.performClick()
                removeDictionaryModal()
                onCloseDictModal()
            }
            true
        }

        mDictionaryModalLayout?.findViewById<ImageButton>(R.id.button_close_dictionary)

        val layoutParams = WindowManager.LayoutParams()

        layoutParams.apply {
            y = yPos - Utils.dpToPixels(250F).toInt()
            width = Utils.dpToPixels(300F).toInt()
            height = Utils.dpToPixels(200F).toInt()
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            format = PixelFormat.TRANSPARENT
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }

        val modalLayout = mDictionaryModalLayout?.findViewById<LinearLayout>(R.id.dictionary_modal_layout)
        val entryView = View.inflate(context, R.layout.dict_entry, modalLayout) as LinearLayout
        val glossaryLayout = entryView.findViewById<RecyclerView>(R.id.glossary_layout)
        val slugView = entryView.findViewById<FuriganaTextView>(R.id.dict_slug)
        val entries = mDictManager.getEntryForValue(searchTerm)

        if (entries.isNotEmpty()) {
            slugView.setFuriganaText(parseReading(entries[0].expression ?: "", entries[0].reading ?: ""), true)
        }
        else {
            slugView.setFuriganaText("No results", false)
        }

        val llm = LinearLayoutManager(context)
        llm.orientation = LinearLayoutManager.VERTICAL
        glossaryLayout.layoutManager = llm
        glossaryLayout.adapter = DictionaryEntryAdapter(entries)

        try {
            windowManager.addView(mDictionaryModalLayout, layoutParams)
        }
        catch (ex: Exception) {
            Log.e("SUBSOVERLAY", "Failed adding dictionary view", ex)
        }
    }

    fun removeDictionaryModal() {
        if (mDictionaryModalLayout != null) {
            windowManager.removeView(mDictionaryModalLayout)
            mDictionaryModalLayout = null
        }
    }

    private val kanaPattern = Regex("[\\u3041-\\u309e\\uff66-\\uff9d\\u30a1-\\u30fe]+")

    private fun parseReading(expression: String, reading: String): String {
        // Some entries might not have readings
        if (reading.isEmpty()) {
            return expression
        }

        // Check if expression actually contains any kanji
        if (expression.any { c -> Character.UnicodeBlock.of(c.toInt()) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS }) {
            // First reading is usually the slug, although for some entries, the slug is just a
            // token string
            val wordParts: ArrayList<Pair<String, String>> = arrayListOf()
            val matches = kanaPattern.findAll(expression)
            // Simple replace if the word contains no kana
            if (matches.count() == 0) {
                wordParts.add(Pair(expression, reading))
            }
            else {
                // Create a new regex for selecting kana from reading corresponding to kanji in word
                var maskPattern = ""
                var lastEnd = 0

                matches.forEach { match ->
                    if (match.range.first == 0) {
                        maskPattern = match.value
                        wordParts.add(Pair(match.value, match.value))
                        lastEnd = match.range.last + 1
                    }
                    else {
                        // we want greedy searching
                        maskPattern += "(.*)" + match.value
                        wordParts.add(Pair(expression.substring(lastEnd, match.range.first), ""))
                        wordParts.add(Pair(match.value, match.value))
                        lastEnd = match.range.last + 1
                    }
                }

                val maskMatch = Regex(maskPattern).find(reading)
                var i = 0
                // If we have a match, fix word parts array, else return simple substitution
                if (maskMatch != null) {
                    maskMatch.groupValues.forEachIndexed { idx, match ->
                        // skip first element, which is the entire string
                        if (idx > 0) {
                            println(match)
                            while (i < wordParts.size && wordParts[i].second.isNotEmpty()) {
                                i++
                            }

                            if (i >= wordParts.size)
                                return@forEachIndexed

                            wordParts[i] = Pair(wordParts[i].first, match)
                        }
                    }
                }
                else {
                    wordParts.clear()
                    wordParts.add(Pair(expression, reading))
                }
            }

            return wordParts.joinToString("") { pair ->
                // if the unicode blocks aren't the same, we have kanji in first and its reading in second
                if (Character.UnicodeBlock.of(pair.first[0].toInt()) != Character.UnicodeBlock.of(pair.second[0].toInt())) {
                    "<ruby>${pair.first}<rt>${pair.second}</rt></ruby>"
                }
                else {
                    pair.first
                }
            }
        }
        else {
            return expression
        }
    }
}