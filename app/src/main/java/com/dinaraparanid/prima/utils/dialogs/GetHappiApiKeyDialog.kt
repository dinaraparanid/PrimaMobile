package com.dinaraparanid.prima.utils.dialogs

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.InputDialog

/**
 * Dialog to get user's api key
 */

internal class GetHappiApiKeyDialog(callSelectLyricsFragment: (String) -> Unit) : InputDialog(
    R.string.api_key,
    callSelectLyricsFragment,
    null
)