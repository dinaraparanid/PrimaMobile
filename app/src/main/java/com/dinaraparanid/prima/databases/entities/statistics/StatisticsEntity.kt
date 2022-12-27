package com.dinaraparanid.prima.databases.entities.statistics

import com.dinaraparanid.prima.databases.entities.Entity

/**
 * Entity for statistics.
 * Represents the number
 * user listened it per time
 */

sealed interface StatisticsEntity : Entity {
    /** How many times it's listened totally */
    val count: Long

    /** How many times it's listened today */
    val countDaily: Long

    /** How many times it's listened this week */
    val countWeekly: Long

    /** How many times it's listened this month */
    val countMonthly: Long

    /** How many times it's listened this year */
    val countYearly: Long
}