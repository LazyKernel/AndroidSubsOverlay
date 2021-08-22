package com.lazykernel.subsoverlay.service.dictionary

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.util.zip.ZipFile

class YomichanParser(context: Context) : IDictParser(context) {

    override fun parseDictionary(fileUri: Uri) {
        if (!parseIndex(fileUri)) {
            // An error occurred and it has been logged
            return
        }

        if (mTitle == null) {
            Log.e("SUBSOVERLAY", "Dictionary had no title, cannot continue")
            return
        }

        val dictionaryID = insertNewDictionary(mTitle!!, mRevision, mFormat, mSequenced)

        if (dictionaryID == null) {
            Log.e("SUBSOVERLAY", "Inserting new dictionary to db failed, cannot continue")
            return
        }

        val zipFile = ZipFile(fileUri.path)
        val entries = zipFile.entries()

        while (entries.hasMoreElements()) {
            val zipEntry = entries.nextElement()
            val bufferedReader = BufferedReader(InputStreamReader(zipFile.getInputStream(zipEntry)))

            try {
                val jsonArray = JSONArray(bufferedReader.readText())
                val termEntries = arrayOfNulls<DictionaryTermEntry>(jsonArray.length())
                for (i in 0 until jsonArray.length()) {
                    val innerArray = jsonArray.getJSONArray(i)
                    termEntries[i] = DictionaryTermEntry(
                            innerArray.optString(0),
                            innerArray.optString(1),
                            innerArray.optString(2),
                            innerArray.optString(3),
                            innerArray.optLong(4),
                            innerArray.getJSONArray(5),
                            innerArray.optLong(6),
                            innerArray.optString(7)
                    )
                }
                insertNewTerms(dictionaryID, termEntries)
            }
            catch (ex: JSONException) {
                Log.e("SUBSOVERLAY", "An error occurred while trying to read '${zipEntry.name}' into a json object.", ex)
            }
            catch (ex: Exception) {
                Log.e("SUBSOVERLAY", "An error occurred while trying to read '${zipEntry.name}'", ex)
            }
        }
    }
}