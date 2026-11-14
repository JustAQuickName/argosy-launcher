package com.nendo.argosy.data.localpc

import androidx.room.withTransaction
import com.nendo.argosy.data.local.ALauncherDatabase
import com.nendo.argosy.data.local.dao.GameArtDao
import com.nendo.argosy.data.local.dao.GameDao
import com.nendo.argosy.data.local.dao.LocalPcDao
import com.nendo.argosy.data.local.dao.LocalPcEntryWithTargets
import com.nendo.argosy.data.local.dao.PlatformDao
import com.nendo.argosy.data.local.entity.GameEntity
import com.nendo.argosy.data.local.entity.LocalPcEntryEntity
import com.nendo.argosy.data.local.entity.LocalPcLaunchTargetEntity
import com.nendo.argosy.data.model.ArtSlot
import com.nendo.argosy.data.model.GameSource
import com.nendo.argosy.data.model.LocalPcRunnerType
import com.nendo.argosy.data.platform.LocalPlatformIds
import com.nendo.argosy.data.platform.PlatformDefinitions
import com.nendo.argosy.util.SearchNormalizer
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalPcRepository @Inject constructor(
    private val database: ALauncherDatabase,
    private val localPcDao: LocalPcDao,
    private val gameDao: GameDao,
    private val platformDao: PlatformDao,
    private val gameArtDao: GameArtDao
) {
    suspend fun getForGame(gameId: Long): LocalPcEntryWithTargets? =
        localPcDao.getEntryForGame(gameId)?.let { localPcDao.getEntryWithTargets(it.id) }

    suspend fun getAll(): List<LocalPcEntryWithTargets> = localPcDao.getAllWithTargets()

    suspend fun create(draft: LocalPcEntryDraft): Long = database.withTransaction {
        require(draft.title.isNotBlank())
        require(draft.target.runnerGameId.isNotBlank())
        ensurePlatform()
        val gameId = gameDao.insert(newGame(draft.title.trim()))
        val now = System.currentTimeMillis()
        val entryId = localPcDao.insertEntry(
            LocalPcEntryEntity(
                gameId = gameId,
                stableId = draft.stableId ?: UUID.randomUUID().toString(),
                sourceFingerprint = draft.sourceFingerprint,
                createdAt = now,
                updatedAt = now
            )
        )
        val target = draft.target.toEntity(entryId, now)
        localPcDao.insertTarget(target)
        localPcDao.setDefaultTarget(entryId, target.id, now)
        draft.coverPath?.let { setCover(gameId, it) }
        entryId
    }

    suspend fun import(drafts: List<LocalPcEntryDraft>): LocalPcImportResult {
        var added = 0
        var updated = 0
        var unchanged = 0
        val rejected = mutableListOf<String>()
        drafts.forEach { draft ->
            runCatching {
                database.withTransaction {
                    val fingerprint = draft.sourceFingerprint ?: LocalPcMarkerParser.fingerprint(draft.target)
                    val existing = localPcDao.getEntryByFingerprint(fingerprint)
                    if (existing == null) {
                        create(draft.copy(sourceFingerprint = fingerprint))
                        added++
                    } else {
                        val changed = mergeImport(existing, draft)
                        if (changed) updated++ else unchanged++
                    }
                }
            }.onFailure { rejected += draft.title }
        }
        return LocalPcImportResult(added, updated, unchanged, rejected)
    }

    suspend fun updateEntry(entryId: Long, title: String, coverPath: String?) = database.withTransaction {
        val entry = requireNotNull(localPcDao.getEntry(entryId))
        val game = requireNotNull(gameDao.getById(entry.gameId))
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty())
        gameDao.update(
            game.copy(
                title = cleanTitle,
                sortTitle = cleanTitle,
                searchTitle = SearchNormalizer.normalize(cleanTitle)
            )
        )
        if (coverPath != null) setCover(game.id, coverPath)
        localPcDao.updateEntry(entry.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun addTarget(entryId: Long, draft: LocalPcTargetDraft): String = database.withTransaction {
        requireNotNull(localPcDao.getEntry(entryId))
        require(draft.runnerGameId.isNotBlank())
        val now = System.currentTimeMillis()
        val target = draft.toEntity(entryId, now)
        localPcDao.insertTarget(target)
        target.id
    }

    suspend fun updateTarget(target: LocalPcLaunchTargetEntity) = database.withTransaction {
        require(localPcDao.targetBelongsToEntry(target.entryId, target.id) == 1)
        localPcDao.updateTarget(target.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun removeTarget(entryId: Long, targetId: String) = database.withTransaction {
        val entry = requireNotNull(localPcDao.getEntry(entryId))
        require(localPcDao.targetBelongsToEntry(entryId, targetId) == 1)
        localPcDao.deleteTarget(targetId)
        if (entry.defaultTargetId == targetId) {
            val replacement = localPcDao.getTargets(entryId).firstOrNull { it.enabled }?.id
            localPcDao.setDefaultTarget(entryId, replacement, System.currentTimeMillis())
        }
    }

    suspend fun setDefaultTarget(entryId: Long, targetId: String?) {
        if (targetId != null) require(localPcDao.targetBelongsToEntry(entryId, targetId) == 1)
        localPcDao.setDefaultTarget(entryId, targetId, System.currentTimeMillis())
    }

    suspend fun removeEntry(entryId: Long) = database.withTransaction {
        val entry = requireNotNull(localPcDao.getEntry(entryId))
        gameDao.delete(entry.gameId)
    }

    suspend fun resolveTarget(gameId: Long, oneShotTargetId: String? = null): LocalPcLaunchTargetEntity? {
        val entry = localPcDao.getEntryForGame(gameId) ?: return null
        val targets = localPcDao.getTargets(entry.id).filter { it.enabled }
        return oneShotTargetId?.let { id -> targets.firstOrNull { it.id == id } }
            ?: entry.defaultTargetId?.let { id -> targets.firstOrNull { it.id == id } }
            ?: targets.singleOrNull()
    }

    suspend fun exportBackup(): LocalPcBackup {
        val entries = localPcDao.getAllWithTargets().mapNotNull { row ->
            val game = gameDao.getById(row.entry.gameId) ?: return@mapNotNull null
            val cover = gameArtDao.get(game.id, ArtSlot.COVER.name)?.resolvedPath
            LocalPcBackupEntry(
                stableId = row.entry.stableId,
                title = game.title,
                coverPath = cover,
                defaultTargetId = row.entry.defaultTargetId,
                targets = row.targets.map { target ->
                    LocalPcBackupTarget(
                        id = target.id,
                        runnerType = target.runnerType,
                        runnerGameId = target.runnerGameId,
                        packageName = target.packageName,
                        displayName = target.displayName,
                        componentName = target.componentName,
                        action = target.action,
                        executablePath = target.executablePath,
                        launchArguments = target.launchArguments,
                        enabled = target.enabled
                    )
                }
            )
        }
        return LocalPcBackup(LocalPcBackupCodec.CURRENT_VERSION, entries)
    }

    suspend fun restoreBackup(backup: LocalPcBackup): LocalPcImportResult {
        require(backup.version in 1..LocalPcBackupCodec.CURRENT_VERSION)
        var added = 0
        var updated = 0
        val rejected = mutableListOf<String>()
        backup.entries.forEach { backupEntry ->
            val result = runCatching {
                database.withTransaction {
                    require(backupEntry.targets.isNotEmpty())
                    val existing = localPcDao.getEntryByStableId(backupEntry.stableId)
                    var entry = existing
                    if (entry == null) {
                        val first = backupEntry.targets.first().toDraft()
                        create(
                            LocalPcEntryDraft(
                                title = backupEntry.title,
                                stableId = backupEntry.stableId,
                                coverPath = backupEntry.coverPath,
                                target = first
                            )
                        )
                        entry = requireNotNull(localPcDao.getEntryByStableId(backupEntry.stableId))
                        val createdTarget = requireNotNull(
                            localPcDao.findTarget(
                                entry.id,
                                first.runnerType.token,
                                first.runnerGameId,
                                first.packageName
                            )
                        )
                        localPcDao.deleteTarget(createdTarget.id)
                    }
                    updateEntry(entry.id, backupEntry.title, backupEntry.coverPath)
                    backupEntry.targets.forEach { saved ->
                        val current = localPcDao.getTarget(saved.id)
                        if (current == null) {
                            localPcDao.insertTarget(saved.toEntity(entry.id))
                        } else if (current.entryId == entry.id) {
                            localPcDao.updateTarget(saved.toEntity(entry.id, current.createdAt))
                        } else {
                            error("Target identity collision")
                        }
                    }
                    val defaultId = backupEntry.defaultTargetId
                        ?.takeIf { localPcDao.targetBelongsToEntry(entry.id, it) == 1 }
                    localPcDao.setDefaultTarget(entry.id, defaultId, System.currentTimeMillis())
                    existing == null
                }
            }
            result.onSuccess { wasAdded ->
                if (wasAdded) added++ else updated++
            }.onFailure {
                rejected += backupEntry.title
            }
        }
        return LocalPcImportResult(added, updated, 0, rejected)
    }

    private fun LocalPcBackupTarget.toDraft(): LocalPcTargetDraft {
        val type = requireNotNull(LocalPcRunnerType.fromToken(runnerType))
        return LocalPcTargetDraft(
            runnerType = type,
            runnerGameId = runnerGameId,
            packageName = packageName,
            displayName = displayName,
            componentName = componentName,
            action = action,
            executablePath = executablePath,
            launchArguments = launchArguments
        )
    }

    private fun LocalPcBackupTarget.toEntity(entryId: Long, createdAt: Long = System.currentTimeMillis()) =
        LocalPcLaunchTargetEntity(
            id = id,
            entryId = entryId,
            runnerType = runnerType,
            runnerGameId = runnerGameId,
            packageName = packageName,
            displayName = displayName,
            componentName = componentName,
            action = action,
            executablePath = executablePath,
            launchArguments = launchArguments,
            enabled = enabled,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis()
        )

    private suspend fun mergeImport(entry: LocalPcEntryEntity, draft: LocalPcEntryDraft): Boolean {
        val current = localPcDao.findTarget(
            entry.id,
            draft.target.runnerType.token,
            draft.target.runnerGameId,
            draft.target.packageName
        )
        if (current != null) return false
        val now = System.currentTimeMillis()
        val target = draft.target.toEntity(entry.id, now)
        localPcDao.insertTarget(target)
        if (entry.defaultTargetId == null) localPcDao.setDefaultTarget(entry.id, target.id, now)
        return true
    }

    private suspend fun ensurePlatform() {
        if (platformDao.getById(LocalPlatformIds.LOCAL_PC) != null) return
        val def = requireNotNull(PlatformDefinitions.getBySlug(PLATFORM_SLUG))
        platformDao.insert(PlatformDefinitions.toEntity(LocalPlatformIds.LOCAL_PC, def))
    }

    private suspend fun setCover(gameId: Long, path: String) {
        gameArtDao.insertSlotIfAbsent(gameId, ArtSlot.COVER.name)
        gameArtDao.updateOverride(gameId, ArtSlot.COVER.name, path)
    }

    private fun newGame(title: String) = GameEntity(
        platformId = LocalPlatformIds.LOCAL_PC,
        platformSlug = PLATFORM_SLUG,
        title = title,
        sortTitle = title,
        localPath = null,
        rommId = null,
        igdbId = null,
        source = GameSource.LOCAL_PC,
        addedAt = Instant.now()
    )

    private fun LocalPcTargetDraft.toEntity(entryId: Long, now: Long) = LocalPcLaunchTargetEntity(
        id = UUID.randomUUID().toString(),
        entryId = entryId,
        runnerType = runnerType.token,
        runnerGameId = runnerGameId.trim(),
        packageName = packageName.trim(),
        displayName = displayName?.trim()?.takeIf { it.isNotEmpty() },
        componentName = componentName?.trim()?.takeIf { it.isNotEmpty() },
        action = action?.trim()?.takeIf { it.isNotEmpty() },
        executablePath = executablePath?.trim()?.takeIf { it.isNotEmpty() },
        launchArguments = launchArguments?.trim()?.takeIf { it.isNotEmpty() },
        createdAt = now,
        updatedAt = now
    )

    companion object {
        const val PLATFORM_SLUG = "local-pc"
    }
}
