package com.nendo.argosy.data.localpc

import com.nendo.argosy.data.model.LocalPcRunnerType

data class LocalPcTargetDraft(
    val runnerType: LocalPcRunnerType,
    val runnerGameId: String,
    val packageName: String,
    val displayName: String? = null,
    val componentName: String? = null,
    val action: String? = null,
    val executablePath: String? = null,
    val launchArguments: String? = null
)

data class LocalPcEntryDraft(
    val title: String,
    val stableId: String? = null,
    val sourceFingerprint: String? = null,
    val coverPath: String? = null,
    val target: LocalPcTargetDraft
)

data class LocalPcImportResult(
    val added: Int,
    val updated: Int,
    val unchanged: Int,
    val rejected: List<String>
)

enum class LocalPcImportDisposition { ADD, ADD_TARGET, UNCHANGED, REJECTED }

data class LocalPcImportPreviewItem(
    val draft: LocalPcEntryDraft?,
    val sourceName: String,
    val disposition: LocalPcImportDisposition,
    val existingGameId: Long? = null
)

data class LocalPcImportPreview(
    val items: List<LocalPcImportPreviewItem>,
    val unreadableSources: List<String>
)

data class LocalPcBackup(
    val version: Int,
    val entries: List<LocalPcBackupEntry>
)

data class LocalPcBackupEntry(
    val stableId: String,
    val title: String,
    val coverPath: String?,
    val defaultTargetId: String?,
    val targets: List<LocalPcBackupTarget>
)

data class LocalPcBackupTarget(
    val id: String,
    val runnerType: String,
    val runnerGameId: String,
    val packageName: String,
    val displayName: String?,
    val componentName: String?,
    val action: String?,
    val executablePath: String?,
    val launchArguments: String?,
    val enabled: Boolean
)
