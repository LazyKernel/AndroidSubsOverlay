package com.lazykernel.subsoverlay.service.dictionary

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lazykernel.subsoverlay.R
import com.lazykernel.subsoverlay.service.dictionary.data.DictionaryTermEntry
import se.fekete.furiganatextview.furiganaview.FuriganaTextView
import kotlin.math.exp

class DictionarySlugEntry(private val entries: Map<String, List<DictionaryTermEntry>>, private val entriesOrder: List<String>) : RecyclerView.Adapter<DictionarySlugEntry.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val slugView: FuriganaTextView
        val glossaryLayout: RecyclerView

        init {
            slugView = view.findViewById(R.id.dict_slug)
            glossaryLayout = view.findViewById(R.id.glossary_layout)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.dict_entry, parent, false)
        val holder = ViewHolder(view)

        val llm = LinearLayoutManager(view.context)
        llm.orientation = LinearLayoutManager.VERTICAL
        holder.glossaryLayout.layoutManager = llm
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val key = entriesOrder[position]
        val list = entries[key] ?: return

        val reading = list.find { v -> v.reading != null && v.reading.isNotBlank() }?.reading ?: ""
        val parsedReading = parseReading(key, reading)
        // Extremely dumb hack to get furigana text view to overflow furigana to right instead of
        // to the left and out of view
        Log.d("SUBSOVERLAY", parsedReading)
        holder.slugView.setFuriganaText("$parsedReading <ruby><rt> </rt></ruby>", true)

        holder.glossaryLayout.adapter = DictionaryEntryAdapter(list)
    }

    override fun getItemCount(): Int {
        return entries.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }


    private val kanaPattern = Regex("[\\u3041-\\u309e\\uff66-\\uff9d\\u30a1-\\u30fe]+")

    private fun parseReading(expression: String, reading: String): String {
        // Some entries might not have readings
        if (reading.isEmpty()) {
            return expression
        }

        Log.d("SUBSOVERLAY", "exp: $expression read: $reading")
        // Check if expression actually contains any kanji
        if (expression.any { c -> Character.UnicodeBlock.of(c.toInt()) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS }) {
            // First reading is usually the slug, although for some entries, the slug is just a
            // token string
            val wordParts: ArrayList<Pair<String, String>> = arrayListOf()
            val matches = kanaPattern.findAll(expression)
            var totalExpression = 0
            var totalReading = 0
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
                        totalExpression += match.value.length
                        totalReading += match.value.length
                    }
                    else {
                        // we want greedy searching
                        maskPattern += "(.*)" + match.value
                        wordParts.add(Pair(expression.substring(lastEnd, match.range.first), ""))
                        wordParts.add(Pair(match.value, match.value))
                        lastEnd = match.range.last + 1
                        totalExpression += match.value.length
                        totalReading += match.value.length
                    }
                }

                val maskMatch = Regex(maskPattern).find(reading)
                var i = 0
                // If we have a match, fix word parts array, else return simple substitution
                if (maskMatch != null) {
                    maskMatch.groupValues.forEachIndexed { idx, match ->
                        // skip first element, which is the entire string
                        if (idx > 0) {
                            while (i < wordParts.size && wordParts[i].second.isNotEmpty()) {
                                i++
                            }

                            if (i >= wordParts.size)
                                return@forEachIndexed

                            totalExpression += wordParts[i].first.length
                            totalReading += match.length
                            wordParts[i] = Pair(wordParts[i].first, match)
                        }
                    }
                    // Check for trailing characters
                    if (totalExpression < expression.length && totalReading < reading.length) {
                        val trailingExpression = expression.substring(totalExpression)
                        val trailingReading = reading.substring(totalReading)
                        wordParts.add(Pair(trailingExpression, trailingReading))
                    }
                    else if (totalExpression < expression.length) {
                        val trailing = expression.substring(totalExpression)
                        wordParts.add(Pair(trailing, trailing))
                    }
                    else if (totalReading < reading.length) {
                        val trailing = reading.substring(totalReading)
                        wordParts.add(Pair(trailing, trailing))
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