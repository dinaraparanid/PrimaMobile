package com.dinaraparanid.prima.utils.dialogs

import android.text.InputType
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.polymorphism.InputDialog

/**
 * Dialog that creates new password
 * when user opens hidden tracks for the first time
 */

internal class CreateHiddenPasswordDialog : InputDialog(
    message = R.string.new_password,
    okAction = { password, _ ->
        StorageUtil.getInstanceSynchronized().storeHiddenPassword(password.hashCode())
        // TODO: Hidden Tracks Fragment
    },
    textType = InputType.TYPE_TEXT_VARIATION_PASSWORD
)