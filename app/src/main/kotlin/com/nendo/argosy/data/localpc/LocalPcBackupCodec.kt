package com.nendo.argosy.data.localpc

import org.json.JSONArray
import org.json.JSONObject

object LocalPcBackupCodec {
    const val CURRENT_VERSION = 1

    fun encode(backup: LocalPcBackup): String {
        val entries = JSONArray()
        backup.entries.forEach { entry ->
            val targets = JSONArray()
            entry.targets.forEach { target ->
                targets.put(
                    JSONObject()
                        .put("id", target.id)
                        .put("runnerType", target.runnerType)
                        .put("runnerGameId", target.runnerGameId)
                        .put("packageName", target.packageName)
                        .putNullable("displayName", target.displayName)
                        .putNullable("componentName", target.componentName)
                        .putNullable("action", target.action)
                        .putNullable("executablePath", target.executablePath)
                        .putNullable("launchArguments", target.launchArguments)
                        .put("enabled", target.enabled)
                )
            }
            entries.put(
                JSONObject()
                    .put("stableId", entry.stableId)
                    .put("title", entry.title)
                    .putNullable("coverPath", entry.coverPath)
                    .putNullable("defaultTargetId", entry.defaultTargetId)
                    .put("targets", targets)
            )
        }
        return JSONObject().put("version", backup.version).put("entries", entries).toString(2)
    }

    fun decode(text: String): LocalPcBackup {
        val root = JSONObject(text)
        val version = root.getInt("version")
        require(version in 1..CURRENT_VERSION)
        val entriesJson = root.getJSONArray("entries")
        val entries = buildList {
            for (index in 0 until entriesJson.length()) {
                val item = entriesJson.getJSONObject(index)
                val targetsJson = item.getJSONArray("targets")
                val targets = buildList {
                    for (targetIndex in 0 until targetsJson.length()) {
                        val target = targetsJson.getJSONObject(targetIndex)
                        add(
                            LocalPcBackupTarget(
                                id = target.getString("id"),
                                runnerType = target.getString("runnerType"),
                                runnerGameId = target.getString("runnerGameId"),
                                packageName = target.getString("packageName"),
                                displayName = target.optionalString("displayName"),
                                componentName = target.optionalString("componentName"),
                                action = target.optionalString("action"),
                                executablePath = target.optionalString("executablePath"),
                                launchArguments = target.optionalString("launchArguments"),
                                enabled = target.optBoolean("enabled", true)
                            )
                        )
                    }
                }
                add(
                    LocalPcBackupEntry(
                        stableId = item.getString("stableId"),
                        title = item.getString("title"),
                        coverPath = item.optionalString("coverPath"),
                        defaultTargetId = item.optionalString("defaultTargetId"),
                        targets = targets
                    )
                )
            }
        }
        return LocalPcBackup(version, entries)
    }

    private fun JSONObject.putNullable(key: String, value: String?): JSONObject =
        if (value == null) put(key, JSONObject.NULL) else put(key, value)

    private fun JSONObject.optionalString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
}
