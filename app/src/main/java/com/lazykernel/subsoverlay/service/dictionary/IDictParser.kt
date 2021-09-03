package com.lazykernel.subsoverlay.service.dictionary

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.database.sqlite.transaction
import com.lazykernel.subsoverlay.service.dictionary.data.DictionaryDBHelper
import com.lazykernel.subsoverlay.service.dictionary.data.DictionaryTermEntry
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

abstract class IDictParser(val context: Context) {

    val mDBHelper = DictionaryDBHelper(context)

    var mTitle: String? = null
    var mFormat: Int? = null
    var mRevision: String? = null
    var mSequenced: Boolean? = null

    abstract fun parseDictionaryToDB(fileUri: Uri)

    fun parseIndex(fileUri: Uri): Boolean {
        val inputStream = context.contentResolver.openInputStream(fileUri)
        val zipStream = ZipInputStream(inputStream)

        var entry = zipStream.nextEntry
        var foundIndex = false
        while (entry != null) {
            if (entry.name == "index.json") {
                foundIndex = true
                try {
                    val jsonObject = JSONObject(zipEntryToString(zipStream))
                    mTitle = jsonObject.getString("title")
                    mFormat = jsonObject.getInt("format")
                    mRevision = jsonObject.getString("revision")
                    mSequenced = jsonObject.getBoolean("sequenced")
                }
                catch (ex: JSONException) {
                    Log.e("SUBSOVERLAY", "An error occurred while trying to read 'index.json' into a json object.", ex)
                    zipStream.close()
                    return false
                }
                finally {
                    zipStream.closeEntry()
                }
                break
            }
            zipStream.closeEntry()
            entry = zipStream.nextEntry
        }

        if (!foundIndex) {
            Log.e("SUBSOVERLAY", "No entry in zip file called 'index.json'")
            zipStream.close()
            return false
        }

        zipStream.close()
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
        return db?.insert("dict_dictionaries", null, values)?.toInt()
    }

    fun insertNewTerms(dictionaryID: Int, entries: Array<DictionaryTermEntry?>) {
        val db = mDBHelper.writableDatabase

        Log.i("SUBSOVERLAY", "Inserting ${entries.size} new terms")
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
                "term_tags," +
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
                    stmt.bindString(6, it.glossary.toString())
                    stmt.bindLong(7, it.sequence ?: -1)
                    stmt.bindString(8, it.termTags)
                    stmt.bindLong(9, dictionaryID.toLong())
                    stmt.executeInsert()
                }
            }
        }
    }

    fun zipEntryToString(zipStream: ZipInputStream): String {
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var read = zipStream.read(buffer, 0, buffer.size)
        while (read > 0) {
            baos.write(buffer, 0, read)
            read = zipStream.read(buffer, 0, buffer.size)
        }
        return baos.toString()
    }
}