package com.example.inkora.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkora.model.ChecklistItem

@Composable
fun ChecklistEditor(
    items: List<ChecklistItem>,
    fontFamily: FontFamily,
    fontSizeSp: Float,
    onItemsChanged: (List<ChecklistItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { checked ->
                        val updated = items.toMutableList()
                        updated[index] = item.copy(isChecked = checked)
                        onItemsChanged(updated)
                    }
                )

                TextField(
                    value = item.text,
                    onValueChange = { newText ->
                        val updated = items.toMutableList()
                        updated[index] = item.copy(text = newText)
                        onItemsChanged(updated)
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontFamily = fontFamily,
                        fontSize = fontSizeSp.sp,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    placeholder = {
                        Text(
                            "To-do item...",
                            fontFamily = fontFamily,
                            fontSize = fontSizeSp.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )

                IconButton(
                    onClick = {
                        val updated = items.filterIndexed { i, _ -> i != index }
                        onItemsChanged(updated)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete Item",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        TextButton(
            onClick = {
                onItemsChanged(items + ChecklistItem(text = "", isChecked = false))
            },
            modifier = Modifier.padding(start = 6.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add Checklist Item", style = MaterialTheme.typography.labelMedium)
        }
    }
}
