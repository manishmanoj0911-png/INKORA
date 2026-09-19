package com.example.inkora.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkora.model.TableData

@Composable
fun TableEditor(
    table: TableData,
    fontFamily: FontFamily,
    fontSizeSp: Float,
    onTableChanged: (TableData) -> Unit,
    onDeleteTable: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Table top action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Table (${table.rows.size} rows × ${table.rows.firstOrNull()?.size ?: 0} cols)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onTableChanged(table.addRow()) }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("+Row", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { onTableChanged(table.addColumn()) }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("+Col", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = onDeleteTable, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove Table",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Scrollable Grid of cells
            val scrollState = rememberScrollState()
            Column(modifier = Modifier.horizontalScroll(scrollState)) {
                table.rows.forEachIndexed { rowIndex, row ->
                    val isHeader = table.hasHeader && rowIndex == 0
                    Row(
                        modifier = Modifier
                            .background(
                                if (isHeader) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else if (rowIndex % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                    ) {
                        row.forEachIndexed { colIndex, cellValue ->
                            Box(
                                modifier = Modifier
                                    .width(130.dp)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .padding(4.dp)
                            ) {
                                TextField(
                                    value = cellValue,
                                    onValueChange = { newVal ->
                                        onTableChanged(table.updateCell(rowIndex, colIndex, newVal))
                                    },
                                    textStyle = TextStyle(
                                        fontFamily = fontFamily,
                                        fontSize = (fontSizeSp - 2).sp,
                                        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    placeholder = {
                                        Text(
                                            if (isHeader) "Header" else "Data",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
