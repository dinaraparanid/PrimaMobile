package com.dinaraparanid.prima.fragments.main_menu

import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.fragments.main_menu.about_app.AboutAppFragment
import com.dinaraparanid.prima.fragments.guess_the_melody.GTMMainFragment
import com.dinaraparanid.prima.fragments.main_menu.settings.SettingsFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.ViewPagerFragment

/** [ViewPagerFragment] wit all main fragments */

class UltimateCollectionFragment : ViewPagerFragment() {
    override val isTabShown = false

    override val fragmentsTitles = intArrayOf(
        R.string.tracks,
        R.string.artists,
        R.string.mp3_converter,
        R.string.guess_the_melody,
        R.string.settings,
        R.string.about_app
    )

    private inline val tracksFragment
        get() = defaultInstance(
            resources.getString(R.string.tracks),
            DefaultTrackListFragment::class
        )

    private inline val artistsFragment
        get() = defaultInstance(
            resources.getString(R.string.artists),
            DefaultArtistListFragment::class
        )

    private inline val mp3ConverterFragment
        get() = defaultInstance(
            null,
            MP3ConverterFragment::class
        )

    private inline val gtmFragment
        get() = defaultInstance(
            null,
            GTMMainFragment::class
        )

    private inline val settingsFragment
        get() = defaultInstance(
            null,
            SettingsFragment::class
        )

    private inline val aboutAppFragment
        get() = defaultInstance(
            null,
            AboutAppFragment::class
        )

    override val fragmentsConstructors: Array<() -> Fragment> by lazy {
        arrayOf(
            ::tracksFragment,
            ::artistsFragment,
            ::mp3ConverterFragment,
            ::gtmFragment,
            ::settingsFragment,
            ::aboutAppFragment
        )
    }

    /** Deinitializes pager's adapter */
    override fun onDestroyView() {
        super.onDestroyView()
        binding?.pager?.adapter = null
    }
}