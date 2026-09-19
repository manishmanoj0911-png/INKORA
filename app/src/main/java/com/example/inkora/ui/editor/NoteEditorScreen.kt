package com.example.inkora.ui.editor

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkora.data.audio.AudioPlaybackState
import com.example.inkora.model.*
import com.example.inkora.ui.fonts.FontPickerDialog
import com.example.inkora.ui.handwriting.HandwritingCanvas
import com.example.inkora.ui.paper.PaperBackgroundCanvas
import com.example.inkora.ui.paper.PaperStyleDialog
import com.example.inkora.ui.paper.isPaperDark
import com.example.inkora.ui.paper.parseColorSafe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val recordingState by viewModel.audioManager.recordingState.collectAsState()
    val playbackState by viewModel.audioManager.playbackState.collectAsState()
    val currentPlayingPath by viewModel.audioManager.currentPlayingPath.collectAsState()

    var showPaperDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showShapePicker by remember { mutableStateOf(false) }

    val activeFontItem = remember(uiState.fontId) {
        FontCatalog.getFontById(uiState.fontId)
    }

    val paper = uiState.paperBackground
    val darkPaper = isPaperDark(paper)
    val textColor = if (darkPaper) Color.White else Color(0xFF1E293B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = uiState.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        placeholder = { Text("Note Title", style = MaterialTheme.typography.titleMedium) },
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back & Save")
                    }
                },
                actions = {
                    // Handwriting vs Type toggle button
                    IconButton(
                        onClick = { viewModel.setHandwritingMode(!uiState.isHandwritingMode) }
                    ) {
                        Icon(
                            if (uiState.isHandwritingMode) Icons.Default.Keyboard else Icons.Default.Draw,
                            contentDescription = if (uiState.isHandwritingMode) "Switch to Keyboard" else "Switch to Stylus",
                            tint = if (uiState.isHandwritingMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Paper style picker
                    IconButton(onClick = { showPaperDialog = true }) {
                        Icon(Icons.Default.Palette, contentDescription = "Paper Background")
                    }

                    // Font dialog
                    IconButton(onClick = { showFontDialog = true }) {
                        Icon(Icons.Default.TextFields, contentDescription = "Font & Typography")
                    }

                    // More actions dropdown
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add Table") },
                                leadingIcon = { Icon(Icons.Default.TableChart, null) },
                                onClick = {
                                    viewModel.updateTable(TableData())
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add Checklist") },
                                leadingIcon = { Icon(Icons.Default.Checklist, null) },
                                onClick = {
                                    viewModel.updateChecklists(uiState.checklists + ChecklistItem(text = ""))
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Convert Handwriting to Text") },
                                leadingIcon = { Icon(Icons.Default.TextFields, null) },
                                onClick = {
                                    viewModel.triggerConvertAllStrokesToText()
                                    showMoreMenu = false
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Add New Page") },
                                leadingIcon = { Icon(Icons.Default.PostAdd, null) },
                                onClick = {
                                    viewModel.addNewPage()
                                    showMoreMenu = false
                                }
                            )
                            if (uiState.pages.size > 1) {
                                DropdownMenuItem(
                                    text = { Text("Delete Current Page") },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        viewModel.deleteCurrentPage()
                                        showMoreMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            // Multi-page switcher & status bar
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Page pager
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.goToPage(uiState.currentPageIndex - 1) },
                            enabled = uiState.currentPageIndex > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Page")
                        }

                        Text(
                            "Page ${uiState.currentPageIndex + 1} of ${uiState.pages.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = { viewModel.goToPage(uiState.currentPageIndex + 1) },
                            enabled = uiState.currentPageIndex < uiState.pages.size - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Page")
                        }

                        IconButton(
                            onClick = { viewModel.addNewPage() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Page")
                        }
                    }

                    // Font & paper indicator badge
                    Text(
                        "${activeFontItem.name} • ${paper.type.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Scalable, smooth Paper Canvas Background
            PaperBackgroundCanvas(paper = paper)

            // 2. Editor Layer: Either Handwriting Canvas or Rich Typing Area
            if (uiState.isHandwritingMode) {
                // Dedicated Handwriting Canvas
                HandwritingCanvas(
                    strokes = uiState.strokes,
                    currentTool = uiState.currentTool,
                    currentColorHex = uiState.strokeColorHex,
                    currentThickness = uiState.strokeThickness,
                    currentOpacity = uiState.strokeOpacity,
                    currentShape = uiState.currentShape,
                    isStylusOnly = uiState.isStylusOnly,
                    onStrokesChanged = { viewModel.updateStrokes(it) },
                    onLassoSelected = { viewModel.onLassoSelectedStrokes(it) }
                )

                // Floating Stylus Toolbar (Top of canvas)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Tool Icons Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Pen
                            IconButton(
                                onClick = {
                                    viewModel.setTool(ToolType.PEN)
                                    viewModel.setShape(ShapeType.NONE)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Pen",
                                    tint = if (uiState.currentTool == ToolType.PEN && uiState.currentShape == ShapeType.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Pencil
                            IconButton(
                                onClick = {
                                    viewModel.setTool(ToolType.PENCIL)
                                    viewModel.setShape(ShapeType.NONE)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Create,
                                    contentDescription = "Pencil",
                                    tint = if (uiState.currentTool == ToolType.PENCIL && uiState.currentShape == ShapeType.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Highlighter
                            IconButton(
                                onClick = {
                                    viewModel.setTool(ToolType.HIGHLIGHTER)
                                    viewModel.setShape(ShapeType.NONE)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Brush,
                                    contentDescription = "Highlighter",
                                    tint = if (uiState.currentTool == ToolType.HIGHLIGHTER && uiState.currentShape == ShapeType.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Eraser
                            IconButton(
                                onClick = {
                                    viewModel.setTool(ToolType.ERASER)
                                    viewModel.setShape(ShapeType.NONE)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoFixNormal,
                                    contentDescription = "Eraser",
                                    tint = if (uiState.currentTool == ToolType.ERASER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Lasso
                            IconButton(
                                onClick = {
                                    viewModel.setTool(ToolType.LASSO)
                                    viewModel.setShape(ShapeType.NONE)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.CropFree,
                                    contentDescription = "Lasso Selection",
                                    tint = if (uiState.currentTool == ToolType.LASSO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Shapes
                            Box {
                                IconButton(
                                    onClick = { showShapePicker = !showShapePicker },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Category,
                                        contentDescription = "Shapes",
                                        tint = if (uiState.currentShape != ShapeType.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                DropdownMenu(
                                    expanded = showShapePicker,
                                    onDismissRequest = { showShapePicker = false }
                                ) {
                                    DropdownMenuItem(text = { Text("Line") }, onClick = { viewModel.setShape(ShapeType.LINE); showShapePicker = false })
                                    DropdownMenuItem(text = { Text("Arrow") }, onClick = { viewModel.setShape(ShapeType.ARROW); showShapePicker = false })
                                    DropdownMenuItem(text = { Text("Rectangle") }, onClick = { viewModel.setShape(ShapeType.RECTANGLE); showShapePicker = false })
                                    DropdownMenuItem(text = { Text("Rounded Rectangle") }, onClick = { viewModel.setShape(ShapeType.ROUNDED_RECTANGLE); showShapePicker = false })
                                    DropdownMenuItem(text = { Text("Circle / Ellipse") }, onClick = { viewModel.setShape(ShapeType.CIRCLE); showShapePicker = false })
                                    DropdownMenuItem(text = { Text("Triangle") }, onClick = { viewModel.setShape(ShapeType.TRIANGLE); showShapePicker = false })
                                }
                            }

                            // Palm Rejection
                            IconButton(
                                onClick = { viewModel.toggleStylusOnly() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.PanTool,
                                    contentDescription = "Palm Rejection",
                                    tint = if (uiState.isStylusOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }

                            // Undo / Redo
                            IconButton(
                                onClick = { viewModel.undo() },
                                enabled = uiState.canUndo,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                            }
                            IconButton(
                                onClick = { viewModel.redo() },
                                enabled = uiState.canRedo,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                            }
                        }

                        // Color Palette Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            val inkColors = listOf(
                                "#1E1B4B" to "Navy Ink",
                                "#4F46E5" to "Indigo",
                                "#DC2626" to "Crimson",
                                "#D97706" to "Amber",
                                "#059669" to "Emerald",
                                "#7C3AED" to "Purple",
                                "#FFFFFF" to "White"
                            )
                            inkColors.forEach { (hex, _) ->
                                val color = parseColorSafe(hex)
                                val isSelected = uiState.strokeColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.5.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.setStrokeColor(hex) }
                                )
                            }

                            Spacer(Modifier.width(6.dp))

                            // Convert to text button
                            TextButton(
                                onClick = { viewModel.triggerConvertAllStrokesToText() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Convert", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            } else {
                // Rich Text & Blocks Mode
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Voice Memos player & recorder
                    VoiceMemoBar(
                        recordingState = recordingState,
                        playbackState = playbackState,
                        currentPlayingPath = currentPlayingPath,
                        audioAttachments = uiState.audioAttachments,
                        onStartRecording = { viewModel.startAudioRecording() },
                        onStopRecording = { viewModel.stopAudioRecording() },
                        onPlay = { path -> viewModel.audioManager.startPlayback(path) },
                        onPause = { viewModel.audioManager.pausePlayback() },
                        onDelete = { id -> viewModel.deleteAudioAttachment(id) }
                    )

                    // Optional Table Editor Block
                    uiState.table?.let { tableData ->
                        TableEditor(
                            table = tableData,
                            fontFamily = activeFontItem.fontFamily,
                            fontSizeSp = uiState.fontSizeSp,
                            onTableChanged = { viewModel.updateTable(it) },
                            onDeleteTable = { viewModel.updateTable(null) }
                        )
                    }

                    // Optional Checklist Editor Block
                    if (uiState.checklists.isNotEmpty()) {
                        ChecklistEditor(
                            items = uiState.checklists,
                            fontFamily = activeFontItem.fontFamily,
                            fontSizeSp = uiState.fontSizeSp,
                            onItemsChanged = { viewModel.updateChecklists(it) }
                        )
                    }

                    // Main Text Area
                    TextField(
                        value = uiState.bodyText,
                        onValueChange = { viewModel.updateBodyText(it) },
                        placeholder = {
                            Text(
                                "Start writing, typing, or tap Draw to use handwriting...",
                                fontFamily = activeFontItem.fontFamily,
                                fontSize = uiState.fontSizeSp.sp,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = activeFontItem.fontFamily,
                            fontSize = uiState.fontSizeSp.sp,
                            color = textColor,
                            lineHeight = (uiState.fontSizeSp * 1.55f).sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    // Display handwriting preview if strokes exist
                    if (uiState.strokes.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setHandwritingMode(true) }
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                HandwritingCanvas(
                                    strokes = uiState.strokes,
                                    currentTool = ToolType.PEN,
                                    currentColorHex = uiState.strokeColorHex,
                                    currentThickness = 2f,
                                    currentOpacity = 1f,
                                    currentShape = ShapeType.NONE,
                                    isStylusOnly = false,
                                    onStrokesChanged = {},
                                    onLassoSelected = {}
                                )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "${uiState.strokes.size} handwriting strokes • Tap to edit",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Handwriting-to-Text Recognition Dialog
    uiState.recognizedTextCandidate?.let { recognizedText ->
        var editableText by remember(recognizedText) { mutableStateOf(recognizedText) }
        var deleteStrokes by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissHandwritingConversion() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Handwriting to Text", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Review and edit the recognized text below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editableText,
                        onValueChange = { editableText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = activeFontItem.fontFamily)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { deleteStrokes = !deleteStrokes }
                    ) {
                        Checkbox(checked = deleteStrokes, onCheckedChange = { deleteStrokes = it })
                        Spacer(Modifier.width(6.dp))
                        Text("Remove handwriting strokes after inserting", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.applyHandwritingConversion(editableText, deleteStrokes) }) {
                    Text("Insert Converted Text")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissHandwritingConversion() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Paper Style Dialog
    if (showPaperDialog) {
        PaperStyleDialog(
            initialPaper = paper,
            onDismiss = { showPaperDialog = false },
            onSaveStyle = { newPaper -> viewModel.updatePaperStyle(newPaper) },
            onSaveAsTemplate = { newPaper, name -> viewModel.saveAsTemplate(newPaper, name) }
        )
    }

    // Font Picker Dialog
    if (showFontDialog) {
        FontPickerDialog(
            currentFontId = uiState.fontId,
            currentFontSizeSp = uiState.fontSizeSp,
            onDismiss = { showFontDialog = false },
            onSelectFont = { id, size -> viewModel.updateFont(id, size) }
        )
    }
}
