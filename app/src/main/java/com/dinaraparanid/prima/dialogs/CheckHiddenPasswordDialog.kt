package com.dinaraparanid.prima.dialogs

import android.text.InputType
import android.widget.Toast
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.InputDialog

/**
 * Dialog that creates new password
 * when user opens hidden tracks for the first time
 */

internal class CheckHiddenPasswordDialog(passwordHash: Int, activity: MainActivity) : InputDialog(
    message = R.string.check_password,
    okAction = { password, dialog ->
        when (password.hashCode()) {
            passwordHash -> activity.showHiddenTrackListFragment()

            else -> {
                dialog.dismiss()

                Toast.makeText(
                    Params.getInstanceSynchronized().application.unchecked,
                    R.string.incorrect_password,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    },
    textType = InputType.TYPE_TEXT_VARIATION_PASSWORD
)