package com.nendo.argosy.data.local.migrations

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nendo.argosy.data.local.ALauncherDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val LOCAL_PC_TEST_DB = "local-pc-migration-test.db"

@RunWith(AndroidJUnit4::class)
class Migration223To224DataTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ALauncherDatabase::class.java
    )

    @Test
    fun localPcTablesHaveIdentityAndTargetConstraints() {
        helper.createDatabase(LOCAL_PC_TEST_DB, 223).close()

        helper.runMigrationsAndValidate(LOCAL_PC_TEST_DB, 224, true, Migration_223_224).use { db ->
            db.query("PRAGMA table_info(local_pc_entries)").use { rows ->
                val columns = mutableSetOf<String>()
                while (rows.moveToNext()) columns += rows.getString(rows.getColumnIndexOrThrow("name"))
                assertTrue(columns.containsAll(setOf("id", "gameId", "stableId", "defaultTargetId", "sourceFingerprint")))
            }
            db.query("PRAGMA index_list(local_pc_entries)").use { rows ->
                val unique = mutableMapOf<String, Int>()
                while (rows.moveToNext()) {
                    unique[rows.getString(rows.getColumnIndexOrThrow("name"))] =
                        rows.getInt(rows.getColumnIndexOrThrow("unique"))
                }
                assertEquals(1, unique["index_local_pc_entries_gameId"])
                assertEquals(1, unique["index_local_pc_entries_stableId"])
            }
            db.query("PRAGMA foreign_key_list(local_pc_launch_targets)").use { rows ->
                assertTrue(rows.moveToFirst())
                assertEquals("local_pc_entries", rows.getString(rows.getColumnIndexOrThrow("table")))
                assertEquals("CASCADE", rows.getString(rows.getColumnIndexOrThrow("on_delete")))
            }
        }
    }
}
