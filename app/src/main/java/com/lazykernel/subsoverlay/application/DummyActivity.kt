package com.lazykernel.subsoverlay.application

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log

class DummyActivity : Activity() {
    /*
    Accepts extras
    "action": (int) one of DummyActivity.Actions (use ordinal)
    "shouldClose": (bool) if true, closes activity after done (default true)
     */
    enum class Actions {
        ACTION_PICK_SUB_FILE,
        ACTION_MEDIA_PROJECTION
    }

    val PICK_SUB_FILE_CODE = 1001
    val MEDIA_PROJECTION_CODE = 1002

    abstract class ResultListener {
        abstract fun onSuccess(type: Actions, data: Intent?)
        abstract fun onFailure(type: Actions, data: Intent?)
    }

    companion object {
        var mResultListener: ResultListener? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("SUBSDUMMYACTION", "bundle $intent")
        when (intent.getIntExtra("action", -1)) {
            Actions.ACTION_PICK_SUB_FILE.ordinal -> pickSubFile()
            Actions.ACTION_MEDIA_PROJECTION.ordinal -> mediaProjection()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i("SUBSDUMMYACTION", "intent data $data")
        if (requestCode == PICK_SUB_FILE_CODE) {
            if (resultCode == RESULT_OK) {
                mResultListener?.onSuccess(Actions.ACTION_PICK_SUB_FILE, data)
            }
            else if (resultCode == RESULT_CANCELED) {
                mResultListener?.onFailure(Actions.ACTION_PICK_SUB_FILE, data)
            }
            mResultListener = null

            if (intent.getBooleanExtra("shouldClose", true)) {
                finish()
            }
        }
        else if (requestCode == MEDIA_PROJECTION_CODE) {
            if (resultCode == RESULT_OK) {
                mResultListener?.onSuccess(Actions.ACTION_MEDIA_PROJECTION, data)
            }
            else if (resultCode == RESULT_CANCELED) {
                mResultListener?.onFailure(Actions.ACTION_MEDIA_PROJECTION, data)
            }
            mResultListener = null

            if (intent.getBooleanExtra("shouldClose", true)) {
                finish()
            }
        }
    }

    private fun pickSubFile() {
        val fileOpenIntent = Intent()
        fileOpenIntent.apply {
            type = "*/*"
            action = Intent.ACTION_GET_CONTENT
            putExtras(intent)
        }
        startActivityForResult(Intent.createChooser(fileOpenIntent, "Select a sub file"), PICK_SUB_FILE_CODE)
    }

    private fun mediaProjection() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), MEDIA_PROJECTION_CODE)
    }
}