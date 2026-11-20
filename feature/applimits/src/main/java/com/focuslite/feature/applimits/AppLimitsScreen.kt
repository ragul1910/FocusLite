package com.focuslite.feature.applimits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuslite.core.model.AppBlockerRepository
import com.focuslite.core.model.BlockedApp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLimitsScreen(
    appBlockerRepo: AppBlockerRepository,
    onBack: () -> Unit
) {
    val blockedApps by appBlockerRepo.getBlockedApps().collectAsState(initial = emptyList())
    val limits by appBlockerRepo.getAllLimits().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Limits & Quotas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Configure daily usage allowances for distracting platforms.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(blockedApps) { app ->
                val limit = limits.find { it.packageName == app.packageName }
                AppLimitCard(
                    app = app,
                    quotaMinutes = limit?.dailyQuotaMinutes ?: 30,
                    isEnabled = limit?.isEnabled ?: false,
                    onToggle = { enabled ->
                        scope.launch {
                            appBlockerRepo.setAppLimit(app.packageName, limit?.dailyQuotaMinutes ?: 30, enabled)
                        }
                    },
                    onQuotaChange = { quota ->
                        scope.launch {
                            appBlockerRepo.setAppLimit(app.packageName, quota, limit?.isEnabled ?: true)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AppLimitCard(
    app: BlockedApp,
    quotaMinutes: Int,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onQuotaChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = app.appName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = if (isEnabled) "$quotaMinutes min / day limit" else "No limit active",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Switch(checked = isEnabled, onCheckedChange = onToggle)
            }

            if (isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = quotaMinutes.toFloat(),
                    onValueChange = { onQuotaChange(it.toInt()) },
                    valueRange = 10f..180f,
                    steps = 16
                )
            }
        }
    }
}
