package com.example.data

import java.util.Calendar

object BillingCycleUtils {

    /**
     * Calculates the start (inclusive) and end (inclusive) epoch millis of the current billing cycle
     * based on a reset day of the month (e.g., 27).
     */
    fun getCycleRange(referenceMillis: Long, resetDay: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceMillis
        }

        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val startCal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            val maxDayInMonth = getActualMaximum(Calendar.DAY_OF_MONTH)
            val actualResetDay = resetDay.coerceAtMost(maxDayInMonth)
            set(Calendar.DAY_OF_MONTH, actualResetDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (currentDay < resetDay) {
            // Started in previous month
            startCal.add(Calendar.MONTH, -1)
            val maxDayInPrevMonth = startCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val actualPrevResetDay = resetDay.coerceAtMost(maxDayInPrevMonth)
            startCal.set(Calendar.DAY_OF_MONTH, actualPrevResetDay)
        }

        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1) // 1 ms before next cycle start
        }

        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    /**
     * Returns a human readable label for the cycle range (e.g. "27 Lug - 26 Aug 2026").
     */
    fun getCycleLabel(startMillis: Long, endMillis: Long): String {
        val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }

        val months = arrayOf("Gen", "Feb", "Mar", "Apr", "Mag", "Giu", "Lug", "Ago", "Set", "Ott", "Nov", "Dic")
        val startDay = startCal.get(Calendar.DAY_OF_MONTH)
        val startMonth = months[startCal.get(Calendar.MONTH)]
        val endDay = endCal.get(Calendar.DAY_OF_MONTH)
        val endMonth = months[endCal.get(Calendar.MONTH)]
        val endYear = endCal.get(Calendar.YEAR)

        return "$startDay $startMonth - $endDay $endMonth $endYear"
    }
}
