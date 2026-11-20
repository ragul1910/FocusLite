package com.focuslite.feature.reelsblocker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.focuslite.core.model.ReelsShieldRepository
import com.focuslite.core.model.ShortFormPlatform
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsBlockerScreen(
    reelsShieldRepo: ReelsShieldRepository,
    onBack: () -> Unit
) {
    val masterEnabled by reelsShieldRepo.isReelsShieldEnabled().collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reels & Shorts Shield") },
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
                    text = "Automatically kicks you out of short-form algorithmic loops in native apps without blocking entire apps.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val platforms = listOf(
                ShortFormPlatform.INSTAGRAM to "Instagram Reels & Feed Videos",
                ShortFormPlatform.YOUTUBE to "YouTube Shorts & Watch Overlays",
                ShortFormPlatform.FACEBOOK to "Facebook Reels & Watch Tab",
                ShortFormPlatform.SNAPCHAT to "Snapchat Spotlight"
            )

            items(platforms.size) { index ->
                val (platform, description) = platforms[index]
                val isPlatformEnabled by reelsShieldRepo.isPlatformShieldEnabled(platform).collectAsState(initial = true)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = platform.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Switch(
                            checked = masterEnabled && isPlatformEnabled,
                            enabled = masterEnabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    reelsShieldRepo.setPlatformShieldToggle(platform, checked)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
