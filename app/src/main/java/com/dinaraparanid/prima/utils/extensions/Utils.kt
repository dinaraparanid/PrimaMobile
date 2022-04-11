package com.dinaraparanid.prima.utils.extensions

import android.text.Html

/** Gets title and subtitle for buttons and text views */

internal fun getTitleAndSubtitle(title: String, subtitle: String) = Html.fromHtml(
    "$title<br /><small>$subtitle</small>"
)