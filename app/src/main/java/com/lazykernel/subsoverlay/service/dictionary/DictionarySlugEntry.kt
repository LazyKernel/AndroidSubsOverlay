package com.lazykernel.subsoverlay.service.dictionary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lazykernel.subsoverlay.R
import com.lazykernel.subsoverlay.service.dictionary.data.DictionaryTermEntry
import se.fekete.furiganatextview.furiganaview.FuriganaTextView

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
        holder.slugView.setFuriganaText(parseReading(key, reading), true)

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