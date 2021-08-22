package com.lazykernel.subsoverlay.service.dictionary

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.database.sqlite.transaction
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipFile

abstract class IDictParser(context: Context) {

    val mDBHelper = DictionaryDBHelper(context)

    var mTitle: String? = null
    var mFormat: Int? = null
    var mRevision: String? = null
    var mSequenced: Boolean? = null

    abstract fun parseDictionary(fileUri: Uri)

    fun parseIndex(fileUri: Uri): Boolean {
        val zipFile = ZipFile(fileUri.path)
        val indexEntry = zipFile.getEntry("index.json")

        if (indexEntry == null) {
            Log.e("SUBSOVERLAY", "No entry in zip file called 'index.json'")
            return false
        }

        val bufferedReader = BufferedReader(InputStreamReader(zipFile.getInputStream(indexEntry)))
        try {
            val jsonObject = JSONObject(bufferedReader.readText())
            mTitle = jsonObject.getString("title")
            mFormat = jsonObject.getInt("format")
            mRevision = jsonObject.getString("revision")
            mSequenced = jsonObject.getBoolean("sequenced")
        }
        catch (ex: JSONException) {
            Log.e("SUBSOVERLAY", "An error occurred while trying to read 'index.json' into a json object.", ex)
            return false
        }

        return true
    }

    // Returns row id, use as dictionary when inserting new entries
    fun insertNewDictionary(title: String, revision: String?, version: Int?, sequenced: Boolean? = false): Int? {
        val db = mDBHelper.writableDatabase

        val values = ContentValues().apply {
            put("title", title)
            put("revision", revision)
            put("version", version)
            put("sequenced", if (sequenced == true) 1 else 0)
        }

        // If someone has more than 2.1 something billion dictionaries
        // something's majorly broken or we're living in 2641
        return db?.insert(mDBHelper.databaseName, null, values)?.toInt()
    }

    fun insertNewTerms(dictionaryID: Int, entries: Array<DictionaryTermEntry?>) {
        val db = mDBHelper.writableDatabase

        db.transaction {
            val stmt = compileStatement(
            "INSERT INTO dict_terms (" +
                "expression," +
                "reading," +
                "definition_tags," +
                "rules," +
                "score," +
                "glossary," +
                "sequence," +
                "term_tags" +
                "dictionary" +
            ")" +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )

            entries.forEach {
                if (it != null) {
                    stmt.clearBindings()
                    stmt.bindString(1, it.expression)
                    stmt.bindString(2, it.reading)
                    stmt.bindString(3, it.definitionTags)
                    stmt.bindString(4, it.rules)
                    stmt.bindLong(5, it.score ?: -1)
                    stmt.bindString(6, JSONArray(it.glossary).toString())
                    stmt.bindLong(7, it.sequence ?: -1)
                    stmt.bindString(8, it.termTags)
                    stmt.bindLong(9, dictionaryID.toLong())
                    stmt.executeInsert()
                }
            }
        }
    }
}