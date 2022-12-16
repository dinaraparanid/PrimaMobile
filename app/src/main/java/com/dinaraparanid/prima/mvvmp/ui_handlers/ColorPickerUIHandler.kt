package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import com.dinaraparanid.prima.mvvmp.view.dialogs.ColorPickerDialog

/** [UIHandler] for [ColorPickerDialog] */

class ColorPickerUIHandler : UIHandler {
    fun closeDialog(dialog: DialogInterface) = dialog.dismiss()

    fun confirmColorSetup(
        dialog: DialogInterface,
        observer: ColorPickerDialog.ColorPickerObserver,
        color: Int
    ) {
        observer.onColorPicked(color)
        dialog.dismiss()
    }
}