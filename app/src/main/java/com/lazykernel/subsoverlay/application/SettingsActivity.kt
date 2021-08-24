package com.lazykernel.subsoverlay.application

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.lazykernel.subsoverlay.R
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.lazykernel.subsoverlay.service.dictionary.DictionaryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    val DICT_FILE_SELECT_CODE = 1002
    lateinit var mDictionaryManager: DictionaryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDictionaryManager = DictionaryManager(this)

        setContentView(R.layout.activity)
        val button = findViewById<Button>(R.id.go_to_accessibility_button)
        button.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val startButton = findViewById<Button>(R.id.start_service_button)
        startButton.setOnClickListener {
            if (!preferences.getBoolean("accessibilityServiceRunning", false)) {
                preferences.edit().putBoolean("accessibilityServiceRunning", true).apply()
            }
        }
        val stopButton = findViewById<Button>(R.id.stop_service_button)
        stopButton.setOnClickListener {
            if (preferences.getBoolean("accessibilityServiceRunning", false)) {
                preferences.edit().putBoolean("accessibilityServiceRunning", false).apply()
            }
        }

        val addDictButton = findViewById<Button>(R.id.dictionary_add)
        addDictButton.setOnClickListener {
            val fileOpenIntent = Intent()
            fileOpenIntent.apply {
                type = "application/zip"
                action = Intent.ACTION_GET_CONTENT
            }
            startActivityForResult(Intent.createChooser(fileOpenIntent, "Select a dictionary file"), DICT_FILE_SELECT_CODE)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            buildListView()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i("SUBSSETTINGS", "intent data $data")
        if (requestCode == DICT_FILE_SELECT_CODE && resultCode == RESULT_OK && data?.data != null) {
            mDictionaryManager.addNewDictionary(data.data!!)
        }
    }

    fun buildListView() {
        val listView = findViewById<ListView>(R.id.dictionaries_list_view)
        val dictionaries = mDictionaryManager.getDictionaries()
        val dictionaryTitles = dictionaries.map { e -> "${e.id}: ${e.title}" }
        val dictAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, dictionaryTitles)
        listView.adapter = dictAdapter
        listView.setOnItemClickListener { adapterView, view, i, l ->
            val dictionary = dictionaries[i]
            val builder = AlertDialog.Builder(this)
            builder.apply {
                setTitle(R.string.are_you_sure)
                setMessage(String.format(resources.getString(R.string.delete_dict_confirmation), dictionary.title))
                setPositiveButton(android.R.string.ok) { _, _ ->
                    mDictionaryManager.deleteDictionary(dictionary.id)
                }
                setNegativeButton(android.R.string.cancel, null)
            }
            builder.show()
        }
    }
}