package com.nendo.argosy.data.model

enum class LocalPcRunnerType(val token: String) {
    GAME_NATIVE("game_native"),
    GAMEHUB_FULL_PATCHED("gamehub_full_patched"),
    GAMEHUB_FULL_STOCK("gamehub_full_stock"),
    GAMEHUB_LITE("gamehub_lite");

    companion object {
        fun fromToken(token: String): LocalPcRunnerType? = entries.firstOrNull { it.token == token }
    }
}
