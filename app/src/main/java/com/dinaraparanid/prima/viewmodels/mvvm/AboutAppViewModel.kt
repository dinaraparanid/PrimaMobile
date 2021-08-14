package com.dinaraparanid.prima.viewmodels.mvvm

import android.content.Intent
import android.net.Uri
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.AboutAppFragment]
 */

class AboutAppViewModel(private val application: MainApplication) : ViewModel() {
    /**
     * Sends intent to open
     * developer's profile on Github
     */

    @JvmName("sendGithubIntent")
    internal fun sendGithubIntent() = application.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://github.com/dinaraparanid")
        )
    )

    /**
     * Sends intent to open
     * developer's profile on VK
     */

    @JvmName("sendVKIntent")
    internal fun sendVKIntent() = application.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://vk.com/paranid5")
        )
    )

    /**
     * Sends intent to
     * developer's email
     */

    @JvmName("sendEmailIntent")
    internal fun sendEmailIntent() = application.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND)
                .setType("plain/text")
                .putExtra(Intent.EXTRA_EMAIL, arrayOf("arseny_magnitogorsk@live.ru")),
            application.resources.getString(R.string.send_email)
        )
    )
}