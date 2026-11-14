package com.nendo.argosy.data.localpc

import com.nendo.argosy.data.model.LocalPcRunnerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalPcMarkerParserTest {
    @Test
    fun parsesPublicGameNativeMarker() {
        val draft = LocalPcMarkerParser.parseGameNative("My Game.pcgame", "42\n")

        requireNotNull(draft)
        assertEquals("My Game", draft.title)
        assertEquals(LocalPcRunnerType.GAME_NATIVE, draft.target.runnerType)
        assertEquals("42", draft.target.runnerGameId)
        assertEquals("game_native\u001fapp.gamenative\u001f42", draft.sourceFingerprint)
    }

    @Test
    fun rejectsWrongExtensionAndInvalidIdentifiers() {
        assertNull(LocalPcMarkerParser.parseGameNative("My Game.steam", "42"))
        assertNull(LocalPcMarkerParser.parseGameNative("My Game.pcgame", "0"))
        assertNull(LocalPcMarkerParser.parseGameNative("My Game.pcgame", "not-a-number"))
    }

    @Test
    fun fingerprintIsStableAcrossTitleChanges() {
        val first = LocalPcMarkerParser.parseGameNative("Old Title.pcgame", "99")
        val renamed = LocalPcMarkerParser.parseGameNative("New Title.pcgame", "99")

        assertEquals(first?.sourceFingerprint, renamed?.sourceFingerprint)
    }
}
