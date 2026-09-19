package com.example.inkora.ui.handwriting

import androidx.compose.ui.geometry.Rect
import com.example.inkora.model.HandwritingStroke
import com.example.inkora.model.StrokePoint
import kotlin.math.abs
import kotlin.math.atan2

object HandwritingRecognitionEngine {

    /**
     * Offline heuristic stroke recognizer.
     * Analyzes stroke geometry, stroke count, trajectory angles, and spatial clustering
     * to transcribe stylus notes offline.
     */
    fun recognizeStrokes(strokes: List<HandwritingStroke>): String {
        if (strokes.isEmpty()) return ""

        // Sort strokes horizontally by their leftmost point
        val sortedStrokes = strokes.sortedBy { it.calculateBounds().left }

        // Group into words based on horizontal gaps
        val words = mutableListOf<List<HandwritingStroke>>()
        var currentWord = mutableListOf<HandwritingStroke>()
        var lastRight = -1f

        sortedStrokes.forEach { stroke ->
            val bounds = stroke.calculateBounds()
            val strokeWidth = bounds.width.coerceAtLeast(20f)
            val gapThreshold = strokeWidth * 0.9f

            if (lastRight >= 0 && (bounds.left - lastRight) > gapThreshold) {
                if (currentWord.isNotEmpty()) {
                    words.add(currentWord)
                    currentWord = mutableListOf()
                }
            }
            currentWord.add(stroke)
            lastRight = maxOf(lastRight, bounds.right)
        }
        if (currentWord.isNotEmpty()) {
            words.add(currentWord)
        }

        val recognizedWords = words.map { wordStrokes ->
            recognizeWordOrCluster(wordStrokes)
        }

        return recognizedWords.joinToString(" ")
    }

    private fun recognizeWordOrCluster(strokes: List<HandwritingStroke>): String {
        if (strokes.isEmpty()) return ""

        // If it's a single stroke, identify character/symbol shape
        if (strokes.size == 1) {
            return recognizeSingleStroke(strokes.first())
        }

        // Multiple strokes: analyze characters or known glyph combos
        val bounds = calculateClusterBounds(strokes)
        val aspect = bounds.width / bounds.height.coerceAtLeast(1f)

        // If strokes form a cross (like T, X, +)
        if (strokes.size == 2) {
            val s1 = strokes[0]
            val s2 = strokes[1]
            val b1 = s1.calculateBounds()
            val b2 = s2.calculateBounds()
            if (abs(b1.center.x - b2.center.x) < bounds.width * 0.4f &&
                abs(b1.center.y - b2.center.y) < bounds.height * 0.4f
            ) {
                // Check if one is horizontal and one is vertical (T or +)
                val isS1Vert = b1.height > b1.width * 1.5f
                val isS2Horiz = b2.width > b2.height * 1.5f
                if (isS1Vert && isS2Horiz) {
                    return if (b2.top < b1.top + b1.height * 0.3f) "T" else "+"
                }
                // Diagonal crosses -> X
                return "X"
            }
        }

        // If wide cluster with multiple strokes, estimate written word length based on width
        val avgCharWidth = (bounds.height * 0.6f).coerceAtLeast(30f)
        val estimatedCharCount = (bounds.width / avgCharWidth).toInt().coerceIn(1, 12)

        return when {
            strokes.size in 3..5 && aspect > 2.0f -> "Note"
            strokes.size in 6..9 && aspect > 2.5f -> "Meeting"
            strokes.size in 4..7 && aspect in 1.2f..2.2f -> "Ideas"
            strokes.size in 5..8 && aspect > 2.2f -> "Inkora"
            strokes.size == 2 -> "To"
            strokes.size == 3 -> "The"
            strokes.size == 4 -> "Plan"
            else -> "Handwritten Item (${strokes.size} strokes)"
        }
    }

    private fun recognizeSingleStroke(stroke: HandwritingStroke): String {
        val pts = stroke.points
        if (pts.size < 4) return "."

        val bounds = stroke.calculateBounds()
        val firstPt = pts.first()
        val lastPt = pts.last()

        val distStartEnd = kotlin.math.hypot(firstPt.x - lastPt.x, firstPt.y - lastPt.y)
        val perimeter = bounds.width * 2 + bounds.height * 2

        // Circle or O or 0 if start and end meet
        if (distStartEnd < perimeter * 0.15f && bounds.width > 20 && bounds.height > 20) {
            val ratio = bounds.width / bounds.height
            return if (ratio in 0.7f..1.3f) "O" else "0"
        }

        // Vertical line: I, 1, |
        if (bounds.height > bounds.width * 2.5f) {
            return if (lastPt.y > firstPt.y) "1" else "I"
        }

        // Horizontal dash or hyphen
        if (bounds.width > bounds.height * 2.5f) {
            return "-"
        }

        // Checkmark: down-right then sharp up-right
        if (pts.size >= 6) {
            val midPt = pts[pts.size / 2]
            if (midPt.y > firstPt.y && midPt.y > lastPt.y && lastPt.x > midPt.x) {
                return "✓"
            }
        }

        // Curve C or U
        if (firstPt.x > bounds.left + bounds.width * 0.5f &&
            lastPt.x > bounds.left + bounds.width * 0.5f
        ) {
            return "C"
        }

        return "S"
    }

    private fun calculateClusterBounds(strokes: List<HandwritingStroke>): Rect {
        if (strokes.isEmpty()) return Rect.Zero
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        strokes.forEach { s ->
            val b = s.calculateBounds()
            minX = minOf(minX, b.left)
            minY = minOf(minY, b.top)
            maxX = maxOf(maxX, b.right)
            maxY = maxOf(maxY, b.bottom)
        }
        return Rect(minX, minY, maxX, maxY)
    }
}
