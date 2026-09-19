package com.example.inkora.model

import androidx.compose.ui.geometry.Rect
import java.util.UUID

enum class ToolType {
    PEN,
    PENCIL,
    HIGHLIGHTER,
    ERASER,
    LASSO,
    SHAPE
}

enum class ShapeType {
    NONE,
    LINE,
    ARROW,
    RECTANGLE,
    ROUNDED_RECTANGLE,
    CIRCLE,
    TRIANGLE
}

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class HandwritingStroke(
    val id: String = UUID.randomUUID().toString(),
    val tool: ToolType = ToolType.PEN,
    val colorHex: String = "#1E1B4B",
    val strokeWidth: Float = 4.0f,
    val opacity: Float = 1.0f,
    val points: List<StrokePoint> = emptyList(),
    val shapeType: ShapeType = ShapeType.NONE,
    val isSelected: Boolean = false
) {
    fun calculateBounds(): Rect {
        if (points.isEmpty()) return Rect.Zero
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        points.forEach { pt ->
            if (pt.x < minX) minX = pt.x
            if (pt.y < minY) minY = pt.y
            if (pt.x > maxX) maxX = pt.x
            if (pt.y > maxY) maxY = pt.y
        }
        val padding = strokeWidth / 2f
        return Rect(minX - padding, minY - padding, maxX + padding, maxY + padding)
    }

    fun translated(dx: Float, dy: Float): HandwritingStroke {
        return copy(points = points.map { it.copy(x = it.x + dx, y = it.y + dy) })
    }

    fun scaled(scaleX: Float, scaleY: Float, pivotX: Float, pivotY: Float): HandwritingStroke {
        return copy(points = points.map {
            it.copy(
                x = pivotX + (it.x - pivotX) * scaleX,
                y = pivotY + (it.y - pivotY) * scaleY
            )
        })
    }
}
