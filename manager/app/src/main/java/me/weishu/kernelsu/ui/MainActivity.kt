package me.weishu.kernelsu.ui

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.channels.Channel
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.data.ksu.install

class MainActivity : ComponentActivity() {

    /** Buffered event channel so duplicate links re-navigate and launch intents survive until composition starts. */
    private val intentChannel = Channel<Intent>(capacity = Channel.BUFFERED)

    private var contentReady = false
    private var splashStartedAt = 0L
    private val splashAnimationDurationMs = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashStartedAt = SystemClock.uptimeMillis()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition {
            !contentReady || SystemClock.uptimeMillis() - splashStartedAt < splashAnimationDurationMs
        }

        val isManager = Natives.isManager
        if (isManager && Natives.kernelUAPIVersion == Natives.managerUAPIVersion) install()

        // Transparent system bars; BreezeTheme owns the icon contrast from here on.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        window.isNavigationBarContrastEnforced = false

        // Route launch intent only on cold start; configuration changes restore the saved back stack.
        if (savedInstanceState == null) intent?.let { intentChannel.trySend(it) }

        setContent {
            BreezeApp(intentChannel) { contentReady = true }
        }
    }

    /** Updates [getIntent] and forwards warm deep links to [BreezeApp]. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentChannel.trySend(intent)
    }
}
