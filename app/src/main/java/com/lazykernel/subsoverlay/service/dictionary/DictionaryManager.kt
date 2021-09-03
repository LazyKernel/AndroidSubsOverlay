package com.lazykernel.subsoverlay.service.dictionary

import android.content.Context
import android.net.Uri
import android.util.Log
import com.lazykernel.subsoverlay.service.dictionary.data.DictionaryDBHelper
import com.lazykernel.subsoverlay.service.dictionary.data.DictionaryEntry
import com.lazykernel.subsoverlay.service.dictionary.data.DictionaryTermEntry
import org.json.JSONArray

class DictionaryManager(context: Context) {
    val mDBHelper = DictionaryDBHelper(context)
    val mDictParser: IDictParser = YomichanParser(context)

    fun addNewDictionary(fileUri: Uri) {
        mDictParser.parseDictionaryToDB(fileUri)
    }

    fun deleteDictionary(id: Int) {
        val db = mDBHelper.writableDatabase
        val deletedRows = db.delete("dict_dictionaries", "id = ?", arrayOf(id.toString()))
        Log.i("DICTIONARYMANAGER", "Deleted $deletedRows row(s)")
    }

    fun getDictionaries() : List<DictionaryEntry> {
        val db = mDBHelper.readableDatabase
        val projection = arrayOf("id", "title", "revision", "version", "sequenced")
        val sortBy = "id ASC"
        val cursor = db.query("dict_dictionaries", projection, null, null, null, null, sortBy)

        val entries = mutableListOf<DictionaryEntry>()
        with(cursor) {
            while (moveToNext()) {
                val dictID = getInt(getColumnIndexOrThrow("id"))
                val title = getString(getColumnIndexOrThrow("title"))
                val revision = getString(getColumnIndexOrThrow("revision"))
                val version = getInt(getColumnIndexOrThrow("version"))
                val sequenced = getInt(getColumnIndexOrThrow("sequenced"))
                entries.add(DictionaryEntry(dictID, title, revision, version, sequenced == 1))
            }
        }
        return entries
    }

    // Value, either reading or expression
    fun getEntryForValue(value: String): List<DictionaryTermEntry> {
        val db = mDBHelper.readableDatabase
        val entryQuery = "SELECT t.*, d.title FROM dict_terms t LEFT JOIN dict_dictionaries d ON t.dictionary = d.id WHERE t.expression = ? OR t.reading = ?"
        val cursor = db.rawQuery(entryQuery, arrayOf(value, value))

        val entries = mutableListOf<DictionaryTermEntry>()
        with(cursor) {
            while (moveToNext()) {
                val entryID = getLong(getColumnIndexOrThrow("id"))
                val expression = getString(getColumnIndexOrThrow("expression"))
                val reading = getString(getColumnIndexOrThrow("reading"))
                val definitionTags = getString(getColumnIndexOrThrow("definition_tags"))
                val rules = getString(getColumnIndexOrThrow("rules"))
                val score = getLong(getColumnIndexOrThrow("score"))
                val glossary = JSONArray(getString(getColumnIndexOrThrow("glossary")))
                val sequence = getLong(getColumnIndexOrThrow("sequence"))
                val termTags = getString(getColumnIndexOrThrow("term_tags"))
                val dictionary = getString(getColumnIndexOrThrow("title"))
                entries.add(DictionaryTermEntry(expression, reading, definitionTags, rules, score, glossary, sequence, termTags, entryID, dictionary))
            }
        }
        return entries
    }
}