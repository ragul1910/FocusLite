package com.focuslite.feature.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Welcome to FocusLite",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Privacy Commitment Banner
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.padding(start = 12.dp))
                Column {
                    Text(
                        text = "100% Private & On-Device",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "FocusLite runs purely on your device without servers or tracking. It never inspects passwords, messages, or keystrokes. You retain complete control at all times.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Step 1: Accessibility Service
        PermissionCard(
            title = "1. Accessibility Service",
            description = "Required solely to detect when a restricted app or short-form video tab is opened, enabling immediate local dismissal.",
            icon = Icons.Default.Accessibility,
            actionLabel = "Open Accessibility Settings",
            onAction = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            }
        )

        // Motorola Edge 70 Fusion (Hello UI / Android 14+) Dedicated Setup Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2015)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D)
                    )
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    Text(
                        text = "Motorola Hello UI (Restricted Settings):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFFFB74D)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Android 13+ greys out Accessibility for sideloaded apps by default. To unlock it:\n\n" +
                            "1. Go to Phone Settings -> Apps -> FocusLite.\n" +
                            "2. Tap the three dots (⋮) in the top-right corner.\n" +
                            "3. Tap 'Allow restricted settings' and confirm with your PIN/fingerprint.\n" +
                            "4. Return here and tap 'Open Accessibility Settings' to turn FocusLite ON.",
                    fontSize = 13.sp,
                    color = Color(0xFFE0E0E0),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Open FocusLite App Info", color = Color(0xFFFFB74D))
                }
            }
        }

        // Step 2: Overlay Permission
        PermissionCard(
            title = "2. Display Over Other Apps",
            description = "Enables the calm full-screen break and downtime overlay when usage limits expire.",
            icon = Icons.Default.Layers,
            actionLabel = "Grant Overlay Permission",
            onAction = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        )

        // Step 3: Usage Access
        PermissionCard(
            title = "3. Usage Data Access",
            description = "Enables accurate calculation of daily foreground application screen time against your custom limits.",
            icon = Icons.Default.QueryStats,
            actionLabel = "Grant Usage Access",
            onAction = {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                context.startActivity(intent)
            }
        )

        // Step 4: Background Reliability (Motorola Moto Power Exemption)
        PermissionCard(
            title = "4. Background Reliability (Motorola)",
            description = "Exempts FocusLite from aggressive Motorola background task termination. Select 'Unrestricted' battery mode.",
            icon = Icons.Default.BatteryAlert,
            actionLabel = "Set Battery Unrestricted",
            onAction = {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Enter FocusLite Dashboard", modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.padding(start = 12.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}
