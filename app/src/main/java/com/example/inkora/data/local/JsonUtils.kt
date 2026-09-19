package com.example.inkora.data.local

import com.example.inkora.model.ChecklistItem
import com.example.inkora.model.HandwritingStroke
import com.example.inkora.model.PaperBackground
import com.example.inkora.model.PaperType
import com.example.inkora.model.ShapeType
import com.example.inkora.model.StrokePoint
import com.example.inkora.model.TableData
import com.example.inkora.model.ToolType
import org.json.JSONArray
import org.json.JSONObject

object JsonUtils {

    fun checklistsToJson(items: List<ChecklistItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("text", item.text)
            obj.put("isChecked", item.isChecked)
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToChecklists(json: String?): List<ChecklistItem> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<ChecklistItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ChecklistItem(
                        id = obj.optString("id"),
                        text = obj.optString("text"),
                        isChecked = obj.optBoolean("isChecked", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun tableToJson(table: TableData?): String {
        if (table == null) return ""
        val obj = JSONObject()
        obj.put("id", table.id)
        obj.put("hasHeader", table.hasHeader)
        val rowsArray = JSONArray()
        table.rows.forEach { row ->
            val rowArray = JSONArray()
            row.forEach { cell -> rowArray.put(cell) }
            rowsArray.put(rowArray)
        }
        obj.put("rows", rowsArray)
        return obj.toString()
    }

    fun jsonToTable(json: String?): TableData? {
        if (json.isNullOrBlank()) return null
        try {
            val obj = JSONObject(json)
            val id = obj.optString("id")
            val hasHeader = obj.optBoolean("hasHeader", true)
            val rowsArray = obj.getJSONArray("rows")
            val rows = mutableListOf<List<String>>()
            for (i in 0 until rowsArray.length()) {
                val rowArray = rowsArray.getJSONArray(i)
                val row = mutableListOf<String>()
                for (j in 0 until rowArray.length()) {
                    row.add(rowArray.getString(j))
                }
                rows.add(row)
            }
            return TableData(id = id, rows = rows, hasHeader = hasHeader)
        } catch (_: Exception) {
            return null
        }
    }

    fun strokesToJson(strokes: List<HandwritingStroke>): String {
        val array = JSONArray()
        strokes.forEach { stroke ->
            val obj = JSONObject()
            obj.put("id", stroke.id)
            obj.put("tool", stroke.tool.name)
            obj.put("colorHex", stroke.colorHex)
            obj.put("strokeWidth", stroke.strokeWidth.toDouble())
            obj.put("opacity", stroke.opacity.toDouble())
            obj.put("shapeType", stroke.shapeType.name)

            val ptsArray = JSONArray()
            stroke.points.forEach { pt ->
                val ptObj = JSONObject()
                ptObj.put("x", pt.x.toDouble())
                ptObj.put("y", pt.y.toDouble())
                ptObj.put("p", pt.pressure.toDouble())
                ptObj.put("t", pt.timestamp)
                ptsArray.put(ptObj)
            }
            obj.put("points", ptsArray)
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToStrokes(json: String?): List<HandwritingStroke> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<HandwritingStroke>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val tool = try {
                    ToolType.valueOf(obj.optString("tool", "PEN"))
                } catch (_: Exception) {
                    ToolType.PEN
                }
                val shapeType = try {
                    ShapeType.valueOf(obj.optString("shapeType", "NONE"))
                } catch (_: Exception) {
                    ShapeType.NONE
                }
                val ptsArray = obj.optJSONArray("points") ?: JSONArray()
                val points = mutableListOf<StrokePoint>()
                for (p in 0 until ptsArray.length()) {
                    val ptObj = ptsArray.getJSONObject(p)
                    points.add(
                        StrokePoint(
                            x = ptObj.optDouble("x", 0.0).toFloat(),
                            y = ptObj.optDouble("y", 0.0).toFloat(),
                            pressure = ptObj.optDouble("p", 1.0).toFloat(),
                            timestamp = ptObj.optLong("t", 0L)
                        )
                    )
                }

                list.add(
                    HandwritingStroke(
                        id = obj.optString("id"),
                        tool = tool,
                        colorHex = obj.optString("colorHex", "#1E1B4B"),
                        strokeWidth = obj.optDouble("strokeWidth", 4.0).toFloat(),
                        opacity = obj.optDouble("opacity", 1.0).toFloat(),
                        points = points,
                        shapeType = shapeType
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun paperToJson(paper: PaperBackground): String {
        val obj = JSONObject()
        obj.put("type", paper.type.name)
        obj.put("backgroundColorHex", paper.backgroundColorHex)
        obj.put("lineColorHex", paper.lineColorHex)
        obj.put("spacingDp", paper.spacingDp.toDouble())
        obj.put("lineThicknessDp", paper.lineThicknessDp.toDouble())
        obj.put("dotRadiusDp", paper.dotRadiusDp.toDouble())
        obj.put("showMarginLine", paper.showMarginLine)
        obj.put("marginOffsetDp", paper.marginOffsetDp.toDouble())
        return obj.toString()
    }

    fun jsonToPaper(json: String?): PaperBackground {
        if (json.isNullOrBlank()) return PaperBackground.ClassicCream
        try {
            val obj = JSONObject(json)
            val type = try {
                PaperType.valueOf(obj.optString("type", "RULED"))
            } catch (_: Exception) {
                PaperType.RULED
            }
            return PaperBackground(
                type = type,
                backgroundColorHex = obj.optString("backgroundColorHex", "#FFFDF9"),
                lineColorHex = obj.optString("lineColorHex", "#E2E8F0"),
                spacingDp = obj.optDouble("spacingDp", 28.0).toFloat(),
                lineThicknessDp = obj.optDouble("lineThicknessDp", 1.0).toFloat(),
                dotRadiusDp = obj.optDouble("dotRadiusDp", 1.5).toFloat(),
                showMarginLine = obj.optBoolean("showMarginLine", true),
                marginOffsetDp = obj.optDouble("marginOffsetDp", 56.0).toFloat()
            )
        } catch (_: Exception) {
            return PaperBackground.ClassicCream
        }
    }
}
