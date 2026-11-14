package com.nendo.argosy.data.localpc

import com.nendo.argosy.data.launcher.GameNativeLauncher
import com.nendo.argosy.data.model.LocalPcRunnerType
import java.io.File

object LocalPcMarkerParser {
    fun parseGameNative(name: String, content: String): LocalPcEntryDraft? {
        if (!name.endsWith(EXTENSION, ignoreCase = true)) return null
        val id = content.trim().toIntOrNull()?.takeIf { it > 0 } ?: return null
        val title = File(name).nameWithoutExtension.trim().takeIf { it.isNotEmpty() } ?: return null
        val target = LocalPcTargetDraft(
            runnerType = LocalPcRunnerType.GAME_NATIVE,
            runnerGameId = id.toString(),
            packageName = GameNativeLauncher.packageName,
            displayName = GameNativeLauncher.displayName
        )
        return LocalPcEntryDraft(
            title = title,
            sourceFingerprint = fingerprint(target),
            target = target
        )
    }

    fun fingerprint(target: LocalPcTargetDraft): String =
        listOf(target.runnerType.token, target.packageName, target.runnerGameId).joinToString(SEPARATOR)

    private const val EXTENSION = ".pcgame"
    private const val SEPARATOR = "\u001f"
}
