package com.nendo.argosy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_pc_entries",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["gameId"], unique = true),
        Index(value = ["stableId"], unique = true)
    ]
)
data class LocalPcEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val stableId: String,
    val defaultTargetId: String? = null,
    val sourceFingerprint: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
