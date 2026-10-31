package me.weishu.kernelsu.ui.screen.flash

import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.ksu.FlashResult
import me.weishu.kernelsu.data.ksu.downloadBoot
import me.weishu.kernelsu.data.ksu.flashModule
import me.weishu.kernelsu.data.ksu.installBoot
import me.weishu.kernelsu.data.ksu.restoreBoot
import me.weishu.kernelsu.data.ksu.uninstallPermanently
import me.weishu.kernelsu.data.model.FlashIt
import me.weishu.kernelsu.ui.component.breeze.dialog.ConfirmBody

enum class FlashingStatus {
    FLASHING,
    SUCCESS,
    FAILED
}

fun flashModulesSequentially(
    uris: List<Uri>,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    var needsReboot = false
    for (uri in uris) {
        flashModule(uri, onStdout, onStderr).apply {
            needsReboot = needsReboot || showReboot
            if (code != 0) {
                return FlashResult(code, err, needsReboot)
            }
        }
    }
    return FlashResult(0, "", needsReboot)
}

fun flashIt(
    flashIt: FlashIt,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    return when (flashIt) {
        is FlashIt.FlashBoot -> installBoot(
            flashIt.boot,
            flashIt.lkm,
            flashIt.ota,
            flashIt.partition,
            flashIt.allowShell,
            flashIt.enableAdb,
            flashIt.backup,
            onStdout,
            onStderr
        )

        is FlashIt.DownloadBoot -> downloadBoot(
            flashIt.url,
            flashIt.partition,
            flashIt.lkm,
            flashIt.allowShell,
            flashIt.enableAdb,
            flashIt.backup,
            onStdout,
            onStderr
        )

        is FlashIt.FlashModules -> {
            flashModulesSequentially(flashIt.uris, onStdout, onStderr)
        }

        FlashIt.FlashRestore -> restoreBoot(onStdout, onStderr)
        FlashIt.FlashUninstall -> uninstallPermanently(onStdout, onStderr)
    }
}

@Composable
fun FlashEffect(
    flashIt: FlashIt,
    text: String,
    logContent: StringBuilder,
    onTextUpdate: (String) -> Unit,
    onShowRebootChange: (Boolean) -> Unit,
    onFlashingStatusChange: (FlashingStatus) -> Unit,
    enabled: Boolean = true
) {
    LaunchedEffect(enabled) {
        if (!enabled || text.isNotEmpty()) {
            return@LaunchedEffect
        }
        var currentText = text
        val mainHandler = Handler(Looper.getMainLooper())
        withContext(Dispatchers.IO) {
            flashIt(flashIt, onStdout = {
                val tempText = "$it\n"
                if (tempText.startsWith("[H[J")) { // clear command
                    currentText = tempText.substring(6)
                } else {
                    currentText += tempText
                }
                mainHandler.post {
                    onTextUpdate(currentText)
                }
                logContent.append(it).append("\n")
            }, onStderr = {
                logContent.append(it).append("\n")
            }).apply {
                if (code != 0) {
                    currentText += "Error code: $code.\n $err Please save and check the log.\n"
                    mainHandler.post {
                        onTextUpdate(currentText)
                    }
                }
                if (showReboot) {
                    currentText += "\n\n\n"
                    mainHandler.post {
                        onTextUpdate(currentText)
                        onShowRebootChange(true)
                    }
                }
                mainHandler.post {
                    onFlashingStatusChange(if (code == 0) FlashingStatus.SUCCESS else FlashingStatus.FAILED)
                }
            }
        }
    }
}

fun saveLog(
    logContent: StringBuilder,
    scope: CoroutineScope,
    showMessage: (String) -> Unit
): () -> Unit {
    return {
        scope.launch {
            val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
            val date = format.format(Date())
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "KernelSU_install_log_${date}.log"
            )
            file.writeText(logContent.toString())
            showMessage("Log saved to ${file.absolutePath}")
        }
    }
}

private const val JAILBREAK_WARNING_COUNTDOWN = 10

/**
 * Body of the jailbreak warning, or null while it is not shown.
 *
 * Confirm stays disabled for [JAILBREAK_WARNING_COUNTDOWN] seconds so the warning cannot be
 * accepted reflexively; the label counts down in the same slot it ends up occupying.
 */
@Composable
fun rememberJailbreakFlashWarningBody(
    visible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
): ConfirmBody? {
    var countdown by remember { mutableIntStateOf(JAILBREAK_WARNING_COUNTDOWN) }

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        countdown = JAILBREAK_WARNING_COUNTDOWN
        while (countdown > 0) {
            delay(1000.milliseconds)
            countdown--
        }
    }

    if (!visible) return null
    return ConfirmBody(
        title = stringResource(android.R.string.dialog_alert_title),
        body = ConfirmBody.Body.Text(stringResource(R.string.jailbreak_flash_warning)),
        confirm = if (countdown > 0) {
            stringResource(R.string.jailbreak_flash_warning_countdown, countdown)
        } else {
            stringResource(R.string.install_next)
        },
        dismiss = stringResource(android.R.string.cancel),
        confirmEnabled = countdown == 0,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
