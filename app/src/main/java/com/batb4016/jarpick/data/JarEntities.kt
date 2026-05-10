package com.batb4016.jarpick.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "jars")
data class JarEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val theme: String,
    val mode: String,
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long?,
    val lastPickedOptionId: String?,
    val currentRoundId: String?,
)

@Entity(
    tableName = "options",
    foreignKeys = [
        ForeignKey(
            entity = JarEntity::class,
            parentColumns = ["id"],
            childColumns = ["jarId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("jarId")],
)
data class OptionEntity(
    @PrimaryKey val id: String,
    val jarId: String,
    val text: String,
    val notes: String?,
    val weight: Int,
    val isActive: Boolean,
    val isTemporarilyHidden: Boolean,
    val hiddenUntil: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long?,
    val totalPickedCount: Int,
    val lastPickedAt: Long?,
)

@Entity(
    tableName = "pick_history",
    indices = [Index("jarId"), Index("optionId"), Index("pickedAt")],
)
data class PickHistoryEntity(
    @PrimaryKey val id: String,
    val jarId: String,
    val optionId: String,
    val optionTextSnapshot: String,
    val jarNameSnapshot: String,
    val pickedAt: Long,
    val localDate: String,
    val mode: String,
    val accepted: Boolean?,
)

@Entity(
    tableName = "round_state",
    indices = [Index("jarId"), Index("optionId"), Index("roundId")],
)
data class RoundStateEntity(
    @PrimaryKey val id: String,
    val jarId: String,
    val optionId: String,
    val roundId: String,
    val usedAt: Long?,
    val eliminatedAt: Long?,
)

data class JarWithOptions(
    @Embedded val jar: JarEntity,
    @Relation(parentColumn = "id", entityColumn = "jarId")
    val options: List<OptionEntity>,
)
