package com.batb4016.jarpick.data

import com.batb4016.jarpick.domain.DecisionMode
import com.batb4016.jarpick.domain.JarLimits
import com.batb4016.jarpick.domain.PickEngine
import com.batb4016.jarpick.domain.PickRequest
import com.batb4016.jarpick.domain.PickableOption
import com.batb4016.jarpick.domain.RoundOptionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

class JarRepository(
    private val dao: JarPickDao,
    private val pickEngine: PickEngine = PickEngine(),
) {
    fun observeJars(): Flow<List<JarWithOptions>> = dao.observeJarsWithOptions()
    fun observeOptions(jarId: String): Flow<List<OptionEntity>> = dao.observeOptions(jarId)
    fun observeHistory(): Flow<List<PickHistoryEntity>> = dao.observeAllHistory()
    fun observeHistory(jarId: String): Flow<List<PickHistoryEntity>> = dao.observeHistory(jarId)

    suspend fun createJar(name: String, icon: String, theme: String, currentJarCount: Int, isPremium: Boolean): Result<String> {
        val limit = JarLimits.canCreateJar(currentJarCount, isPremium)
        if (!limit.allowed) return Result.failure(IllegalStateException(limit.reason))
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        dao.upsertJar(
            JarEntity(
                id = id,
                name = name.ifBlank { "New Jar" },
                icon = icon.ifBlank { "jar" },
                theme = theme.ifBlank { "Sunlit" },
                mode = DecisionMode.FAIR.name,
                createdAt = now,
                updatedAt = now,
                archivedAt = null,
                lastPickedOptionId = null,
                currentRoundId = null,
            ),
        )
        return Result.success(id)
    }

    suspend fun createStarterJars(currentJarCount: Int, isPremium: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val starterJars = listOf(
            "Dinner Jar" to listOf("Pizza", "Sushi", "Tacos", "Pasta", "Burgers", "Salad", "Thai", "Cook at home"),
            "Chores Jar" to listOf("Dishes", "Laundry", "Trash", "Clear desk", "Vacuum one room", "Wipe counter"),
            "Weekend Jar" to listOf("Walk outside", "Coffee shop", "Movie night", "Board game", "Try a new recipe", "Clean one area"),
            "Study Jar" to listOf("Review notes", "Practice questions", "Watch one lecture", "Summarize one topic", "Make flashcards"),
            "Game Night Jar" to listOf("Who goes first", "Pick teams", "Choose game", "Choose snack", "Choose playlist"),
        )
        val availableStarterJars = if (isPremium) {
            starterJars
        } else {
            starterJars.take((3 - currentJarCount).coerceAtLeast(0))
        }
        if (availableStarterJars.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Free JarPick supports up to 3 jars. Upgrade for unlimited starter jars."))
        }
        availableStarterJars.forEachIndexed { index, starter ->
            val jarId = createJar(starter.first, listOf("dinner", "task", "walk", "study", "game")[index], "Sunlit", 0, true).getOrThrow()
            starter.second.forEach { createOption(jarId, it, "", 1, 0, true) }
        }
        Result.success(Unit)
    }

    suspend fun createOption(jarId: String, text: String, notes: String?, weight: Int, currentOptionCount: Int, isPremium: Boolean): Result<String> {
        val limit = JarLimits.canCreateOption(currentOptionCount, isPremium)
        if (!limit.allowed) return Result.failure(IllegalStateException(limit.reason))
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        dao.upsertOption(
            OptionEntity(
                id = id,
                jarId = jarId,
                text = text.ifBlank { "Choice" },
                notes = notes?.ifBlank { null },
                weight = weight.coerceIn(1, 5),
                isActive = true,
                isTemporarilyHidden = false,
                hiddenUntil = null,
                createdAt = now,
                updatedAt = now,
                archivedAt = null,
                totalPickedCount = 0,
                lastPickedAt = null,
            ),
        )
        return Result.success(id)
    }

    suspend fun updateJarMode(jarId: String, mode: DecisionMode, isPremium: Boolean): Result<Unit> {
        if (!isPremium && mode != DecisionMode.FAIR) {
            return Result.failure(IllegalStateException("Upgrade to use ${mode.name.lowercase().replace('_', ' ')}."))
        }
        val jar = dao.getJar(jarId) ?: return Result.failure(IllegalArgumentException("Jar not found."))
        dao.updateJar(jar.copy(mode = mode.name, updatedAt = System.currentTimeMillis()))
        return Result.success(Unit)
    }

    suspend fun pick(jarId: String, isPremium: Boolean, freeHistoryLimit: Int = 20): PickHistoryEntity? = withContext(Dispatchers.IO) {
        val jar = dao.getJar(jarId) ?: return@withContext null
        val requestedMode = runCatching { DecisionMode.valueOf(jar.mode) }.getOrDefault(DecisionMode.FAIR)
        val mode = if (isPremium) requestedMode else DecisionMode.FAIR
        val options = dao.getOptions(jarId)
        val roundId = jar.currentRoundId ?: UUID.randomUUID().toString()
        val roundStates = dao.getRoundStates(jarId, roundId)
        val result = pickEngine.pick(
            PickRequest(
                mode = mode,
                options = options.map { it.toPickable() },
                lastPickedOptionId = jar.lastPickedOptionId,
                roundStates = roundStates.map { RoundOptionState(it.optionId, it.usedAt, it.eliminatedAt) },
            ),
        ) ?: return@withContext null
        val now = System.currentTimeMillis()
        val effectiveRoundId = result.roundId ?: roundId
        if (result.resetRound) dao.clearRoundState(jarId)
        val history = PickHistoryEntity(
            id = UUID.randomUUID().toString(),
            jarId = jar.id,
            optionId = result.option.id,
            optionTextSnapshot = result.option.text,
            jarNameSnapshot = jar.name,
            pickedAt = now,
            localDate = LocalDate.now().toString(),
            mode = mode.name,
            accepted = null,
        )
        dao.insertHistory(history)
        if (!isPremium) dao.trimHistory(freeHistoryLimit)
        dao.getOption(result.option.id)?.let { option ->
            dao.updateOption(option.copy(totalPickedCount = option.totalPickedCount + 1, lastPickedAt = now, updatedAt = now))
        }
        dao.updateJar(jar.copy(lastPickedOptionId = result.option.id, currentRoundId = effectiveRoundId, updatedAt = now))
        if (mode == DecisionMode.NO_REPEAT_UNTIL_EMPTY || mode == DecisionMode.ELIMINATION) {
            dao.upsertRoundState(
                RoundStateEntity(
                    id = "${effectiveRoundId}:${result.option.id}",
                    jarId = jarId,
                    optionId = result.option.id,
                    roundId = effectiveRoundId,
                    usedAt = if (mode == DecisionMode.NO_REPEAT_UNTIL_EMPTY) now else null,
                    eliminatedAt = if (mode == DecisionMode.ELIMINATION) now else null,
                ),
            )
        }
        history
    }

    suspend fun hideOptionForNow(optionId: String) {
        val option = dao.getOption(optionId) ?: return
        val fourHours = 4 * 60 * 60 * 1000L
        dao.updateOption(option.copy(isTemporarilyHidden = true, hiddenUntil = System.currentTimeMillis() + fourHours, updatedAt = System.currentTimeMillis()))
    }

    suspend fun archiveOption(optionId: String) {
        val option = dao.getOption(optionId) ?: return
        dao.updateOption(option.copy(archivedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun resetEliminations(jarId: String) = dao.clearRoundState(jarId)

    suspend fun deleteAllLocalData() {
        dao.deleteAllHistory()
        dao.deleteAllJars()
    }

    private fun OptionEntity.toPickable() = PickableOption(
        id = id,
        text = text,
        weight = weight,
        isActive = isActive,
        isTemporarilyHidden = isTemporarilyHidden,
        hiddenUntil = hiddenUntil,
        archivedAt = archivedAt,
    )
}
