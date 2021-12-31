package com.dinaraparanid.prima.utils.polymorphism

import com.dinaraparanid.prima.utils.Statistics
import com.dinaraparanid.prima.utils.StorageUtil

/** Updates statistics in Shared Preferences */

internal interface StatisticsUpdatable : AsyncContext {
    /** How statistics should be updated */
    val updateStyle: Statistics.() -> Statistics

    suspend fun updateStatisticsAsync() {
        runOnIOThread {
            StorageUtil.runSynchronized {
                storeStatistics(
                    loadStatistics()
                        ?.let(updateStyle)
                        ?: updateStyle(Statistics.empty)
                )

                storeStatisticsDaily(
                    loadStatisticsDaily()
                        ?.let(updateStyle)
                        ?: updateStyle(Statistics.empty)
                )

                storeStatisticsWeekly(
                    loadStatisticsWeekly()
                        ?.let(updateStyle)
                        ?: updateStyle(Statistics.empty)
                )

                storeStatisticsMonthly(
                    loadStatisticsMonthly()
                        ?.let(updateStyle)
                        ?: updateStyle(Statistics.empty)
                )

                storeStatisticsYearly(
                    loadStatisticsYearly()
                        ?.let(updateStyle)
                        ?: updateStyle(Statistics.empty)
                )
            }
        }
    }
}