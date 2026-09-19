package com.example.inkora.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#0D9488"
)

@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagId"]
)
data class NoteTagCrossRef(
    val noteId: String,
    val tagId: String
)
