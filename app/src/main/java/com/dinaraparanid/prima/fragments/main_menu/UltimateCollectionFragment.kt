package com.dinaraparanid.prima.fragments.main_menu

import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.fragments.about_app.AboutAppFragment
import com.dinaraparanid.prima.fragments.guess_the_melody.GTMMainFragment
import com.dinaraparanid.prima.fragments.settings.SettingsFragment
import com.dinaraparanid.prima.utils.polymorphism.ViewPagerFragment

/**
 * [ViewPagerFragment] wit all main fragments
 */

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

    private val tracksFragment by lazy {
        defaultInstance(
            mainLabelOldText,
            resources.getString(R.string.tracks),
            DefaultTrackListFragment::class
        )
    }

    private val artistsFragment by lazy {
        defaultInstance(
            mainLabelOldText,
            resources.getString(R.string.artists),
            DefaultArtistListFragment::class
        )
    }

    private val mp3ConverterFragment by lazy {
        defaultInstance(
            mainLabelOldText,
            null,
            MP3ConverterFragment::class
        )
    }

    private val gtmFragment by lazy {
        defaultInstance(
            mainLabelOldText,
            null,
            GTMMainFragment::class
        )
    }

    private val settingsFragment by lazy {
        defaultInstance(
            mainLabelOldText,
            null,
            SettingsFragment::class
        )
    }

    private val aboutAppFragment by lazy {
        defaultInstance(
            mainLabelOldText,
            null,
            AboutAppFragment::class
        )
    }

    override val fragments: Array<Fragment> by lazy {
        arrayOf(
            tracksFragment,
            artistsFragment,
            mp3ConverterFragment,
            gtmFragment,
            settingsFragment,
            aboutAppFragment
        )
    }
}