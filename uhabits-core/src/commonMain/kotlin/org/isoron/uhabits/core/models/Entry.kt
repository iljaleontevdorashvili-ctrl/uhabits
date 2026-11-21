/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.core.models

import org.isoron.platform.time.LocalDate

data class Entry(
    val date: LocalDate,
    val value: Int,
    val notes: String = "",
    val completed: Boolean = false
) {
    val formattedValue: String
        get() = when (value) {
            YES_MANUAL -> "YES_MANUAL"
            YES_AUTO -> "YES_AUTO"
            NO -> "NO"
            SKIP -> "SKIP"
            UNKNOWN -> "UNKNOWN"
            else -> value.toString()
        }

    companion object {
        /**
         * Value indicating that the habit is not applicable for this timestamp.
         */
        const val SKIP = 3

        /**
         * Value indicating that the user has performed the habit at this timestamp.
         */
        const val YES_MANUAL = 2

        /**
         * Value indicating that the user did not perform the habit, but they were not
         * expected to, because of the frequency of the habit.
         */
        const val YES_AUTO = 1

        /**
         * Value indicating that the user did not perform the habit, even though they were
         * expected to perform it.
         */
        const val NO = 0

        /**
         * Value indicating that no data is available for the given timestamp.
         */
        const val UNKNOWN = -1

        /**
         * Returns the value that a tap on a checkmark should produce.
         *
         * For regular habits, the value cycles through YES_MANUAL, SKIP (if enabled), NO, and
         * UNKNOWN (if question marks are enabled). For bad habits, which are successful by
         * default, a tap alternates between NO (a recorded failure) and UNKNOWN (no data, which
         * is computed as a success).
         */
        fun nextToggleValue(
            value: Int,
            isSkipEnabled: Boolean,
            areQuestionMarksEnabled: Boolean,
            isBadHabit: Boolean = false
        ): Int {
            if (isBadHabit) return if (value == NO) UNKNOWN else NO
            return when (value) {
                YES_AUTO -> YES_MANUAL
                YES_MANUAL -> if (isSkipEnabled) SKIP else NO
                SKIP -> NO
                NO -> if (areQuestionMarksEnabled) UNKNOWN else YES_MANUAL
                UNKNOWN -> YES_MANUAL
                else -> YES_MANUAL
            }
        }
    }
}
