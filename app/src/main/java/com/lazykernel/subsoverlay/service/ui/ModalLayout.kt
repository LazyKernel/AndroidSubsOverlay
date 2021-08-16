package com.lazykernel.subsoverlay.service.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout

class ModalLayout(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {

    override fun removeDetachedView(child: View?, animate: Boolean) {
        super.removeDetachedView(child, false)
    }
}