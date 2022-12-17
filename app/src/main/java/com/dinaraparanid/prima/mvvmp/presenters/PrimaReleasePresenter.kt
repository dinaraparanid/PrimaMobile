package com.dinaraparanid.prima.mvvmp.presenters

import com.dinaraparanid.prima.mvvmp.view.dialogs.PrimaReleaseDialogFragment
import com.dinaraparanid.prima.utils.web.github.ReleaseInfo
import org.koin.core.component.inject
import org.koin.core.qualifier.named

/** [BasePresenter] for NewReleaseDialogFragment */

class PrimaReleasePresenter(
    private val releaseInfo: ReleaseInfo,
    private val target: PrimaReleaseDialogFragment.Target,
) : BasePresenter() {
    companion object {
        const val NEW_VERSION_STR_RES = "new_version"
        const val VERSION_STR_RES = "version"
    }

    private val newVersionStrRes by inject<String>(named(NEW_VERSION_STR_RES))
    private val versionStrRes by inject<String>(named(VERSION_STR_RES))

    val version
        @JvmName("getVersion")
        get() = when (target) {
            PrimaReleaseDialogFragment.Target.NEW -> "${newVersionStrRes}: ${releaseInfo.name}!"
            PrimaReleaseDialogFragment.Target.CURRENT -> versionStrRes
        }

    val body
        @JvmName("getBody")
        get() = releaseInfo.body.replace("[*]".toRegex(), "")
}