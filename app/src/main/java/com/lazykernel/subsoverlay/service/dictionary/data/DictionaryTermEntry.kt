package com.lazykernel.subsoverlay.service.dictionary.data

import org.json.JSONArray

data class DictionaryTermEntry(
        val expression: String?,
        val reading: String?,
        val definitionTags: String?,
        val rules: String?,
        val score: Long?,
        val glossary: JSONArray,
        val sequence: Long?,
        val termTags: String?,
        val entryID: Long? = null,
        val dictionary: String? = null
)
