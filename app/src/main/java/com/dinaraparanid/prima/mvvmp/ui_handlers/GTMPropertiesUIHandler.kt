package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.Context
import android.content.DialogInterface
import android.widget.Toast
import androidx.annotation.StringRes
import com.dinaraparanid.prima.mvvmp.view.dialogs.GTMSetPropertiesDialog

/** Ancestor [UIHandler] for GTMPropertiesUIHandlers */

interface GTMPropertiesUIHandler : UIHandler {
    fun dismissAndShowError(context: Context, @StringRes message: Int, dialog: DialogInterface) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        dialog.dismiss()
    }

    fun <D : GTMSetPropertiesDialog<*, *, *, *>> D.onOkPressed(dialog: DialogInterface)
}