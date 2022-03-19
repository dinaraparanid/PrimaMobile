package com.dinaraparanid.prima.fragments.main_menu.statistics

import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.ViewPagerFragment

/** [ViewPagerFragment] for all statistics fragments */

class StatisticsHolderFragment : ViewPagerFragment() {
    override val isTabShown = true

    override val fragmentsTitles = intArrayOf(
        R.string.all_time,
        R.string.year,
        R.string.month,
        R.string.week,
        R.string.day
    )

    override val fragmentsConstructors: Array<() -> Fragment> by lazy {
        arrayOf(
            ::allTimeStatistics,
            ::yearStatistics,
            ::monthStatistics,
            ::weekStatistics,
            ::dayStatistics
        )
    }

    private inline val allTimeStatistics
        get() = StatisticsFragment.newInstance(StatisticsFragment.Companion.StatisticsType.ALL)

    private val yearStatistics
        get() = StatisticsFragment.newInstance(StatisticsFragment.Companion.StatisticsType.YEARLY)

    private val monthStatistics
        get() = StatisticsFragment.newInstance(StatisticsFragment.Companion.StatisticsType.MONTHLY)

    private inline val weekStatistics
        get() = StatisticsFragment.newInstance(StatisticsFragment.Companion.StatisticsType.WEEKLY)

    private inline val dayStatistics
        get() = StatisticsFragment.newInstance(StatisticsFragment.Companion.StatisticsType.DAILY)
}