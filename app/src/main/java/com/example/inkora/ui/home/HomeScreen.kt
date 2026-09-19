package com.example.inkora.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkora.data.local.entity.NoteEntity
import com.example.inkora.model.SyncStatus
import com.example.inkora.ui.auth.AccountDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToEditor: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAccountDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var lockedNoteTarget by remember { mutableStateOf<NoteEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Inkora",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "• Write. Create. Remember.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Cloud Sync Chip
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showAccountDialog = true },
                        color = if (uiState.syncEngineState.status == SyncStatus.SYNCED) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (uiState.syncEngineState.isSyncing) Icons.Default.Sync else Icons.Default.CloudDone,
                                contentDescription = "Sync",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (uiState.syncEngineState.isSyncing) "Syncing" else uiState.syncEngineState.status.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Grid / List Toggle
                    IconButton(onClick = { viewModel.toggleLayoutMode() }) {
                        Icon(
                            if (uiState.isGridLayout) Icons.Default.ViewAgenda else Icons.Default.GridView,
                            contentDescription = "Toggle Grid/List"
                        )
                    }

                    // Settings
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedFilter != HomeFilter.TRASH) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.createNote("Untitled Note") { newId ->
                            onNavigateToEditor(newId)
                        }
                    },
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Create Note") },
                    text = { Text("New Note", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search notes, text, or tags...", style = MaterialTheme.typography.bodyMedium) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // 2. Folder Navigation Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedFolderId == null && uiState.selectedFilter == HomeFilter.ALL,
                        onClick = { viewModel.selectFolder(null) },
                        label = { Text("All Notes") },
                        leadingIcon = { Icon(Icons.Default.Notes, null, modifier = Modifier.size(16.dp)) }
                    )
                }

                items(uiState.folders) { folder ->
                    FilterChip(
                        selected = uiState.selectedFolderId == folder.id,
                        onClick = { viewModel.selectFolder(folder.id) },
                        label = { Text(folder.name) },
                        leadingIcon = { Icon(Icons.Outlined.Folder, null, modifier = Modifier.size(16.dp)) }
                    )
                }

                item {
                    AssistChip(
                        onClick = { showNewFolderDialog = true },
                        label = { Text("+ Folder") },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            // 3. Status Filters (All, Pinned, Favorites, Trash)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(HomeFilter.values()) { filter ->
                    val isSelected = uiState.selectedFilter == filter && uiState.selectedFolderId == null
                    SuggestionChip(
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                filter.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            // Trash empty banner
            if (uiState.selectedFilter == HomeFilter.TRASH && uiState.notes.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notes in trash", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { viewModel.emptyTrash() }) {
                            Text("Empty Trash Now", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. Notes Grid or List
            if (uiState.notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            if (uiState.selectedFilter == HomeFilter.TRASH) Icons.Default.DeleteOutline else Icons.Default.NoteAdd,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = when {
                                uiState.searchQuery.isNotBlank() -> "No notes match \"${uiState.searchQuery}\""
                                uiState.selectedFilter == HomeFilter.TRASH -> "Trash is empty"
                                uiState.selectedFilter == HomeFilter.FAVORITES -> "No favorite notes yet"
                                uiState.selectedFilter == HomeFilter.PINNED -> "No pinned notes"
                                else -> "Write. Create. Remember.\nTap + New Note to begin"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                val gridCells = if (uiState.isGridLayout) GridCells.Fixed(2) else GridCells.Fixed(1)
                LazyVerticalGrid(
                    columns = gridCells,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            isGrid = uiState.isGridLayout,
                            onClick = {
                                if (note.isLocked) {
                                    lockedNoteTarget = note
                                } else {
                                    onNavigateToEditor(note.id)
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(note) },
                            onTogglePinned = { viewModel.togglePinned(note) },
                            onToggleLock = { viewModel.toggleLock(note) },
                            onDelete = {
                                if (uiState.selectedFilter == HomeFilter.TRASH) {
                                    viewModel.deletePermanently(note.id)
                                } else {
                                    viewModel.moveToTrash(note.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Biometric / Device Lock verification dialog
    lockedNoteTarget?.let { note ->
        AlertDialog(
            onDismissRequest = { lockedNoteTarget = null },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Unlock Note") },
            text = { Text("Authenticate to open '${note.title}'.") },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = note.id
                        lockedNoteTarget = null
                        onNavigateToEditor(targetId)
                    }
                ) {
                    Text("Unlock & Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { lockedNoteTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    placeholder = { Text("e.g. Lectures") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName.trim())
                            newFolderName = ""
                            showNewFolderDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Account & Cloud Sync Dialog
    if (showAccountDialog) {
        AccountDialog(
            authManager = viewModel.authManager,
            syncEngine = com.example.inkora.data.sync.FirebaseSyncEngine(
                context = androidx.compose.ui.platform.LocalContext.current,
                database = com.example.inkora.data.local.InkoraDatabase.getInstance(androidx.compose.ui.platform.LocalContext.current),
                authManager = viewModel.authManager
            ),
            onDismiss = { showAccountDialog = false }
        )
    }
}
