package com.dinaraparanid.prima.viewmodels.mvvm

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.dialogs.ReleaseDialog
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.web.github.GitHubFetcher
import java.lang.ref.WeakReference

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.main_menu.about_app.AboutAppFragment]
 */

class AboutAppViewModel(private val activity: WeakReference<Activity>) : ViewModel() {
    /**
     * Sends intent to open
     * developer's profile on Github
     */

    @JvmName("sendGithubIntent")
    internal fun sendGithubIntent() = activity.unchecked.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://github.com/dinaraparanid")
        )
    )

    /**
     * Sends intent to open
     * developer's profile on Twitter
     */

    @JvmName("sendTwitterIntent")
    internal fun sendTwitterIntent() = activity.unchecked.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://twitter.com/paranid5")
        )
    )

    /**
     * Sends intent to open
     * developer's profile on Telegram
     */

    @JvmName("sendTelegramIntent")
    internal fun sendTelegramIntent() = activity.unchecked.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://t.me/Paranid5")
        )
    )

    /**
     * Sends intent to
     * developer's email
     */

    @JvmName("sendEmailIntent")
    internal fun sendEmailIntent() = activity.unchecked.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND)
                .setType("plain/text")
                .putExtra(Intent.EXTRA_EMAIL, arrayOf("arseny_magnitogorsk@live.ru")),
            activity.unchecked.resources.getString(R.string.send_email)
        )
    )

    @JvmName("showCurrentVersionInfo")
    internal fun showCurrentVersionInfo() = activity.unchecked.run {
        GitHubFetcher().fetchLatestRelease().observe(this as MainActivity) { release ->
            ReleaseDialog(release, this, ReleaseDialog.Target.CURRENT).show()
        }
    }
}