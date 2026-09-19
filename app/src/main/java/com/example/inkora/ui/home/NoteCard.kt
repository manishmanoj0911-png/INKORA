package com.example.inkora.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkora.data.local.entity.NoteEntity
import com.example.inkora.model.FontCatalog
import com.example.inkora.ui.paper.isPaperDark
import com.example.inkora.ui.paper.parseColorSafe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoteCard(
    note: NoteEntity,
    isGrid: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePinned: () -> Unit,
    onToggleLock: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontItem = FontCatalog.getFontById(note.fontId)
    val paperColor = parseColorSafe(note.paperColorHex, Color(0xFFFFFDF9))
    val isDarkPaper = isPaperDark(com.example.inkora.model.PaperBackground(backgroundColorHex = note.paperColorHex))
    val contentColor = if (isDarkPaper) Color.White else Color(0xFF1E293B)

    val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.modifiedAt))

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (note.isPinned) 1.8.dp else 0.8.dp,
                color = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            ),
        color = paperColor,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Top Indicator row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge: Paper type + pages
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = (if (isDarkPaper) Color.White else Color.Black).copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "${note.paperType.lowercase().capitalize(Locale.ROOT)} • ${note.pageCount}p",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.75f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (note.isLocked) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }

                        IconButton(
                            onClick = onTogglePinned,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin",
                                tint = if (note.isPinned) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (note.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (note.isFavorite) Color(0xFFEF4444) else contentColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Title
                Text(
                    text = note.title.ifBlank { "Untitled Note" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = fontItem.fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = contentColor,
                    maxLines = if (isGrid) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                // Preview Snippet
                if (note.isLocked) {
                    Text(
                        text = "🔒 Protected Note — Tap to unlock",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.6f),
                        maxLines = 2
                    )
                } else {
                    Text(
                        text = note.previewSnippet.ifBlank { "No additional text" },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = fontItem.fontFamily
                        ),
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = if (isGrid) 4 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Bottom row: Date + Sync state indicator + Delete action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.5f)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Trash",
                        tint = contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
