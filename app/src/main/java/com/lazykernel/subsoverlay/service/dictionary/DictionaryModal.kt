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
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lazykernel.subsoverlay.R
import com.lazykernel.subsoverlay.service.dictionary.data.DictionaryTermEntry
import com.lazykernel.subsoverlay.utils.Utils
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

        val modalLayout = mDictionaryModalLayout?.findViewById<RecyclerView>(R.id.dictionary_modal_layout)
        val entries = mDictManager.getEntryForValue(searchTerm)

        if (modalLayout != null) {
            if (entries.isNotEmpty()) {
                // Hide "No results"
                val noResultsView = mDictionaryModalLayout?.findViewById<TextView>(R.id.no_results_text)
                noResultsView?.visibility = View.GONE

                val groupingLambda = { v: DictionaryTermEntry -> v.expression ?: v.reading ?: "" }
                val entriesGrouped = entries.groupBy(groupingLambda)
                val entriesSorted = entries.sortedByDescending { v -> v.score }
                val llm = LinearLayoutManager(context)
                llm.orientation = LinearLayoutManager.VERTICAL
                modalLayout.layoutManager = llm
                modalLayout.adapter = DictionarySlugEntry(entriesGrouped, entriesSorted.map(groupingLambda))
            } else {
                // Display "No results"
                val noResultsView = mDictionaryModalLayout?.findViewById<TextView>(R.id.no_results_text)
                noResultsView?.visibility = View.VISIBLE
            }
        }

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
}