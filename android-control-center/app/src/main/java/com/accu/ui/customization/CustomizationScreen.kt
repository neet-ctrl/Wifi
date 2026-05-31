package com.accu.ui.customization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accu.ui.components.ACCTopBar
import com.accu.ui.components.FeatureSwitch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    onNavigateToDarkMode: () -> Unit,
    onNavigateToColorEditor: () -> Unit,
    onBack: () -> Unit,
    viewModel: CustomizationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { ACCTopBar(title = "Customization", onBack = onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // Section: Theme
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Theme & Colors", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        // Monet style grid
                        Text("Material You Style", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        val monetStyles = listOf("TONAL_SPOT", "VIBRANT", "EXPRESSIVE", "SPRITZ", "RAINBOW", "FRUIT_SALAD", "CONTENT", "MONOCHROMATIC")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(monetStyles) { style ->
                                FilterChip(selected = state.monetStyle == style, onClick = { viewModel.setMonetStyle(style) }, label = { Text(style.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) })
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        // Seed color picker
                        Text("Seed Color", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        val seedColors = listOf(0xFF4A56E2, 0xFF00D4FF, 0xFF00E676, 0xFFFF6D00, 0xFFD500F9, 0xFFFF1744, 0xFFFFD600, 0xFFFF4081, 0xFF00BCD4, 0xFF8BC34A, 0xFF795548, 0xFF607D8B)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(seedColors) { color ->
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape).background(Color(color))
                                        .border(if (state.seedColor == color.toInt()) 3.dp else 0.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        .clickable { viewModel.setSeedColor(color.toInt()) }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onNavigateToColorEditor, Modifier.weight(1f)) { Icon(Icons.Default.Palette, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Color Editor") }
                            Button(onClick = { viewModel.applyTheme() }, Modifier.weight(1f)) { Text("Apply") }
                        }
                    }
                }
            }

            // Section: Display
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Display", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        FeatureSwitch(title = "Pitch Black Dark Mode", subtitle = "Pure black backgrounds on OLED screens", checked = state.pitchBlack, onCheckedChange = viewModel::togglePitchBlack, leadingIcon = { Icon(Icons.Default.DarkMode, null) })
                        FeatureSwitch(title = "Accurate Shades", subtitle = "Generate accurate color shades from seed", checked = state.accurateShades, onCheckedChange = viewModel::toggleAccurateShades, leadingIcon = { Icon(Icons.Default.ColorLens, null) })
                        ListItem(
                            headlineContent = { Text("Per-App Dark Mode") },
                            supportingContent = { Text("Force dark mode on specific apps (DarQ)") },
                            leadingContent = { Icon(Icons.Default.NightlightRound, null) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                            modifier = Modifier.clickable(onClick = onNavigateToDarkMode),
                        )
                    }
                }
            }

            // Section: Icons & Shapes
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Icons & Shapes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Icon Shape", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        val shapes = listOf("Circle", "Squircle", "Rounded Square", "Leaf", "Hexagon")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(shapes) { shape ->
                                FilterChip(selected = state.iconShape == shape, onClick = { viewModel.setIconShape(shape) }, label = { Text(shape) })
                            }
                        }
                    }
                }
            }

            // Section: Status Bar
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Status Bar & Navigation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        FeatureSwitch(title = "Transparent Status Bar", checked = state.transparentStatusBar, onCheckedChange = viewModel::toggleTransparentStatusBar)
                        FeatureSwitch(title = "Transparent Navigation Bar", checked = state.transparentNavBar, onCheckedChange = viewModel::toggleTransparentNavBar)
                        FeatureSwitch(title = "Hide Notch", subtitle = "Display in full screen ignoring cutout", checked = state.hideNotch, onCheckedChange = viewModel::toggleHideNotch)
                    }
                }
            }

            // Section: Font
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Typography", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Font Scale", style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("0.8×", style = MaterialTheme.typography.labelSmall)
                            Slider(value = state.fontScale, onValueChange = viewModel::setFontScale, valueRange = 0.8f..1.4f, steps = 5, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                            Text("1.4×", style = MaterialTheme.typography.labelSmall)
                        }
                        Text("Current: ${"%.1f".format(state.fontScale)}×", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Saved themes
            if (state.savedThemes.isNotEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Saved Themes (${state.savedThemes.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            state.savedThemes.forEach { theme ->
                                ListItem(
                                    headlineContent = { Text(theme.name) },
                                    supportingContent = { Text("${theme.monetStyle} · ${if (theme.isApplied) "Applied" else "Not applied"}") },
                                    trailingContent = {
                                        Row {
                                            if (!theme.isApplied) OutlinedButton(onClick = { viewModel.applyTheme(theme.id) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) { Text("Apply") }
                                            IconButton(onClick = { viewModel.deleteTheme(theme.id) }) { Icon(Icons.Default.Delete, "Delete", Modifier.size(16.dp)) }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
