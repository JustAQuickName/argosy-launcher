package com.nendo.argosy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_pc_launch_targets",
    foreignKeys = [
        ForeignKey(
            entity = LocalPcEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("entryId"),
        Index(value = ["entryId", "runnerType", "runnerGameId", "packageName"], unique = true)
    ]
)
data class LocalPcLaunchTargetEntity(
    @PrimaryKey
    val id: String,
    val entryId: Long,
    val runnerType: String,
    val runnerGameId: String,
    val packageName: String,
    val displayName: String? = null,
    val componentName: String? = null,
    val action: String? = null,
    val executablePath: String? = null,
    val launchArguments: String? = null,
    val enabled: Boolean = true,
    val verifiedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)
