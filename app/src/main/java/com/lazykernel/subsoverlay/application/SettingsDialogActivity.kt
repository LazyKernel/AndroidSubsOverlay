package com.lazykernel.subsoverlay.application

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import com.lazykernel.subsoverlay.R

class SettingsDialogActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = layoutInflater.inflate(R.layout.settings_modal, null)
        layout.apply {
            setBackgroundColor(Color.WHITE)
            background = AppCompatResources.getDrawable(applicationContext, R.drawable.rounded_corners)
            clipToOutline = true
        }
        setContentView(layout)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            Log.i("SUBSOVERLAY", "Loaded $data")
        }
        else if (resultCode == RESULT_CANCELED) {
            Log.i("SUBSOVERLAY", "Sub file selecting cancelled")
        }
    }
}

//class SettingsDialogActivity : DialogFragment() {
//
//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        return activity?.let {
//            val builder = AlertDialog.Builder(it)
//            val inflater = requireActivity().layoutInflater
//            val layout = inflater.inflate(R.layout.settings_modal, null)
//            layout.apply {
//                setBackgroundColor(Color.WHITE)
//                background = AppCompatResources.getDrawable(applicationContext, R.drawable.rounded_corners)
//                clipToOutline = true
//            }
//
//            builder.apply {
//                setView(layout)
//                setNegativeButton(
//                    R.string.label_close,
//                    DialogInterface.OnClickListener { dialog, _ ->
//                        dialog.cancel()
//                    })
//            }
//
//            builder.create()
//        } ?: throw IllegalStateException("Activity cannot be null, alert the developer")
//    }
//}