package com.streamix.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.streamix.core.model.Profile

@Composable
fun PasscodeScreen(
    navController: NavController,
    profileState: MutableState<Profile>
) {
    var passcode by remember { mutableStateOf("") }
    val correctPasscode = "1234" // Default PIN

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Back button on the top left
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(28.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Enter PIN", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)

            Spacer(Modifier.height(40.dp))

            // Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val filled = index < passcode.length
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(if (filled) Color.White else Color(0xFF333333), CircleShape)
                    )
                }
            }

            Spacer(Modifier.height(60.dp))

            // Keypad
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "DEL")
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                keys.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                        row.forEach { key ->
                            if (key.isEmpty()) {
                                Spacer(Modifier.size(60.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clickable {
                                            if (key == "DEL") {
                                                if (passcode.isNotEmpty()) passcode = passcode.dropLast(1)
                                            } else if (passcode.length < 4) {
                                                passcode += key
                                                if (passcode.length == 4) {
                                                    if (passcode == correctPasscode) {
                                                        profileState.value = Profile.ADULT
                                                        navController.popBackStack()
                                                    } else {
                                                        passcode = ""
                                                        // Show error
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (key == "DEL") {
                                        Icon(Icons.Default.Backspace, contentDescription = null, tint = Color.White)
                                    } else {
                                        Text(key, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
