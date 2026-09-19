package com.example.inkora.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.inkora.data.backup.BackupFileInfo
import com.example.inkora.data.backup.BackupManager
import com.example.inkora.data.local.InkoraDatabase
import com.example.inkora.model.FontCatalog
import com.example.inkora.model.PaperType
import com.example.inkora.ui.fonts.FontPickerDialog
import com.example.inkora.ui.paper.PaperStyleDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    database: InkoraDatabase,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context, database) }

    var backupsList by remember { mutableStateOf<List<BackupFileInfo>>(emptyList()) }
    var isBackingUp by remember { mutableStateOf(false) }
    var selectedBackupForRestore by remember { mutableStateOf<BackupFileInfo?>(null) }
    var showFontDialog by remember { mutableStateOf(false) }
    var defaultFontId by remember { mutableStateOf("outfit") }
    var defaultFontSizeSp by remember { mutableFloatStateOf(16f) }
    var palmRejectionEnabled by remember { mutableStateOf(true) }
    var strokeSmoothingEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        backupsList = backupManager.getBackupList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Backups", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // 1. App Identity Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Inkora Notes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Tagline: Write. Create. Remember.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Version 1.0.0 • Offline-first handwriting & rich notes with Firebase cloud sync",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Default Typography & Paper Section
            item {
                Text(
                    "Editor Defaults",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Font selection
                        val activeFont = FontCatalog.getFontById(defaultFontId)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Default Font", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(activeFont.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(onClick = { showFontDialog = true }) {
                                Text("Change")
                            }
                        }

                        Divider()

                        // Palm rejection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Stylus Palm Rejection", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Ignore non-stylus touches in drawing mode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = palmRejectionEnabled, onCheckedChange = { palmRejectionEnabled = it })
                        }

                        Divider()

                        // Stroke smoothing
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("High-Precision Stroke Smoothing", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Spline bezier curves for realistic ink", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = strokeSmoothingEnabled, onCheckedChange = { strokeSmoothingEnabled = it })
                        }
                    }
                }
            }

            // 3. Local Backup & Restore Section
            item {
                Text(
                    "Local Backups & Data Portability",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Export complete offline snapshots including all notes, handwriting strokes, folders, and voice attachments.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    isBackingUp = true
                                    try {
                                        val backupFile = backupManager.createBackup()
                                        backupsList = backupManager.getBackupList()
                                        Toast.makeText(context, "Backup created: ${backupFile.name}", Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isBackingUp = false
                                    }
                                }
                            },
                            enabled = !isBackingUp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (isBackingUp) "Creating Backup..." else "Create Backup Now")
                        }
                    }
                }
            }

            // Backup files list
            if (backupsList.isNotEmpty()) {
                item {
                    Text(
                        "Existing Backups (${backupsList.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(backupsList) { backupInfo ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(backupInfo.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${backupInfo.formattedDate} • ${backupInfo.noteCount} notes • ${backupInfo.sizeBytes / 1024} KB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row {
                                IconButton(onClick = { selectedBackupForRestore = backupInfo }) {
                                    Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            backupManager.deleteBackup(backupInfo.file)
                                            backupsList = backupManager.getBackupList()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Restore confirmation dialog
    selectedBackupForRestore?.let { backupInfo ->
        AlertDialog(
            onDismissRequest = { selectedBackupForRestore = null },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Restore Notes?") },
            text = {
                Text("Restoring from '${backupInfo.name}' will import ${backupInfo.noteCount} notes and their folders back into Inkora.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = backupInfo.file
                        selectedBackupForRestore = null
                        scope.launch {
                            val result = backupManager.restoreBackup(file)
                            if (result.isSuccess) {
                                Toast.makeText(context, "Restored ${result.getOrNull()} notes successfully", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Restore failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Confirm Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBackupForRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFontDialog) {
        FontPickerDialog(
            currentFontId = defaultFontId,
            currentFontSizeSp = defaultFontSizeSp,
            onDismiss = { showFontDialog = false },
            onSelectFont = { id, size ->
                defaultFontId = id
                defaultFontSizeSp = size
            }
        )
    }
}
