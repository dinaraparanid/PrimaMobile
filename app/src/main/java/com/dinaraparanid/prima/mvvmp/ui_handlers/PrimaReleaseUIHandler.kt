package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.Context
import android.content.Intent
import android.net.Uri

/** [UIHandler] for NewReleaseDialog */

class PrimaReleaseUIHandler : UIHandler {
    fun sendToDownload(context: Context) = context.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://github.com/dinaraparanid/PrimaMobile/releases")
        )
    )
}