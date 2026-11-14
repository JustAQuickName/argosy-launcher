package com.nendo.argosy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nendo.argosy.data.local.entity.LocalPcEntryEntity
import com.nendo.argosy.data.local.entity.LocalPcLaunchTargetEntity
import kotlinx.coroutines.flow.Flow

data class LocalPcEntryWithTargets(
    val entry: LocalPcEntryEntity,
    val targets: List<LocalPcLaunchTargetEntity>
)

@Dao
interface LocalPcDao {
    @Query("SELECT * FROM local_pc_entries ORDER BY updatedAt DESC")
    fun observeEntries(): Flow<List<LocalPcEntryEntity>>

    @Query("SELECT * FROM local_pc_entries ORDER BY updatedAt DESC")
    suspend fun getEntries(): List<LocalPcEntryEntity>

    @Query("SELECT * FROM local_pc_entries WHERE id = :id")
    suspend fun getEntry(id: Long): LocalPcEntryEntity?

    @Query("SELECT * FROM local_pc_entries WHERE gameId = :gameId")
    suspend fun getEntryForGame(gameId: Long): LocalPcEntryEntity?

    @Query("SELECT * FROM local_pc_entries WHERE stableId = :stableId")
    suspend fun getEntryByStableId(stableId: String): LocalPcEntryEntity?

    @Query("SELECT * FROM local_pc_entries WHERE sourceFingerprint = :fingerprint")
    suspend fun getEntryByFingerprint(fingerprint: String): LocalPcEntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: LocalPcEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: LocalPcEntryEntity)

    @Query("DELETE FROM local_pc_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("SELECT * FROM local_pc_launch_targets WHERE entryId = :entryId ORDER BY createdAt, id")
    fun observeTargets(entryId: Long): Flow<List<LocalPcLaunchTargetEntity>>

    @Query("SELECT * FROM local_pc_launch_targets WHERE entryId = :entryId ORDER BY createdAt, id")
    suspend fun getTargets(entryId: Long): List<LocalPcLaunchTargetEntity>

    @Query(
        "SELECT * FROM local_pc_launch_targets WHERE entryId = :entryId AND runnerType = :runnerType " +
            "AND runnerGameId = :runnerGameId AND packageName = :packageName"
    )
    suspend fun findTarget(entryId: Long, runnerType: String, runnerGameId: String, packageName: String):
        LocalPcLaunchTargetEntity?

    @Query("SELECT * FROM local_pc_launch_targets WHERE id = :id")
    suspend fun getTarget(id: String): LocalPcLaunchTargetEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTarget(target: LocalPcLaunchTargetEntity)

    @Update
    suspend fun updateTarget(target: LocalPcLaunchTargetEntity)

    @Query("DELETE FROM local_pc_launch_targets WHERE id = :id")
    suspend fun deleteTarget(id: String)

    @Query("UPDATE local_pc_entries SET defaultTargetId = :targetId, updatedAt = :updatedAt WHERE id = :entryId")
    suspend fun setDefaultTarget(entryId: Long, targetId: String?, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM local_pc_launch_targets WHERE entryId = :entryId AND id = :targetId")
    suspend fun targetBelongsToEntry(entryId: Long, targetId: String): Int

    @Transaction
    suspend fun getEntryWithTargets(id: Long): LocalPcEntryWithTargets? {
        val entry = getEntry(id) ?: return null
        return LocalPcEntryWithTargets(entry, getTargets(id))
    }

    @Transaction
    suspend fun getAllWithTargets(): List<LocalPcEntryWithTargets> =
        getEntries().map { LocalPcEntryWithTargets(it, getTargets(it.id)) }
}
