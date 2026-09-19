package com.example.inkora.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#4F46E5",
    val iconName: String = "folder",
    val parentFolderId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
