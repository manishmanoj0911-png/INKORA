package com.example.inkora.ui.handwriting

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import com.example.inkora.model.HandwritingStroke
import com.example.inkora.model.ShapeType
import com.example.inkora.model.StrokePoint
import com.example.inkora.model.ToolType
import com.example.inkora.ui.paper.parseColorSafe
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HandwritingCanvas(
    strokes: List<HandwritingStroke>,
    currentTool: ToolType,
    currentColorHex: String,
    currentThickness: Float,
    currentOpacity: Float,
    currentShape: ShapeType,
    isStylusOnly: Boolean,
    onStrokesChanged: (List<HandwritingStroke>) -> Unit,
    onLassoSelected: (List<HandwritingStroke>) -> Unit,
    modifier: Modifier = Modifier
) {
    var activePoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }
    var lassoPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedStrokeIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isMovingSelection by remember { mutableStateOf(false) }
    var lastMoveOffset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInteropFilter { motionEvent ->
                // Palm rejection / Stylus detection
                val isStylus = motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
                if (isStylusOnly && !isStylus && motionEvent.action != MotionEvent.ACTION_UP) {
                    return@pointerInteropFilter false
                }

                val x = motionEvent.x
                val y = motionEvent.y
                val pressure = motionEvent.pressure.coerceIn(0.2f, 1.5f)

                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        if (currentTool == ToolType.LASSO) {
                            lassoPath = listOf(Offset(x, y))
                        } else if (currentTool == ToolType.ERASER) {
                            // Erase hit strokes
                            val hitRadius = currentThickness * 3f
                            val remaining = strokes.filterNot { stroke ->
                                stroke.points.any { pt ->
                                    kotlin.math.hypot(pt.x - x, pt.y - y) < hitRadius
                                }
                            }
                            if (remaining.size != strokes.size) {
                                onStrokesChanged(remaining)
                            }
                        } else {
                            activePoints = listOf(StrokePoint(x, y, pressure))
                        }
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val historySize = motionEvent.historySize
                        if (currentTool == ToolType.LASSO) {
                            val newPts = lassoPath.toMutableList()
                            for (h in 0 until historySize) {
                                newPts.add(Offset(motionEvent.getHistoricalX(h), motionEvent.getHistoricalY(h)))
                            }
                            newPts.add(Offset(x, y))
                            lassoPath = newPts
                        } else if (currentTool == ToolType.ERASER) {
                            val hitRadius = currentThickness * 3f
                            val remaining = strokes.filterNot { stroke ->
                                stroke.points.any { pt ->
                                    kotlin.math.hypot(pt.x - x, pt.y - y) < hitRadius
                                }
                            }
                            if (remaining.size != strokes.size) {
                                onStrokesChanged(remaining)
                            }
                        } else {
                            val newPts = activePoints.toMutableList()
                            for (h in 0 until historySize) {
                                val hx = motionEvent.getHistoricalX(h)
                                val hy = motionEvent.getHistoricalY(h)
                                val hp = motionEvent.getHistoricalPressure(h).coerceIn(0.2f, 1.5f)
                                newPts.add(StrokePoint(hx, hy, hp))
                            }
                            newPts.add(StrokePoint(x, y, pressure))
                            activePoints = newPts
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (currentTool == ToolType.LASSO) {
                            if (lassoPath.size > 5) {
                                // Find enclosed strokes
                                val lassoBounds = calculateLassoBounds(lassoPath)
                                val enclosed = strokes.filter { stroke ->
                                    val sb = stroke.calculateBounds()
                                    lassoBounds.contains(sb.center)
                                }
                                selectedStrokeIds = enclosed.map { it.id }.toSet()
                                onLassoSelected(enclosed)
                            }
                            lassoPath = emptyList()
                        } else if (activePoints.isNotEmpty()) {
                            val newStroke = HandwritingStroke(
                                tool = currentTool,
                                colorHex = currentColorHex,
                                strokeWidth = currentThickness,
                                opacity = currentOpacity,
                                points = activePoints,
                                shapeType = currentShape
                            )
                            onStrokesChanged(strokes + newStroke)
                            activePoints = emptyList()
                        }
                    }
                }
                true
            }
    ) {
        // 1. Draw existing strokes
        strokes.forEach { stroke ->
            drawSingleStroke(stroke, isSelected = selectedStrokeIds.contains(stroke.id))
        }

        // 2. Draw currently active drawing stroke
        if (activePoints.isNotEmpty()) {
            val liveStroke = HandwritingStroke(
                tool = currentTool,
                colorHex = currentColorHex,
                strokeWidth = currentThickness,
                opacity = currentOpacity,
                points = activePoints,
                shapeType = currentShape
            )
            drawSingleStroke(liveStroke, isSelected = false)
        }

        // 3. Draw lasso selection path
        if (lassoPath.size > 1) {
            val path = Path().apply {
                moveTo(lassoPath[0].x, lassoPath[0].y)
                for (i in 1 until lassoPath.size) {
                    lineTo(lassoPath[i].x, lassoPath[i].y)
                }
            }
            drawPath(
                path = path,
                color = Color(0xFF6366F1),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )
            )
        }

        // 4. Draw bounding box around selected strokes
        if (selectedStrokeIds.isNotEmpty()) {
            val selected = strokes.filter { selectedStrokeIds.contains(it.id) }
            if (selected.isNotEmpty()) {
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = Float.MIN_VALUE
                var maxY = Float.MIN_VALUE
                selected.forEach { s ->
                    val b = s.calculateBounds()
                    minX = minOf(minX, b.left)
                    minY = minOf(minY, b.top)
                    maxX = maxOf(maxX, b.right)
                    maxY = maxOf(maxY, b.bottom)
                }
                val padding = 8f
                val box = Rect(minX - padding, minY - padding, maxX + padding, maxY + padding)
                drawRect(
                    color = Color(0xFF4F46E5),
                    topLeft = box.topLeft,
                    size = box.size,
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)))
                )
            }
        }
    }
}

private fun DrawScope.drawSingleStroke(stroke: HandwritingStroke, isSelected: Boolean) {
    val baseColor = parseColorSafe(stroke.colorHex)
    val color = if (isSelected) {
        Color(0xFF4F46E5)
    } else {
        baseColor.copy(alpha = stroke.opacity)
    }

    val pts = stroke.points
    if (pts.isEmpty()) return

    // Shapes rendering
    if (stroke.shapeType != ShapeType.NONE && pts.size >= 2) {
        val start = Offset(pts.first().x, pts.first().y)
        val end = Offset(pts.last().x, pts.last().y)
        drawShape(stroke.shapeType, start, end, color, stroke.strokeWidth)
        return
    }

    if (pts.size == 1) {
        drawCircle(
            color = color,
            radius = stroke.strokeWidth / 2f,
            center = Offset(pts[0].x, pts[0].y)
        )
        return
    }

    val path = Path()
    path.moveTo(pts[0].x, pts[0].y)

    // Smooth spline interpolation using quadratic beziers
    for (i in 1 until pts.size) {
        val prev = pts[i - 1]
        val curr = pts[i]
        val midX = (prev.x + curr.x) / 2f
        val midY = (prev.y + curr.y) / 2f
        path.quadraticTo(prev.x, prev.y, midX, midY)
    }
    val last = pts.last()
    path.lineTo(last.x, last.y)

    val blendMode = if (stroke.tool == ToolType.HIGHLIGHTER) BlendMode.Multiply else BlendMode.SrcOver
    val cap = if (stroke.tool == ToolType.PENCIL) StrokeCap.Square else StrokeCap.Round

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = stroke.strokeWidth,
            cap = cap,
            join = StrokeJoin.Round
        ),
        blendMode = blendMode
    )
}

private fun DrawScope.drawShape(
    type: ShapeType,
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float
) {
    when (type) {
        ShapeType.LINE -> {
            drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        }

        ShapeType.ARROW -> {
            drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
            val angle = atan2(end.y - start.y, end.x - start.x)
            val arrowLength = (strokeWidth * 3.5f).coerceAtLeast(24f)
            val arrowAngle = Math.toRadians(25.0).toFloat()

            val p1 = Offset(
                end.x - arrowLength * cos(angle - arrowAngle),
                end.y - arrowLength * sin(angle - arrowAngle)
            )
            val p2 = Offset(
                end.x - arrowLength * cos(angle + arrowAngle),
                end.y - arrowLength * sin(angle + arrowAngle)
            )
            drawLine(color = color, start = end, end = p1, strokeWidth = strokeWidth, cap = StrokeCap.Round)
            drawLine(color = color, start = end, end = p2, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        }

        ShapeType.RECTANGLE -> {
            val left = minOf(start.x, end.x)
            val top = minOf(start.y, end.y)
            val width = kotlin.math.abs(end.x - start.x)
            val height = kotlin.math.abs(end.y - start.y)
            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = strokeWidth)
            )
        }

        ShapeType.ROUNDED_RECTANGLE -> {
            val left = minOf(start.x, end.x)
            val top = minOf(start.y, end.y)
            val width = kotlin.math.abs(end.x - start.x)
            val height = kotlin.math.abs(end.y - start.y)
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = strokeWidth)
            )
        }

        ShapeType.CIRCLE -> {
            val left = minOf(start.x, end.x)
            val top = minOf(start.y, end.y)
            val width = kotlin.math.abs(end.x - start.x)
            val height = kotlin.math.abs(end.y - start.y)
            drawOval(
                color = color,
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = strokeWidth)
            )
        }

        ShapeType.TRIANGLE -> {
            val left = minOf(start.x, end.x)
            val right = maxOf(start.x, end.x)
            val top = minOf(start.y, end.y)
            val bottom = maxOf(start.y, end.y)

            val pTop = Offset((left + right) / 2f, top)
            val pBottomLeft = Offset(left, bottom)
            val pBottomRight = Offset(right, bottom)

            val path = Path().apply {
                moveTo(pTop.x, pTop.y)
                lineTo(pBottomRight.x, pBottomRight.y)
                lineTo(pBottomLeft.x, pBottomLeft.y)
                close()
            }
            drawPath(path = path, color = color, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))
        }

        else -> {}
    }
}

private fun calculateLassoBounds(points: List<Offset>): Rect {
    if (points.isEmpty()) return Rect.Zero
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE

    points.forEach { pt ->
        minX = minOf(minX, pt.x)
        minY = minOf(minY, pt.y)
        maxX = maxOf(maxX, pt.x)
        maxY = maxOf(maxY, pt.y)
    }
    return Rect(minX, minY, maxX, maxY)
}
