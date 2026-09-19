package com.example.inkora.model

import java.util.UUID

enum class SyncStatus(val label: String) {
    SYNCED("Synced"),
    PENDING_UPLOAD("Pending upload"),
    PENDING_DELETE("Pending delete"),
    CONFLICT("Conflict"),
    ERROR("Sync error")
}

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val isChecked: Boolean = false
)

data class TableData(
    val id: String = UUID.randomUUID().toString(),
    val rows: List<List<String>> = listOf(
        listOf("Header 1", "Header 2", "Header 3"),
        listOf("Row 1, Cell 1", "Row 1, Cell 2", "Row 1, Cell 3"),
        listOf("Row 2, Cell 1", "Row 2, Cell 2", "Row 2, Cell 3")
    ),
    val hasHeader: Boolean = true
) {
    fun addRow(): TableData {
        val colCount = rows.firstOrNull()?.size ?: 3
        val newRow = List(colCount) { "" }
        return copy(rows = rows + listOf(newRow))
    }

    fun removeRow(index: Int): TableData {
        if (rows.size <= 1) return this
        return copy(rows = rows.filterIndexed { i, _ -> i != index })
    }

    fun addColumn(): TableData {
        return copy(rows = rows.map { it + "" })
    }

    fun removeColumn(index: Int): TableData {
        if ((rows.firstOrNull()?.size ?: 0) <= 1) return this
        return copy(rows = rows.map { row -> row.filterIndexed { i, _ -> i != index } })
    }

    fun updateCell(rowIndex: Int, colIndex: Int, value: String): TableData {
        return copy(
            rows = rows.mapIndexed { r, row ->
                if (r == rowIndex) {
                    row.mapIndexed { c, cell -> if (c == colIndex) value else cell }
                } else row
            }
        )
    }
}

enum class AttachmentType {
    IMAGE,
    AUDIO,
    PDF,
    DRAWING
}

data class NoteAttachment(
    val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val type: AttachmentType,
    val localPath: String,
    val remoteUrl: String? = null,
    val name: String,
    val sizeBytes: Long = 0L,
    val durationSeconds: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
