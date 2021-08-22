package com.lazykernel.subsoverlay.service.dictionary

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DictionaryDBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "dictdb"
        // Build version handling if/when this is incremented
        const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase?) {

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (db == null) {
            Log.e("SUBSOVERLAY", "Creating dictionary db failed, db was null")
            return
        }

        // Create dictionaries table
        db.execSQL(
            "CREATE TABLE dict_dictionaries(" +
                "id INTEGER PRIMARY KEY," +
                "title TEXT NOT NULL," +
                "revision TEXT," +
                "version INTEGER," +
                // Will be boolean (0 or 1)
                "sequenced INTEGER" +
            ")"
        )

        // Create terms table (from term_bank_*.json) files
        // Foreign key relationship with dict.dictionaries
        db.execSQL(
            "CREATE TABLE dict_terms(" +
                "id INTEGER PRIMARY KEY," +
                "expression TEXT," +
                "reading TEXT," +
                "definition_tags TEXT," +
                "rules TEXT," +
                "score INTEGER," +
                // Glossary is an array, but sqlite doesn't support arrays
                // For this use case, maybe ok to parse array to string
                "glossary TEXT NOT NULL," +
                "sequence INTEGER," +
                "term_tags TEXT," +
                "dictionary INTEGER" +
                // ON DELETE CASCADE allows us to delete all terms in a dictionary with one
                // delete statement on parent table
                "FOREIGN KEY (dictionary) REFERENCES dict_dictionaries(id) ON DELETE CASCADE" +
            ")"
        )

        // Create indexes for expression and reading columns in dict.terms
        db.execSQL("CREATE INDEX dict_idx_term_expression ON dict_terms (expression)")
        db.execSQL("CREATE INDEX dict_idx_term_reading ON dict_terms (reading)")
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (db == null) {
            Log.e("SUBSOVERLAY", "Downgrading dictionary db failed, db was null")
            return
        }

        db.execSQL("DROP INDEX IF EXISTS dict_idx_term_reading")
        db.execSQL("DROP INDEX IF EXISTS dict_idx_term_expression")
        db.execSQL("DROP TABLE IF EXISTS dict_terms")
        db.execSQL("DROP TABLE IF EXISTS dict_dictionaries")

        super.onDowngrade(db, oldVersion, newVersion)
    }

}