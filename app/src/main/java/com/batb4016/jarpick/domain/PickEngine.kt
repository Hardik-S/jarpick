package com.batb4016.jarpick.domain

import kotlin.random.Random
import java.util.UUID

data class PickableOption(
    val id: String,
    val text: String,
    val weight: Int = 1,
    val isActive: Boolean = true,
    val isTemporarilyHidden: Boolean = false,
    val hiddenUntil: Long? = null,
    val archivedAt: Long? = null,
)

data class RoundOptionState(
    val optionId: String,
    val usedAt: Long? = null,
    val eliminatedAt: Long? = null,
)

data class PickRequest(
    val mode: DecisionMode,
    val options: List<PickableOption>,
    val lastPickedOptionId: String? = null,
    val roundStates: List<RoundOptionState> = emptyList(),
    val now: Long = System.currentTimeMillis(),
)

data class PickResult(
    val option: PickableOption,
    val roundId: String? = null,
    val resetRound: Boolean = false,
)

class PickEngine(
    private val random: Random = Random.Default,
    private val roundIdFactory: () -> String = { UUID.randomUUID().toString() },
) {
    fun pick(request: PickRequest): PickResult? {
        val active = request.options.filter { it.isAvailableAt(request.now) }
        if (active.isEmpty()) return null

        return when (request.mode) {
            DecisionMode.FAIR -> pickFair(active, request.lastPickedOptionId)
            DecisionMode.WEIGHTED -> pickWeighted(active)
            DecisionMode.NO_REPEAT_UNTIL_EMPTY -> pickNoRepeatUntilEmpty(active, request)
            DecisionMode.ELIMINATION -> pickElimination(active, request)
        }
    }

    private fun pickFair(options: List<PickableOption>, lastPickedOptionId: String?): PickResult {
        val candidates = avoidImmediateRepeat(options, lastPickedOptionId)
        return PickResult(option = candidates.random(random))
    }

    private fun pickWeighted(options: List<PickableOption>): PickResult {
        val weighted = options.flatMap { option ->
            List(option.weight.coerceIn(1, 5)) { option }
        }
        return PickResult(option = weighted.random(random))
    }

    private fun pickNoRepeatUntilEmpty(options: List<PickableOption>, request: PickRequest): PickResult {
        val usedIds = request.roundStates.filter { it.usedAt != null }.map { it.optionId }.toSet()
        val unused = options.filterNot { it.id in usedIds }
        val reset = unused.isEmpty()
        val candidates = if (reset) options else unused
        val picked = avoidImmediateRepeat(candidates, request.lastPickedOptionId).random(random)
        return PickResult(option = picked, roundId = if (reset) roundIdFactory() else null, resetRound = reset)
    }

    private fun pickElimination(options: List<PickableOption>, request: PickRequest): PickResult? {
        val eliminatedIds = request.roundStates.filter { it.eliminatedAt != null }.map { it.optionId }.toSet()
        val candidates = options.filterNot { it.id in eliminatedIds }
        if (candidates.isEmpty()) return null
        return PickResult(option = avoidImmediateRepeat(candidates, request.lastPickedOptionId).random(random))
    }

    private fun avoidImmediateRepeat(options: List<PickableOption>, lastPickedOptionId: String?): List<PickableOption> {
        if (options.size <= 1 || lastPickedOptionId == null) return options
        return options.filterNot { it.id == lastPickedOptionId }.ifEmpty { options }
    }

    private fun PickableOption.isAvailableAt(now: Long): Boolean {
        if (!isActive || archivedAt != null) return false
        if (!isTemporarilyHidden) return true
        return hiddenUntil != null && hiddenUntil <= now
    }
}
