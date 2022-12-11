package com.dinaraparanid.prima.dialogs

import android.content.Context
import androidx.annotation.StringRes
import com.dinaraparanid.prima.R
import com.kaopiz.kprogresshud.KProgressHUD

/**
 * Creates dialog that shows that
 * user needs to wait until some event is finished
 *
 * @param context [Context] in which dialog will be shown
 * @param isCancelable is user allowed to cancel dialog by himself
 * @param message message to show (default is `please, wait`)
 * @return dialog itself
 */

fun createAndShowAwaitDialog(
    context: Context,
    isCancelable: Boolean,
    @StringRes message: Int = R.string.please_wait
) = KProgressHUD.create(context)
    .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
    .setLabel(context.resources.getString(message))
    .setCancellable(isCancelable)
    .setAnimationSpeed(2)
    .setDimAmount(0.5F)
    .show()