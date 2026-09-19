package com.example.inkora.ui.paper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.example.inkora.model.PaperBackground
import com.example.inkora.model.PaperType

fun parseColorSafe(hex: String, fallback: Color = Color.White): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorInt = if (cleanHex.length == 6) {
            ("FF$cleanHex").toLong(16)
        } else {
            cleanHex.toLong(16)
        }
        Color(colorInt)
    } catch (_: Exception) {
        fallback
    }
}

fun isPaperDark(paper: PaperBackground): Boolean {
    val c = parseColorSafe(paper.backgroundColorHex, Color.White)
    // Relative luminance
    val lum = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue
    return lum < 0.4f
}

@Composable
fun PaperBackgroundCanvas(
    paper: PaperBackground,
    modifier: Modifier = Modifier
) {
    val bgColor = parseColorSafe(paper.backgroundColorHex, Color(0xFFFFFDF9))
    val lineColor = parseColorSafe(paper.lineColorHex, Color(0xFFE2E8F0))

    Canvas(modifier = modifier.fillMaxSize()) {
        // 1. Draw solid background paper
        drawRect(color = bgColor, size = size)

        val spacingPx = paper.spacingDp.dp.toPx()
        val thicknessPx = paper.lineThicknessDp.dp.toPx()
        val dotRadiusPx = paper.dotRadiusDp.dp.toPx()
        val marginPx = paper.marginOffsetDp.dp.toPx()

        when (paper.type) {
            PaperType.BLANK -> {
                // Completely plain
            }

            PaperType.RULED, PaperType.NARROW_RULED, PaperType.WIDE_RULED -> {
                val effectiveSpacing = when (paper.type) {
                    PaperType.NARROW_RULED -> spacingPx * 0.75f
                    PaperType.WIDE_RULED -> spacingPx * 1.35f
                    else -> spacingPx
                }

                // Draw horizontal ruled lines
                var y = effectiveSpacing
                while (y < size.height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = thicknessPx
                    )
                    y += effectiveSpacing
                }

                // Optional left vertical margin line
                if (paper.showMarginLine && marginPx > 0) {
                    val marginColor = lineColor.copy(alpha = 0.85f)
                    drawLine(
                        color = marginColor,
                        start = Offset(marginPx, 0f),
                        end = Offset(marginPx, size.height),
                        strokeWidth = thicknessPx * 1.2f
                    )
                }
            }

            PaperType.GRID -> {
                // Horizontal lines
                var y = spacingPx
                while (y < size.height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = thicknessPx
                    )
                    y += spacingPx
                }
                // Vertical lines
                var x = spacingPx
                while (x < size.width) {
                    drawLine(
                        color = lineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = thicknessPx
                    )
                    x += spacingPx
                }
            }

            PaperType.DOTTED -> {
                var y = spacingPx
                while (y < size.height) {
                    var x = spacingPx
                    while (x < size.width) {
                        drawCircle(
                            color = lineColor,
                            radius = dotRadiusPx,
                            center = Offset(x, y)
                        )
                        x += spacingPx
                    }
                    y += spacingPx
                }
            }

            PaperType.GRAPH -> {
                // Minor grid
                val minorSpacing = spacingPx / 4f
                var my = minorSpacing
                while (my < size.height) {
                    val isMajor = (my % spacingPx) < (minorSpacing * 0.5f)
                    val alpha = if (isMajor) 0.8f else 0.35f
                    val w = if (isMajor) thicknessPx * 1.5f else thicknessPx * 0.7f
                    drawLine(
                        color = lineColor.copy(alpha = alpha),
                        start = Offset(0f, my),
                        end = Offset(size.width, my),
                        strokeWidth = w
                    )
                    my += minorSpacing
                }
                var mx = minorSpacing
                while (mx < size.width) {
                    val isMajor = (mx % spacingPx) < (minorSpacing * 0.5f)
                    val alpha = if (isMajor) 0.8f else 0.35f
                    val w = if (isMajor) thicknessPx * 1.5f else thicknessPx * 0.7f
                    drawLine(
                        color = lineColor.copy(alpha = alpha),
                        start = Offset(mx, 0f),
                        end = Offset(mx, size.height),
                        strokeWidth = w
                    )
                    mx += minorSpacing
                }
            }

            PaperType.CORNELL -> {
                // Cornell layout:
                // Left column: Cue section (width = 30% of canvas)
                // Top-right: Main notes section with ruled lines
                // Bottom: Summary section (height = 20% of canvas)
                val cueWidth = size.width * 0.30f
                val summaryHeight = size.height * 0.20f
                val summaryTop = size.height - summaryHeight

                // Draw cue vertical dividing line
                drawLine(
                    color = lineColor,
                    start = Offset(cueWidth, 0f),
                    end = Offset(cueWidth, summaryTop),
                    strokeWidth = thicknessPx * 2f
                )

                // Draw summary horizontal dividing line
                drawLine(
                    color = lineColor,
                    start = Offset(0f, summaryTop),
                    end = Offset(size.width, summaryTop),
                    strokeWidth = thicknessPx * 2f
                )

                // Ruled lines in Main notes area (from cueWidth to size.width, top to summaryTop)
                var y = spacingPx
                while (y < summaryTop) {
                    drawLine(
                        color = lineColor.copy(alpha = 0.6f),
                        start = Offset(cueWidth, y),
                        end = Offset(size.width, y),
                        strokeWidth = thicknessPx
                    )
                    y += spacingPx
                }

                // Ruled lines in summary section
                var sy = summaryTop + spacingPx
                while (sy < size.height) {
                    drawLine(
                        color = lineColor.copy(alpha = 0.5f),
                        start = Offset(0f, sy),
                        end = Offset(size.width, sy),
                        strokeWidth = thicknessPx
                    )
                    sy += spacingPx
                }
            }

            PaperType.CHECKLIST -> {
                // Subtle ruled lines with check guide circles along the left margin
                var y = spacingPx
                val checkX = marginPx.coerceAtLeast(40f)
                while (y < size.height) {
                    // Line
                    drawLine(
                        color = lineColor.copy(alpha = 0.6f),
                        start = Offset(checkX + 24f, y),
                        end = Offset(size.width, y),
                        strokeWidth = thicknessPx
                    )
                    // Checkbox outline circle
                    drawCircle(
                        color = lineColor,
                        radius = 8f,
                        center = Offset(checkX, y - 8f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = thicknessPx * 1.2f)
                    )
                    y += spacingPx
                }
            }

            PaperType.MUSIC -> {
                // 5-line staff staves grouped together
                val staffLineSpacing = spacingPx * 0.35f
                val staffGroupSpacing = spacingPx * 2.8f
                var staffTop = spacingPx

                while (staffTop + (staffLineSpacing * 4) < size.height) {
                    for (i in 0..4) {
                        val ly = staffTop + (i * staffLineSpacing)
                        drawLine(
                            color = lineColor,
                            start = Offset(24f, ly),
                            end = Offset(size.width - 24f, ly),
                            strokeWidth = thicknessPx * 1.1f
                        )
                    }
                    staffTop += staffGroupSpacing
                }
            }
        }
    }
}
