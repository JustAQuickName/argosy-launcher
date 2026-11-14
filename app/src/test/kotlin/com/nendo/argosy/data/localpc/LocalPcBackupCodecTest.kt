package com.nendo.argosy.data.localpc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LocalPcBackupCodecTest {
    @Test
    fun versionOneRoundTripsEveryTargetField() {
        val backup = LocalPcBackup(
            version = 1,
            entries = listOf(
                LocalPcBackupEntry(
                    stableId = "entry-1",
                    title = "Game",
                    coverPath = "/cover.png",
                    defaultTargetId = "target-2",
                    targets = listOf(
                        LocalPcBackupTarget(
                            id = "target-1",
                            runnerType = "game_native",
                            runnerGameId = "42",
                            packageName = "app.gamenative",
                            displayName = "Portable",
                            componentName = null,
                            action = null,
                            executablePath = "D:/Game/game.exe",
                            launchArguments = "--safe",
                            enabled = true
                        ),
                        LocalPcBackupTarget(
                            id = "target-2",
                            runnerType = "gamehub_full_patched",
                            runnerGameId = "local-7",
                            packageName = "com.xiaoji.egggame",
                            displayName = null,
                            componentName = "activity",
                            action = "action",
                            executablePath = null,
                            launchArguments = null,
                            enabled = false
                        )
                    )
                )
            )
        )

        assertEquals(backup, LocalPcBackupCodec.decode(LocalPcBackupCodec.encode(backup)))
    }

    @Test
    fun rejectsUnknownFutureVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalPcBackupCodec.decode("""{"version":2,"entries":[]}""")
        }
    }
}
