package com.batb4016.jarpick.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JarPickDao {
    @Transaction
    @Query("SELECT * FROM jars WHERE archivedAt IS NULL ORDER BY updatedAt DESC")
    fun observeJarsWithOptions(): Flow<List<JarWithOptions>>

    @Query("SELECT * FROM jars WHERE id = :jarId LIMIT 1")
    suspend fun getJar(jarId: String): JarEntity?

    @Query("SELECT * FROM options WHERE jarId = :jarId AND archivedAt IS NULL ORDER BY createdAt ASC")
    fun observeOptions(jarId: String): Flow<List<OptionEntity>>

    @Query("SELECT * FROM options WHERE jarId = :jarId AND archivedAt IS NULL ORDER BY createdAt ASC")
    suspend fun getOptions(jarId: String): List<OptionEntity>

    @Query("SELECT * FROM options WHERE id = :optionId LIMIT 1")
    suspend fun getOption(optionId: String): OptionEntity?

    @Query("SELECT * FROM round_state WHERE jarId = :jarId AND roundId = :roundId")
    suspend fun getRoundStates(jarId: String, roundId: String): List<RoundStateEntity>

    @Query("SELECT * FROM pick_history ORDER BY pickedAt DESC")
    fun observeAllHistory(): Flow<List<PickHistoryEntity>>

    @Query("SELECT * FROM pick_history WHERE jarId = :jarId ORDER BY pickedAt DESC")
    fun observeHistory(jarId: String): Flow<List<PickHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJar(jar: JarEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOption(option: OptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PickHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoundState(roundState: RoundStateEntity)

    @Update
    suspend fun updateJar(jar: JarEntity)

    @Update
    suspend fun updateOption(option: OptionEntity)

    @Query("DELETE FROM pick_history WHERE id NOT IN (SELECT id FROM pick_history ORDER BY pickedAt DESC LIMIT :limit)")
    suspend fun trimHistory(limit: Int)

    @Query("DELETE FROM round_state WHERE jarId = :jarId")
    suspend fun clearRoundState(jarId: String)

    @Query("DELETE FROM jars")
    suspend fun deleteAllJars()

    @Query("DELETE FROM pick_history")
    suspend fun deleteAllHistory()
}
