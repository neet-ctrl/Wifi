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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorEditorScreen(
    onBack: () -> Unit,
    viewModel: CustomizationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var red by remember { mutableFloatStateOf(((state.seedColor shr 16) and 0xFF) / 255f) }
    var green by remember { mutableFloatStateOf(((state.seedColor shr 8) and 0xFF) / 255f) }
    var blue by remember { mutableFloatStateOf((state.seedColor and 0xFF) / 255f) }
    val previewColor = Color(red, green, blue)

    Scaffold(topBar = { ACCTopBar(title = "Color Editor", onBack = onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Preview
            item {
                Card(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().height(160.dp).background(previewColor), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Color Preview", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("#${Integer.toHexString(android.graphics.Color.rgb((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())).uppercase().padStart(6, '0')}", color = Color.White.copy(0.8f))
                        }
                    }
                }
            }

            // RGB Sliders
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("RGB Channels", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        ColorChannel("Red", red, Color(1f, 0f, 0f)) { red = it }
                        ColorChannel("Green", green, Color(0f, 0.7f, 0f)) { green = it }
                        ColorChannel("Blue", blue, Color(0f, 0.4f, 1f)) { blue = it }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            val color = android.graphics.Color.rgb((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
                            viewModel.setSeedColor(color)
                        }, Modifier.fillMaxWidth()) { Text("Set as Seed Color") }
                    }
                }
            }

            // Palette presets (ColorBlendr)
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Material You Palette Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Generated from current seed color using Material color system", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0.9f, 0.7f, 0.5f, 0.35f, 0.2f, 0.1f).forEach { alpha ->
                                Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(6.dp)).background(previewColor.copy(alpha = alpha)))
                            }
                        }
                    }
                }
            }

            // Monet styles
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Dynamic Color Styles (ColorBlendr)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        val styles = mapOf(
                            "TONAL_SPOT" to "Standard Monet palette generation",
                            "VIBRANT" to "High saturation, vivid colors",
                            "EXPRESSIVE" to "Bold and colorful expression",
                            "SPRITZ" to "Muted, desaturated tones",
                            "RAINBOW" to "Full spectrum colors",
                            "FRUIT_SALAD" to "Tropical, mixed palette",
                            "CONTENT" to "Colors extracted from wallpaper content",
                            "MONOCHROMATIC" to "Single-color scheme",
                        )
                        styles.forEach { (style, desc) ->
                            ListItem(
                                headlineContent = { Text(style.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                supportingContent = { Text(desc, style = MaterialTheme.typography.bodySmall) },
                                leadingContent = { RadioButton(selected = state.monetStyle == style, onClick = { viewModel.setMonetStyle(style) }) },
                                modifier = Modifier.clickable { viewModel.setMonetStyle(style) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorChannel(name: String, value: Float, color: Color, onChange: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(48.dp))
            Slider(value = value, onValueChange = onChange, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color), modifier = Modifier.weight(1f))
            Text("${(value * 255).toInt()}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(32.dp))
        }
    }
}
