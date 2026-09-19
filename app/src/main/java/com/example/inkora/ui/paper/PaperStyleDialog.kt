package com.example.inkora.ui.paper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.inkora.model.PaperBackground
import com.example.inkora.model.PaperType

@Composable
fun PaperStyleDialog(
    initialPaper: PaperBackground,
    onDismiss: () -> Unit,
    onSaveStyle: (PaperBackground) -> Unit,
    onSaveAsTemplate: ((PaperBackground, String) -> Unit)? = null
) {
    var selectedType by remember { mutableStateOf(initialPaper.type) }
    var selectedBgColorHex by remember { mutableStateOf(initialPaper.backgroundColorHex) }
    var selectedLineColorHex by remember { mutableStateOf(initialPaper.lineColorHex) }
    var spacing by remember { mutableFloatStateOf(initialPaper.spacingDp) }
    var thickness by remember { mutableFloatStateOf(initialPaper.lineThicknessDp) }
    var showMargin by remember { mutableStateOf(initialPaper.showMarginLine) }

    var templateName by remember { mutableStateOf("") }
    var showSaveTemplateInput by remember { mutableStateOf(false) }

    val bgPresets = listOf(
        "#FFFDF5" to "Classic Cream",
        "#FFFFFF" to "Crisp White",
        "#FEF9C3" to "Legal Yellow",
        "#F0FDF4" to "Soft Mint",
        "#F0F9FF" to "Sky Mist",
        "#FAF5FF" to "Lavender",
        "#FFF1F2" to "Rose Tint",
        "#F1F5F9" to "Slate",
        "#1E2433" to "Charcoal Dark",
        "#0F1218" to "Midnight Obsidian"
    )

    val linePresets = listOf(
        "#E2E8F0" to "Soft Gray",
        "#CBD5E1" to "Slate Border",
        "#93C5FD" to "Blue Ink",
        "#A7F3D0" to "Mint Guide",
        "#E5E0D8" to "Warm Sepia",
        "#334155" to "Dark Slate",
        "#242C3D" to "Obsidian Grid"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Page Style & Paper", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live preview preview card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    val previewPaper = PaperBackground(
                        type = selectedType,
                        backgroundColorHex = selectedBgColorHex,
                        lineColorHex = selectedLineColorHex,
                        spacingDp = spacing,
                        lineThicknessDp = thickness,
                        showMarginLine = showMargin
                    )
                    PaperBackgroundCanvas(paper = previewPaper)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {
                        Text(
                            "${selectedType.displayName} • ${spacing.toInt()}dp",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // 1. Paper Type Selection
                Text("Paper Pattern", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(PaperType.values()) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName) }
                        )
                    }
                }

                Divider()

                // 2. Paper Color Selection
                Text("Paper Background Color", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(bgPresets) { (hex, _) ->
                        val color = parseColorSafe(hex)
                        val isSelected = selectedBgColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { selectedBgColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (isPaperDark(PaperBackground(backgroundColorHex = hex))) Color.White else Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Divider()

                // 3. Line / Grid Color Selection
                Text("Line & Grid Color", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(linePresets) { (hex, _) ->
                        val color = parseColorSafe(hex)
                        val isSelected = selectedLineColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { selectedLineColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (isPaperDark(PaperBackground(backgroundColorHex = hex))) Color.White else Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Divider()

                // 4. Line Spacing Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Line / Grid Spacing", style = MaterialTheme.typography.bodyMedium)
                    Text("${spacing.toInt()} dp", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = spacing,
                    onValueChange = { spacing = it },
                    valueRange = 18f..48f,
                    steps = 15
                )

                // Margin toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Red / Margin Line", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = showMargin,
                        onCheckedChange = { showMargin = it }
                    )
                }

                if (onSaveAsTemplate != null) {
                    Divider()
                    if (!showSaveTemplateInput) {
                        OutlinedButton(
                            onClick = { showSaveTemplateInput = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save as Reusable Template")
                        }
                    } else {
                        OutlinedTextField(
                            value = templateName,
                            onValueChange = { templateName = it },
                            label = { Text("Template Name") },
                            placeholder = { Text("e.g. Physics Ruled") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val resultPaper = PaperBackground(
                        type = selectedType,
                        backgroundColorHex = selectedBgColorHex,
                        lineColorHex = selectedLineColorHex,
                        spacingDp = spacing,
                        lineThicknessDp = thickness,
                        showMarginLine = showMargin
                    )
                    if (showSaveTemplateInput && templateName.isNotBlank() && onSaveAsTemplate != null) {
                        onSaveAsTemplate(resultPaper, templateName.trim())
                    }
                    onSaveStyle(resultPaper)
                    onDismiss()
                }
            ) {
                Text("Apply Style")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
