package me.weishu.kernelsu.ui.webui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * The module WebUI window. Owns nothing but the window: everything WebView-specific lives
 * below [WebUIEntry], so the two windows cannot drift apart.
 */
class WebUIActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Transparent system bars; BreezeTheme owns the icon contrast from here on.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        window.isNavigationBarContrastEnforced = false

        super.onCreate(savedInstanceState)

        setContent {
            BreezeWebUIRoot(this) { finish() }
        }
    }
}
