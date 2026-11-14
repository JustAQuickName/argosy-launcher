package com.nendo.argosy.data.localpc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.nendo.argosy.data.local.entity.LocalPcLaunchTargetEntity
import com.nendo.argosy.data.model.LocalPcRunnerType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class LocalPcDiagnosticCode {
    READY,
    PACKAGE_MISSING,
    INVALID_GAME_ID,
    COMPONENT_UNAVAILABLE,
    STOCK_CONTRACT_UNVERIFIED,
    UNKNOWN_RUNNER
}

data class LocalPcRunnerDiagnostic(
    val code: LocalPcDiagnosticCode,
    val detail: String? = null
) {
    val canLaunch: Boolean get() = code == LocalPcDiagnosticCode.READY
}

data class LocalPcPreparedLaunch(
    val diagnostic: LocalPcRunnerDiagnostic,
    val intent: Intent?
)

@Singleton
class LocalPcRunnerLauncher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun prepare(target: LocalPcLaunchTargetEntity): LocalPcPreparedLaunch {
        val runner = LocalPcRunnerType.fromToken(target.runnerType)
            ?: return failed(LocalPcDiagnosticCode.UNKNOWN_RUNNER)
        if (target.runnerGameId.isBlank()) return failed(LocalPcDiagnosticCode.INVALID_GAME_ID)
        if (runner == LocalPcRunnerType.GAME_NATIVE && target.runnerGameId.toIntOrNull()?.takeIf { it > 0 } == null) {
            return failed(LocalPcDiagnosticCode.INVALID_GAME_ID)
        }
        if (runner == LocalPcRunnerType.GAMEHUB_FULL_STOCK) {
            return failed(LocalPcDiagnosticCode.STOCK_CONTRACT_UNVERIFIED)
        }
        val intent = createIntent(target, runner)
        val resolved = context.packageManager.resolveActivity(intent, 0)
            ?: return failed(LocalPcDiagnosticCode.COMPONENT_UNAVAILABLE, target.packageName)
        if (resolved.activityInfo.packageName != target.packageName) {
            return failed(LocalPcDiagnosticCode.COMPONENT_UNAVAILABLE, target.packageName)
        }
        return LocalPcPreparedLaunch(LocalPcRunnerDiagnostic(LocalPcDiagnosticCode.READY), intent)
    }

    fun launch(target: LocalPcLaunchTargetEntity): LocalPcRunnerDiagnostic {
        val prepared = prepare(target)
        val intent = prepared.intent ?: return prepared.diagnostic
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return prepared.diagnostic
    }

    private fun createIntent(target: LocalPcLaunchTargetEntity, runner: LocalPcRunnerType): Intent {
        val component = target.componentName ?: when (runner) {
            LocalPcRunnerType.GAME_NATIVE -> target.packageName + ".MainActivity"
            LocalPcRunnerType.GAMEHUB_FULL_PATCHED, LocalPcRunnerType.GAMEHUB_LITE ->
                GAMEHUB_DETAIL_ACTIVITY
            LocalPcRunnerType.GAMEHUB_FULL_STOCK -> ""
        }
        val action = target.action ?: when (runner) {
            LocalPcRunnerType.GAME_NATIVE -> GAMENATIVE_ACTION
            LocalPcRunnerType.GAMEHUB_FULL_PATCHED -> GAMEHUB_PATCHED_ACTION
            LocalPcRunnerType.GAMEHUB_LITE -> GAMEHUB_LITE_ACTION
            LocalPcRunnerType.GAMEHUB_FULL_STOCK -> ""
        }
        return Intent().apply {
            this.component = ComponentName(target.packageName, component)
            this.action = action
            when (runner) {
                LocalPcRunnerType.GAME_NATIVE -> {
                    putExtra("app_id", target.runnerGameId.toInt())
                    putExtra("game_source", "CUSTOM_GAME")
                    target.executablePath?.let { putExtra("exe", it) }
                    target.launchArguments?.let { putExtra("exec_args", it) }
                }
                LocalPcRunnerType.GAMEHUB_FULL_PATCHED -> {
                    putExtra("localGameId", target.runnerGameId)
                    putExtra("steamAppId", target.runnerGameId)
                    putExtra("autoStartGame", true)
                }
                LocalPcRunnerType.GAMEHUB_LITE -> {
                    putExtra("steamAppId", target.runnerGameId)
                    putExtra("autoStartGame", true)
                }
                LocalPcRunnerType.GAMEHUB_FULL_STOCK -> Unit
            }
        }
    }

    private fun failed(code: LocalPcDiagnosticCode, detail: String? = null) =
        LocalPcPreparedLaunch(LocalPcRunnerDiagnostic(code, detail), null)

    companion object {
        const val GAMENATIVE_ACTION = "app.gamenative.LAUNCH_GAME"
        const val GAMEHUB_PATCHED_ACTION = "com.gamehub.LAUNCH_GAME"
        const val GAMEHUB_LITE_ACTION = "gamehub.lite.LAUNCH_GAME"
        const val GAMEHUB_DETAIL_ACTIVITY = "com.xj.landscape.launcher.ui.gamedetail.GameDetailActivity"
    }
}
