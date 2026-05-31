package com.accu.ui.appmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accu.ui.components.ACCTopBar

data class UsageStat(
    val appName: String,
    val pkg: String,
    val todayMins: Int,
    val weekMins: Int,
    val launchCount: Int,
    val lastUsed: String,
    val category: String = "Social",
    val dailyLimitMins: Int = 0,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InureUsageStatsScreen(onBack: () -> Unit = {}) {
    var period by remember { mutableStateOf(0) } // 0=Today, 1=Week, 2=Month
    val periods = listOf("Today", "This Week", "This Month")
    var sortBy by remember { mutableStateOf("usage") } // usage | launches | last_used
    var showLimitDialog by remember { mutableStateOf<UsageStat?>(null) }
    var snackbar by remember { mutableStateOf<String?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(snackbar) { snackbar?.let { snackbarHost.showSnackbar(it); snackbar = null } }

    val stats by remember {
        mutableStateOf(listOf(
            UsageStat("YouTube", "com.google.android.youtube", 142, 680, 18, "Now", "Entertainment", 60),
            UsageStat("Chrome", "com.android.chrome", 87, 420, 34, "2 min ago", "Productivity"),
            UsageStat("WhatsApp", "com.whatsapp", 71, 390, 58, "5 min ago", "Social", 45),
            UsageStat("Gmail", "com.google.android.gm", 43, 210, 12, "1 hr ago", "Productivity"),
            UsageStat("Spotify", "com.spotify.music", 38, 180, 7, "2 hr ago", "Entertainment"),
            UsageStat("Maps", "com.google.android.apps.maps", 31, 145, 5, "Yesterday", "Navigation"),
            UsageStat("Slack", "com.slack", 28, 390, 44, "30 min ago", "Productivity"),
            UsageStat("Instagram", "com.instagram.android", 22, 180, 8, "1 hr ago", "Social", 30),
            UsageStat("Telegram", "org.telegram.messenger", 18, 120, 22, "45 min ago", "Social"),
            UsageStat("Settings", "com.android.settings", 12, 40, 9, "3 hr ago", "System"),
            UsageStat("Reddit", "com.reddit.frontpage", 8, 95, 4, "4 hr ago", "Social"),
            UsageStat("GitHub", "com.github.android", 6, 45, 11, "Yesterday", "Productivity"),
        ))
    }

    val sorted = stats.sortedByDescending {
        when (sortBy) {
            "launches" -> it.launchCount.toDouble()
            "last_used" -> it.todayMins.toDouble()
            else -> if (period == 0) it.todayMins.toDouble() else it.weekMins.toDouble()
        }
    }
    val totalMins = if (period == 0) stats.sumOf { it.todayMins } else stats.sumOf { it.weekMins }

    Scaffold(
        topBar = {
            ACCTopBar(
                title = "Usage Statistics",
                onBack = onBack,
                actions = {
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) { Icon(Icons.Default.Sort, "Sort") }
                        DropdownMenu(showSortMenu, { showSortMenu = false }) {
                            DropdownMenuItem(text = { Text("By Usage Time") }, leadingIcon = { if (sortBy == "usage") Icon(Icons.Default.Check, null) }, onClick = { sortBy = "usage"; showSortMenu = false })
                            DropdownMenuItem(text = { Text("By Launch Count") }, leadingIcon = { if (sortBy == "launches") Icon(Icons.Default.Check, null) }, onClick = { sortBy = "launches"; showSortMenu = false })
                            DropdownMenuItem(text = { Text("By Last Used") }, leadingIcon = { if (sortBy == "last_used") Icon(Icons.Default.Check, null) }, onClick = { sortBy = "last_used"; showSortMenu = false })
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
                start = padding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                end = padding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
            ),
        ) {
            // Period selector
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(periods.size) { i ->
                        FilterChip(selected = period == i, onClick = { period = i }, label = { Text(periods[i]) })
                    }
                }
            }

            // Summary card with ring chart + bar chart
            item {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            // Ring chart
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                                Canvas(modifier = Modifier.size(80.dp)) {
                                    val stroke = Stroke(12.dp.toPx(), cap = StrokeCap.Round)
                                    drawArc(Color.White.copy(0.2f), 0f, 360f, false, style = stroke)
                                    val fraction = (if (period == 0) totalMins.toFloat() / 600 else totalMins.toFloat() / 4200).coerceIn(0f, 1f)
                                    drawArc(Color.White, -90f, 360f * fraction, false, style = stroke)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${totalMins / 60}h", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("${totalMins % 60}m", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(periods[period] + " Screen Time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                val hrs = totalMins / 60; val mins = totalMins % 60
                                Text("${hrs}h ${mins}m total", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("${sorted.firstOrNull()?.appName ?: ""} most used", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                                Spacer(Modifier.height(4.dp))
                                Text("${stats.sumOf { it.launchCount }} total launches · ${stats.size} apps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                            }
                        }

                        // 7-day bar chart (visible always)
                        Text("Daily Usage (last 7 days)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                        val days = listOf("M" to 340, "T" to 280, "W" to 420, "T" to 510, "F" to 390, "S" to 680, "S" to totalMins.coerceAtMost(700))
                        val maxVal = days.maxOf { it.second }.toFloat()
                        Row(Modifier.fillMaxWidth().height(50.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            days.forEach { (label, value) ->
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    val frac = (value / maxVal).coerceIn(0f, 1f)
                                    Box(Modifier.fillMaxWidth().fillMaxHeight(frac).background(Color.White.copy(if (label == "S") 1f else 0.4f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                                }
                            }
                        }
                    }
                }
            }

            // Category breakdown
            item {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("By Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        val categoryMap = stats.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.todayMins } }.entries.sortedByDescending { it.value }
                        categoryMap.forEach { (cat, mins) ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(cat, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(100.dp))
                                LinearProgressIndicator(
                                    progress = { (mins.toFloat() / (categoryMap.first().value.toFloat())).coerceIn(0f, 1f) },
                                    modifier = Modifier.weight(1f).height(6.dp),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("${mins}m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(36.dp))
                            }
                        }
                    }
                }
            }

            // Per-app list
            item {
                PaddingRow(horizontalPadding = 16.dp, topPadding = 8.dp, bottomPadding = 4.dp) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Per App", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Showing ${sorted.size} apps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(sorted, key = { it.pkg }) { stat ->
                val mins = if (period == 0) stat.todayMins else stat.weekMins
                val maxMins = if (period == 0) (sorted.firstOrNull()?.todayMins ?: 1) else (sorted.firstOrNull()?.weekMins ?: 1)
                val overLimit = stat.dailyLimitMins > 0 && stat.todayMins > stat.dailyLimitMins

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (overLimit) MaterialTheme.colorScheme.errorContainer.copy(0.4f) else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(stat.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                if (overLimit) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.errorContainer) {
                                        Text("LIMIT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                "${mins}m · ${stat.launchCount}x · last: ${stat.lastUsed}${if (stat.dailyLimitMins > 0) " · limit: ${stat.dailyLimitMins}m" else ""}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                            )
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (mins.toFloat() / maxMins).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                color = if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                            IconButton(onClick = { showLimitDialog = stat }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Timer, "Set limit", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    // Set daily limit dialog
    val limitStat = showLimitDialog
    if (limitStat != null) {
        var limitText by remember { mutableStateOf(if (limitStat.dailyLimitMins > 0) limitStat.dailyLimitMins.toString() else "") }
        AlertDialog(
            onDismissRequest = { showLimitDialog = null },
            icon = { Icon(Icons.Default.Timer, null) },
            title = { Text("Daily Limit — ${limitStat.appName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Set a daily usage limit. ACCU will alert you when you approach the limit.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = limitText, onValueChange = { limitText = it.filter { c -> c.isDigit() } },
                        label = { Text("Limit (minutes)") },
                        placeholder = { Text("0 = no limit") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    if (limitText.isNotBlank() && limitText.toIntOrNull() ?: 0 < limitStat.todayMins) {
                        Text("⚠ Today's usage (${limitStat.todayMins}m) already exceeds this limit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    snackbar = "Limit for ${limitStat.appName}: ${limitText.ifBlank { "0" }}m"
                    showLimitDialog = null
                }) { Text("Set Limit") }
            },
            dismissButton = { TextButton(onClick = { showLimitDialog = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PaddingRow(horizontalPadding: androidx.compose.ui.unit.Dp, topPadding: androidx.compose.ui.unit.Dp = 0.dp, bottomPadding: androidx.compose.ui.unit.Dp = 0.dp, content: @Composable () -> Unit) {
    Box(Modifier.padding(horizontal = horizontalPadding).padding(top = topPadding, bottom = bottomPadding)) { content() }
}
