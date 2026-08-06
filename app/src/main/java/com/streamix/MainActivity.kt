package com.streamix

import android.os.Bundle
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.streamix.ui.navigation.StreamixNavGraph
import com.streamix.ui.theme.StreamixTheme
import com.streamix.core.analytics.AnalyticsManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.streamix.ui.player.PlayerManager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var analyticsManager: AnalyticsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        // Ensure portrait on start unless video was already playing (activity recreation)
        if (!PlayerManager.isVisible.value) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        setContent {
            StreamixTheme {
                StreamixNavGraph(analyticsManager)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
        // If coming back from background and player is NOT visible, force portrait
        if (!PlayerManager.isVisible.value) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}
