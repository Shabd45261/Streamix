package com.streamix.ui.youtube

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.streamix.core.storage.PreferencesManager
import com.streamix.ui.theme.LocalCustomColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLoginScreen(
    navController: NavController,
    preferencesManager: PreferencesManager
) {
    val context = LocalContext.current
    val colors = LocalCustomColors.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.primary)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.secondary.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = colors.secondary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(
                "YouTube Login",
                color = colors.secondary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                super.onPageStarted(view, url, favicon)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                val cookies = CookieManager.getInstance().getCookie(url)
                                if (cookies != null && cookies.contains("SID=")) {
                                    // User likely logged in
                                    scope.launch {
                                        preferencesManager.setYoutubeAccount(cookies, "Logged In")
                                        navController.popBackStack()
                                    }
                                }
                                super.onPageFinished(view, url)
                            }
                        }
                        loadUrl("https://accounts.google.com/ServiceLogin?service=youtube")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = colors.tertiary,
                    trackColor = Color.Transparent
                )
            }
        }
        
        Text(
            "Login to sync your playlists and feed. Your credentials are only handled by Google.",
            modifier = Modifier.padding(16.dp),
            color = colors.secondary.copy(alpha = 0.6f),
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}
