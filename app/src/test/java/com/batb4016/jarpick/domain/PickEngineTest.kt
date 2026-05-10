package com.batb4016.jarpick.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PickEngineTest {
    @Test fun fairPickReturnsNullWithNoActiveOptions() {
        val engine = PickEngine(Random(1))
        assertNull(engine.pick(PickRequest(DecisionMode.FAIR, emptyList())))
        assertNull(engine.pick(PickRequest(DecisionMode.FAIR, listOf(option("a", isActive = false)))))
    }

    @Test fun fairPickAvoidsImmediateRepeatWhenPossible() {
        val engine = PickEngine(Random(2))
        repeat(20) {
            val result = engine.pick(PickRequest(DecisionMode.FAIR, listOf(option("a"), option("b")), lastPickedOptionId = "a"))
            assertEquals("b", result?.option?.id)
        }
    }

    @Test fun weightedPickRespectsWeightsStatisticallyEnoughForUnitTesting() {
        val engine = PickEngine(Random(3))
        val counts = mutableMapOf("light" to 0, "heavy" to 0)
        repeat(1000) {
            val picked = engine.pick(PickRequest(DecisionMode.WEIGHTED, listOf(option("light", weight = 1), option("heavy", weight = 5))))!!
            counts[picked.option.id] = counts.getValue(picked.option.id) + 1
        }
        assertTrue("heavy option should be selected much more often", counts.getValue("heavy") > counts.getValue("light") * 3)
    }

    @Test fun noRepeatUsesAllOptionsBeforeReset() {
        val engine = PickEngine(Random(4)) { "new-round" }
        val options = listOf(option("a"), option("b"))
        val first = engine.pick(PickRequest(DecisionMode.NO_REPEAT_UNTIL_EMPTY, options, roundStates = emptyList()))!!
        val second = engine.pick(
            PickRequest(
                DecisionMode.NO_REPEAT_UNTIL_EMPTY,
                options,
                lastPickedOptionId = first.option.id,
                roundStates = listOf(RoundOptionState(first.option.id, usedAt = 1L)),
            ),
        )!!
        assertNotEquals(first.option.id, second.option.id)
        val reset = engine.pick(
            PickRequest(
                DecisionMode.NO_REPEAT_UNTIL_EMPTY,
                options,
                roundStates = listOf(RoundOptionState("a", usedAt = 1L), RoundOptionState("b", usedAt = 2L)),
            ),
        )!!
        assertTrue(reset.resetRound)
        assertEquals("new-round", reset.roundId)
    }

    @Test fun eliminationExcludesEliminatedOptions() {
        val engine = PickEngine(Random(5))
        repeat(20) {
            val result = engine.pick(
                PickRequest(
                    DecisionMode.ELIMINATION,
                    listOf(option("a"), option("b")),
                    roundStates = listOf(RoundOptionState("a", eliminatedAt = 1L)),
                ),
            )
            assertEquals("b", result?.option?.id)
        }
    }

    private fun option(
        id: String,
        weight: Int = 1,
        isActive: Boolean = true,
    ) = PickableOption(id = id, text = id, weight = weight, isActive = isActive)
}
