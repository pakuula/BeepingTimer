package me.pakuula.beeper

import me.pakuula.beeper.util.Work
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals

import org.junit.Test


class WorkTest {
    fun assertNext(work: Work) : Work {
        val nextWork = work.next()
        nextWork.checkInvariant()
        return nextWork
    }

    fun assertRepeatNext(work: Work, n: Int): Work {
        var nextWork = work
        repeat(n) {
            nextWork = assertNext(nextWork)
        }
        return nextWork
    }

    @Test
    fun `default values`() {
        val work = Work(
            maxRep = 3,
            maxSet = 3,
        )
        assertTrue(work.isPreparation)
        assertFalse(work.isWorking)
        assertFalse(work.isRest)
        assertFalse(work.isFinished)
        assertEquals(1, work.currentRep)
        assertEquals(1, work.currentSet)
    }

    @Test
    fun `first work`() {
        val work = Work(
            maxRep = 3,
            maxSet = 3,
        )

        val nextWork = assertNext(work)
        assertFalse(nextWork.isPreparation)
        assertTrue(nextWork.isWorking)
        assertFalse(nextWork.isRest)
        assertFalse(nextWork.isFinished)
        assertEquals(1, nextWork.currentRep)
        assertEquals(1, nextWork.currentSet)
    }

    @Test
    fun `second rep`() {
        val work = Work(
            maxRep = 3,
            maxSet = 3,
        )
        val nextWork = assertRepeatNext(work, 2)

        assertFalse(nextWork.isPreparation)
        assertTrue(nextWork.isWorking)
        assertFalse(nextWork.isRest)
        assertFalse(nextWork.isFinished)
        assertEquals(2, nextWork.currentRep)
        assertEquals(1, nextWork.currentSet)
    }

    @Test
    fun `work until last rep in the first set`() {
        val work = Work(
            maxRep = 3,
            maxSet = 3,
        )
        val nextWork = assertRepeatNext(work, 3)
        nextWork.checkInvariant()

        assertFalse(nextWork.isPreparation)
        assertTrue(nextWork.isWorking)
        assertFalse(nextWork.isRest)
        assertFalse(nextWork.isFinished)
        assertEquals(3, nextWork.currentRep)
        assertEquals(1, nextWork.currentSet)
    }

    @Test
    fun `work until the first rest`() {
        val work = Work(
            maxRep = 3,
            maxSet = 3,
        )
        val nextWork = assertRepeatNext(work, 4)

        assertTrue(nextWork.isRest)
        assertFalse(nextWork.isWorking)
        assertFalse(nextWork.isPreparation)
        assertFalse(nextWork.isFinished)
        assertEquals(1, nextWork.currentRep)
        assertEquals(2, nextWork.currentSet)
    }

    @Test
    fun `work after the first rest`() {
        var work = Work(
            maxRep = 3,
            maxSet = 3,
        )
        work = assertNext(work) // before 1st rep

        work = assertRepeatNext(work, 3) // 1st set, 3 reps
        work = assertNext(work) // after 1st rest
        assertFalse(work.isRest)
        assertTrue(work.isWorking)
        assertFalse(work.isPreparation)
        assertFalse(work.isFinished)
        assertEquals(1, work.currentRep)
        assertEquals(2, work.currentSet)
    }

    @Test
    fun `work until the second rest`() {
        var work = Work(
            maxRep = 3,
            maxSet = 3,
        )
        work = work.next() // 1st rep

        for (i in 1..2) {
            assertTrue(work.toString(), work.isWorking)
            assertEquals(1, work.currentRep)
            assertEquals(i, work.currentSet)

            work = work.next().next().next()

            assertTrue(work.isRest)
            assertFalse(work.isWorking)
            assertFalse(work.isFinished)
            assertEquals(1, work.currentRep)
            assertEquals(i+1, work.currentSet)

            work = assertNext(work) // go to the next rep
            assertTrue(work.isWorking)
        }
    }

    @Test
    fun `work until the last rep in the last set`() {
        var work = Work(
            maxRep = 3,
            maxSet = 3,
        )
        // skip preparation, 1st set, 3 reps, 1st rest, 2nd set, 3 reps, 2nd rest
        work = assertRepeatNext(work, 1 + 2 * 4)


        assertFalse(work.isPreparation)
        assertTrue(work.isWorking)
        assertFalse(work.isRest)
        assertFalse(work.isFinished)
        assertEquals(1, work.currentRep)
        assertEquals(3, work.currentSet)

        work = assertNext(work) // go to the next rep
        assertFalse(work.isPreparation)
        assertTrue(work.isWorking)
        assertFalse(work.isRest)
        assertFalse(work.isFinished)
        assertEquals(2, work.currentRep)
        assertEquals(3, work.currentSet)

        work = assertNext(work) // go to the next rep
        assertFalse(work.isPreparation)
        assertTrue(work.isWorking)
        assertFalse(work.isRest)
        assertFalse(work.isFinished)
        assertEquals(3, work.currentRep)
        assertEquals(3, work.currentSet)
        assertTrue(work.isLastRep())
        assertTrue(work.isVeryLastRep())
    }

    @Test
    fun `work until finished`() {
        var work = Work(
            maxRep = 3,
            maxSet = 3,
        )
        // skip preparation, 1st set, 3 reps, 1st rest, 2nd set, 3 reps, 2nd rest, 3rd set, 3 reps
        work = assertRepeatNext(work, 1 + 2 * 4 + 2)

        assertFalse(work.isPreparation)
        assertTrue(work.isWorking)
        assertFalse(work.isRest)
        assertFalse(work.isFinished)
        assertEquals(3, work.currentRep)
        assertEquals(3, work.currentSet)

        work = assertNext(work) // go to the next rep
        assertTrue(work.isFinished)
        assertFalse(work.isWorking)
        assertFalse(work.isRest)
        assertEquals(3, work.currentRep)
        assertEquals(3, work.currentSet)
    }

    @Test
    fun `all next`() {
        var work = Work(
            maxRep = 3,
            maxSet = 3,
        )
        val expected = listOf(
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 1, currentSet = 1),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 2, currentSet = 1),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 3, currentSet = 1),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = true, isFinished = false, currentRep = 1, currentSet = 2),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 1, currentSet = 2),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 2, currentSet = 2),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 3, currentSet = 2),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = true, isFinished = false, currentRep = 1, currentSet = 3),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 1, currentSet = 3),
            Work(maxRep = 3, maxSet = 3, isPreparation = false, isRest = false, isFinished = false, currentRep = 2, currentSet = 3),
            Work(maxRep = 3, maxSet = 3, isPreparation = false, isRest = false, isFinished = false, currentRep = 3, currentSet = 3),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = true, currentRep = 3, currentSet = 3),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = true, currentRep = 3, currentSet = 3),
        )
        val actual = mutableListOf<Work>()
        repeat(expected.size) {
            work = assertNext(work)
            actual.add(work.copy()) // copy to avoid reference issues
        }

        assertEquals(expected, actual)
    }

    @Test
    fun `all prev`() {
        var work = Work(
            maxRep = 3,
            maxSet = 3,
            currentRep = 3,
            currentSet = 3,
            isFinished = true,
            isRest = false,
            isPreparation = false,
        )
        val expected = listOf(
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 3, currentSet = 3),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 2, currentSet = 3),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 1, currentSet = 3),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = true, isFinished = false, currentRep = 1, currentSet = 3),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 3, currentSet = 2),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 2, currentSet = 2),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 1, currentSet = 2),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = true, isFinished = false, currentRep = 1, currentSet = 2),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 3, currentSet = 1),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 2, currentSet = 1),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 1, currentSet = 1),
            Work(maxRep = 3, maxSet = 3, isPreparation = false,  isRest = false, isFinished = false, currentRep = 1, currentSet = 1),
        )
        val actual = mutableListOf<Work>()
        repeat(expected.size) {
            work = work.prev()
            actual.add(work.copy()) // copy to avoid reference issues
        }
        for (i in expected.indices) {
            assertEquals("step ${i+1}: expected ${expected[i]}, got ${actual[i]}",
                expected[i], actual[i])
        }
    }
}
