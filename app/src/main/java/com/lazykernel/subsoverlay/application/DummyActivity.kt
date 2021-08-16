package com.lazykernel.subsoverlay.application

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.fragment.app.FragmentActivity

class DummyActivity : Activity() {
    /*
    Accepts extras
    "action": (int) one of DummyActivity.Actions (use ordinal)
    "shouldClose": (bool) if true, closes activity after done (default true)
     */
    enum class Actions {
        ACTION_PICK_SUB_FILE
    }

    abstract class ResultListener {
        abstract fun onSuccess(data: Intent?)
        abstract fun onFailure(data: Intent?)
    }

    companion object {
        var mResultListener: ResultListener? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("SUBSDUMMYACTION", "bundle $intent")
        when (intent.getIntExtra("action", -1)) {
            Actions.ACTION_PICK_SUB_FILE.ordinal -> pickSubFile()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i("SUBSDUMMYACTION", "intent data $data")
        if (resultCode == RESULT_OK) {
            mResultListener?.onSuccess(data)
        }
        else if (resultCode == RESULT_CANCELED) {
            mResultListener?.onFailure(data)
        }
        mResultListener = null

        if (intent.getBooleanExtra("shouldClose", true)) {
            finish()
        }
    }

    private fun pickSubFile() {
        val fileOpenIntent = Intent()
        fileOpenIntent.apply {
            type = "*/*"
            action = Intent.ACTION_GET_CONTENT
            putExtras(intent)
        }
        startActivityForResult(Intent.createChooser(fileOpenIntent, "Select a sub file"), 1001)
    }
}