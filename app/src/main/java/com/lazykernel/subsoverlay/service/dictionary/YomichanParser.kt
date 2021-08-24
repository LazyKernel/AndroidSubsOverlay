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
import java.util.zip.ZipInputStream

class YomichanParser(context: Context) : IDictParser(context) {

    override fun parseDictionaryToDB(fileUri: Uri) {
        if (!parseIndex(fileUri)) {
            // An error occurred and it has been logged
            return
        }

        if (mTitle == null) {
            Log.e("SUBSOVERLAY", "Dictionary had no title, cannot continue")
            return
        }

        if (mFormat != 3) {
            Log.e("SUBSOVERLAY", "Can only read v3 dictionaries at the moment")
            return
        }

        val dictionaryID = insertNewDictionary(mTitle!!, mRevision, mFormat, mSequenced)

        if (dictionaryID == null) {
            Log.e("SUBSOVERLAY", "Inserting new dictionary to db failed, cannot continue")
            return
        }

        val zipStream = ZipInputStream(context.contentResolver.openInputStream(fileUri))
        var entry = zipStream.nextEntry

        while (entry != null) {
            if (!entry.name.startsWith("term_bank_")) {
                zipStream.closeEntry()
                continue
            }

            try {
                val jsonArray = JSONArray(zipEntryToString(zipStream))
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
                Log.e("SUBSOVERLAY", "An error occurred while trying to read '${entry.name}' into a json object.", ex)
            }
            catch (ex: Exception) {
                Log.e("SUBSOVERLAY", "An error occurred while trying to read '${entry.name}'", ex)
            }
            finally {
                zipStream.closeEntry()
            }

            entry = zipStream.nextEntry
        }

        zipStream.close()
        Log.i("SUBSOVERLAY", "Done parsing dict")
    }
}