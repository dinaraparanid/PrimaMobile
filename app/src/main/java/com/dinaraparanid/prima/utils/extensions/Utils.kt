package com.dinaraparanid.prima.utils.extensions

import android.os.Build
import android.text.Html
import android.text.Spanned

/** Gets title and subtitle for buttons and text views */

fun getTitleAndSubtitle(title: String, subtitle: String): Spanned = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> Html.fromHtml(
        "$title<br /><small>$subtitle</small>",
        Html.FROM_HTML_MODE_LEGACY
    )

    else -> Html.fromHtml("$title<br /><small>$subtitle</small>")
}