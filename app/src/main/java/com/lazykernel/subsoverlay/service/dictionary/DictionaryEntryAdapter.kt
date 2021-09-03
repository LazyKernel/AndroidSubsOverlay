package com.lazykernel.subsoverlay.service.dictionary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lazykernel.subsoverlay.R
import com.lazykernel.subsoverlay.service.dictionary.data.DictionaryTermEntry

class DictionaryEntryAdapter(private val entries: List<DictionaryTermEntry>) : RecyclerView.Adapter<DictionaryEntryAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val orderNumberView: TextView
        val dictNameView: TextView
        val glossaryTextView: TextView

        init {
            orderNumberView = view.findViewById(R.id.glossary_order_number)
            dictNameView = view.findViewById(R.id.glossary_dict_name)
            glossaryTextView = view.findViewById(R.id.glossary_text)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.dict_glossary_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val e = entries[position]

        var glossaryString = ""
        if (e.glossary.length() > 0) {
            glossaryString = e.glossary.getString(0)
            for (i in 1 until e.glossary.length()) {
                val glossary = e.glossary.getString(i)
                glossaryString += "\n$glossary"
            }
        }

        holder.orderNumberView.setText("${position + 1}.")
        holder.dictNameView.setText(e.dictionary)
        holder.glossaryTextView.setText(glossaryString)
    }

    override fun getItemCount(): Int {
        return entries.size
    }

    override fun getItemId(position: Int): Long {
        return entries[position].entryID ?: -1
    }

}