package org.isoron.uhabits.core.models

import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry.Companion.NO
import org.isoron.uhabits.core.models.Entry.Companion.SKIP
import org.isoron.uhabits.core.models.Entry.Companion.UNKNOWN
import org.isoron.uhabits.core.models.Entry.Companion.YES_AUTO
import org.isoron.uhabits.core.models.Entry.Companion.YES_MANUAL
import org.isoron.uhabits.core.models.sqlite.SQLiteHabitList
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BadHabitTest : BaseUnitTest() {
    private lateinit var today: LocalDate
    private lateinit var habit: Habit

    @BeforeTest
    override fun setUp() {
        super.setUp()
        today = getToday()
        habit = fixtures.createEmptyHabit()
        habit.isBadHabit = true
        habit.badHabitSince = today.minus(9)
        habit.recompute()
    }

    @Test
    fun testEveryDaySinceStartDefaultsToSuccess() {
        for (i in 0..9) {
            val entry = habit.computedEntries.get(today.minus(i))
            assertEquals(YES_MANUAL, entry.value)
            assertTrue(entry.completed)
        }
        assertTrue(habit.isCompletedToday())
    }

    @Test
    fun testDaysBeforeStartAndInTheFutureStayUnknown() {
        assertEquals(UNKNOWN, habit.computedEntries.get(today.minus(10)).value)
        assertEquals(UNKNOWN, habit.computedEntries.get(today.plus(1)).value)
        assertFalse(habit.computedEntries.get(today.plus(1)).completed)
    }

    @Test
    fun testOnlyTodayIsFilledWhenThereIsNoStartDate() {
        habit.badHabitSince = null
        habit.recompute()
        val known = habit.computedEntries.getKnown()
        assertEquals(1, known.size)
        assertEquals(today, known[0].date)
        assertEquals(YES_MANUAL, known[0].value)
    }

    @Test
    fun testStartDateMovesBackToOldestEntry() {
        habit.originalEntries.add(Entry(today.minus(20), NO))
        habit.recompute()
        assertEquals(NO, habit.computedEntries.get(today.minus(20)).value)
        assertEquals(YES_MANUAL, habit.computedEntries.get(today.minus(19)).value)
        assertEquals(UNKNOWN, habit.computedEntries.get(today.minus(21)).value)
    }

    @Test
    fun testExplicitFailureIsPreservedAndBreaksStreak() {
        assertEquals(10, habit.streaks.getBest(1)[0].length)

        habit.originalEntries.add(Entry(today.minus(4), NO))
        habit.recompute()

        val failure = habit.computedEntries.get(today.minus(4))
        assertEquals(NO, failure.value)
        assertFalse(failure.completed)

        val best = habit.streaks.getBest(5)
        assertEquals(2, best.size)
        assertEquals(listOf(4, 5), best.map { it.length }.sorted())
    }

    @Test
    fun testClearingFailureRestoresSuccess() {
        habit.originalEntries.add(Entry(today.minus(4), NO))
        habit.recompute()
        habit.originalEntries.add(Entry(today.minus(4), UNKNOWN))
        habit.recompute()

        assertEquals(YES_MANUAL, habit.computedEntries.get(today.minus(4)).value)
        assertEquals(10, habit.streaks.getBest(1)[0].length)
    }

    @Test
    fun testScoreGrowsWhileAvoidingAndDropsOnFailure() {
        val before = habit.scores[today].value
        assertTrue(before > 0.0)
        assertTrue(habit.scores[today].value > habit.scores[today.minus(5)].value)

        habit.originalEntries.add(Entry(today, NO))
        habit.recompute()
        assertTrue(habit.scores[today].value < before)
    }

    @Test
    fun testNotesAndSkipAreKept() {
        habit.originalEntries.add(Entry(today.minus(2), UNKNOWN, "Resisted"))
        habit.originalEntries.add(Entry(today.minus(3), SKIP))
        habit.recompute()

        val noted = habit.computedEntries.get(today.minus(2))
        assertEquals(YES_MANUAL, noted.value)
        assertEquals("Resisted", noted.notes)
        assertEquals(SKIP, habit.computedEntries.get(today.minus(3)).value)
    }

    @Test
    fun testConfiguredFrequencyIsIgnored() {
        habit.frequency = Frequency(1, 7)
        habit.recompute()
        habit.originalEntries.add(Entry(today, NO))
        habit.recompute()
        assertEquals(NO, habit.computedEntries.get(today).value)
        assertFalse(habit.isCompletedToday())
    }

    @Test
    fun testRegularHabitsAreUnaffected() {
        val regular = fixtures.createEmptyHabit()
        regular.badHabitSince = today.minus(9)
        regular.recompute()
        assertTrue(regular.computedEntries.getKnown().isEmpty())
        assertEquals(UNKNOWN, regular.computedEntries.get(today).value)
    }

    @Test
    fun testBadHabitToggleAlternatesBetweenFailureAndDefault() {
        for (value in listOf(YES_MANUAL, YES_AUTO, SKIP, UNKNOWN)) {
            assertEquals(NO, Entry.nextToggleValue(value, true, true, isBadHabit = true))
        }
        assertEquals(UNKNOWN, Entry.nextToggleValue(NO, true, true, isBadHabit = true))
    }

    @Test
    fun testRegularToggleIsUnchanged() {
        assertEquals(YES_MANUAL, Entry.nextToggleValue(YES_AUTO, true, true))
        assertEquals(SKIP, Entry.nextToggleValue(YES_MANUAL, true, true))
        assertEquals(NO, Entry.nextToggleValue(YES_MANUAL, false, true))
        assertEquals(UNKNOWN, Entry.nextToggleValue(NO, false, true))
        assertEquals(YES_MANUAL, Entry.nextToggleValue(NO, false, false))
    }

    @Test
    fun testCopyFromAndEquality() {
        val other = fixtures.createEmptyHabit()
        other.copyFrom(habit)
        assertTrue(other.isBadHabit)
        assertEquals(habit.badHabitSince, other.badHabitSince)
        other.id = habit.id
        assertEquals(habit, other)
        other.isBadHabit = false
        assertFalse(habit == other)
    }

    @Test
    fun testPersistenceMappingRoundTrip() {
        val data = SQLiteHabitList.copyFrom(habit)
        assertEquals(1, data.isBadHabit)
        assertEquals(habit.badHabitSince!!.unixTime, data.badHabitSince)

        val loaded = modelFactory.buildHabit()
        SQLiteHabitList.copyTo(data, loaded)
        assertTrue(loaded.isBadHabit)
        assertEquals(habit.badHabitSince, loaded.badHabitSince)
    }
}
