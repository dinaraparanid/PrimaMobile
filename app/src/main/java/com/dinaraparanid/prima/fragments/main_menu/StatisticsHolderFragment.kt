package com.dinaraparanid.prima.fragments.main_menu

import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.ViewPagerFragment

/**
 * [ViewPagerFragment] for all statistics fragments
 */

class StatisticsHolderFragment : ViewPagerFragment() {
    override val isTabShown = true
    override val fragmentsTitles = intArrayOf(
        R.string.all_time,
        R.string.year,
        R.string.month,
        R.string.week,
        R.string.day
    )

    override val fragments: Array<Fragment> by lazy {
        arrayOf(
            allTimeStatistics,
            yearStatistics,
            monthStatistics,
            weekStatistics,
            dayStatistics
        )
    }

    private val allTimeStatistics by lazy {
        StatisticsFragment.newInstance(
            mainLabelOldText,
            StatisticsFragment.Companion.Type.ALL
        )
    }

    private val yearStatistics by lazy {
        StatisticsFragment.newInstance(
            mainLabelOldText,
            StatisticsFragment.Companion.Type.YEARLY
        )
    }

    private val monthStatistics by lazy {
        StatisticsFragment.newInstance(
            mainLabelOldText,
            StatisticsFragment.Companion.Type.MONTHLY
        )
    }

    private val weekStatistics by lazy {
        StatisticsFragment.newInstance(
            mainLabelOldText,
            StatisticsFragment.Companion.Type.WEEKLY
        )
    }

    private val dayStatistics by lazy {
        StatisticsFragment.newInstance(
            mainLabelOldText,
            StatisticsFragment.Companion.Type.DAILY
        )
    }
}