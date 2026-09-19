package com.example.inkora.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inkora.data.audio.AudioNoteManager
import com.example.inkora.data.local.JsonUtils
import com.example.inkora.data.local.entity.AttachmentEntity
import com.example.inkora.data.local.entity.NoteEntity
import com.example.inkora.data.local.entity.PageEntity
import com.example.inkora.data.repository.FolderRepository
import com.example.inkora.data.repository.NoteRepository
import com.example.inkora.model.*
import com.example.inkora.ui.handwriting.HandwritingRecognitionEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NoteEditorUiState(
    val isLoading: Boolean = true,
    val note: NoteEntity? = null,
    val pages: List<PageEntity> = emptyList(),
    val currentPageIndex: Int = 0,
    val title: String = "",
    val bodyText: String = "",
    val checklists: List<ChecklistItem> = emptyList(),
    val table: TableData? = null,
    val strokes: List<HandwritingStroke> = emptyList(),
    val paperBackground: PaperBackground = PaperBackground.ClassicCream,
    val fontId: String = "outfit",
    val fontSizeSp: Float = 16f,
    val isHandwritingMode: Boolean = false,
    val currentTool: ToolType = ToolType.PEN,
    val strokeColorHex: String = "#1E1B4B",
    val strokeThickness: Float = 4.0f,
    val strokeOpacity: Float = 1.0f,
    val currentShape: ShapeType = ShapeType.NONE,
    val isStylusOnly: Boolean = false,
    val audioAttachments: List<AttachmentEntity> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val recognizedTextCandidate: String? = null,
    val selectedStrokesForConversion: List<HandwritingStroke> = emptyList(),
    val saveMessage: String? = null
)

class NoteEditorViewModel(
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository,
    val audioManager: AudioNoteManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<List<HandwritingStroke>>()
    private val redoStack = mutableListOf<List<HandwritingStroke>>()

    fun loadNote(noteId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val note = noteRepository.getNoteDirect(noteId)
            val pages = noteRepository.getPagesDirect(noteId)

            if (note != null && pages.isNotEmpty()) {
                val firstPage = pages.first()
                val paper = PaperBackground(
                    type = try { PaperType.valueOf(note.paperType) } catch (_: Exception) { PaperType.RULED },
                    backgroundColorHex = note.paperColorHex,
                    lineColorHex = note.paperLineColorHex,
                    spacingDp = note.paperSpacing
                )
                val parsedStrokes = JsonUtils.jsonToStrokes(firstPage.strokesJson)

                _uiState.value = NoteEditorUiState(
                    isLoading = false,
                    note = note,
                    pages = pages,
                    currentPageIndex = 0,
                    title = note.title,
                    bodyText = firstPage.bodyText,
                    checklists = JsonUtils.jsonToChecklists(firstPage.checklistJson),
                    table = JsonUtils.jsonToTable(firstPage.tableJson),
                    strokes = parsedStrokes,
                    paperBackground = paper,
                    fontId = note.fontId,
                    fontSizeSp = note.fontSizeSp
                )
                undoStack.clear()
                redoStack.clear()

                // Observe audio attachments
                noteRepository.getAttachmentsForNote(noteId).collectLatest { attachments ->
                    _uiState.value = _uiState.value.copy(
                        audioAttachments = attachments.filter { it.type == AttachmentType.AUDIO.name }
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(title = newTitle)
        autoSave()
    }

    fun updateBodyText(newText: String) {
        _uiState.value = _uiState.value.copy(bodyText = newText)
        autoSave()
    }

    fun updateChecklists(items: List<ChecklistItem>) {
        _uiState.value = _uiState.value.copy(checklists = items)
        autoSave()
    }

    fun updateTable(tableData: TableData?) {
        _uiState.value = _uiState.value.copy(table = tableData)
        autoSave()
    }

    fun updateStrokes(newStrokes: List<HandwritingStroke>) {
        val current = _uiState.value.strokes
        undoStack.add(current)
        redoStack.clear()
        _uiState.value = _uiState.value.copy(
            strokes = newStrokes,
            canUndo = undoStack.isNotEmpty(),
            canRedo = false
        )
        autoSave()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(_uiState.value.strokes)
            _uiState.value = _uiState.value.copy(
                strokes = previous,
                canUndo = undoStack.isNotEmpty(),
                canRedo = true
            )
            autoSave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(_uiState.value.strokes)
            _uiState.value = _uiState.value.copy(
                strokes = next,
                canUndo = true,
                canRedo = redoStack.isNotEmpty()
            )
            autoSave()
        }
    }

    fun setHandwritingMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isHandwritingMode = enabled)
    }

    fun setTool(tool: ToolType) {
        val opacity = if (tool == ToolType.HIGHLIGHTER) 0.35f else 1.0f
        val width = when (tool) {
            ToolType.HIGHLIGHTER -> 22.0f
            ToolType.PENCIL -> 2.5f
            ToolType.PEN -> 4.0f
            ToolType.ERASER -> 24.0f
            else -> _uiState.value.strokeThickness
        }
        _uiState.value = _uiState.value.copy(
            currentTool = tool,
            strokeOpacity = opacity,
            strokeThickness = width
        )
    }

    fun setStrokeColor(hex: String) {
        _uiState.value = _uiState.value.copy(strokeColorHex = hex)
    }

    fun setStrokeThickness(thickness: Float) {
        _uiState.value = _uiState.value.copy(strokeThickness = thickness)
    }

    fun setShape(shape: ShapeType) {
        _uiState.value = _uiState.value.copy(currentShape = shape)
    }

    fun toggleStylusOnly() {
        _uiState.value = _uiState.value.copy(isStylusOnly = !_uiState.value.isStylusOnly)
    }

    fun updatePaperStyle(paper: PaperBackground) {
        _uiState.value = _uiState.value.copy(paperBackground = paper)
        viewModelScope.launch {
            _uiState.value.note?.id?.let { noteId ->
                noteRepository.updateNotePaperStyle(noteId, paper)
            }
        }
    }

    fun saveAsTemplate(paper: PaperBackground, name: String) {
        viewModelScope.launch {
            folderRepository.createTemplate(
                com.example.inkora.data.local.entity.PageTemplateEntity(
                    name = name,
                    paperType = paper.type.name,
                    paperColorHex = paper.backgroundColorHex,
                    paperLineColorHex = paper.lineColorHex,
                    paperSpacing = paper.spacingDp,
                    fontId = _uiState.value.fontId,
                    fontSizeSp = _uiState.value.fontSizeSp
                )
            )
        }
    }

    fun updateFont(fontId: String, fontSizeSp: Float) {
        _uiState.value = _uiState.value.copy(fontId = fontId, fontSizeSp = fontSizeSp)
        viewModelScope.launch {
            _uiState.value.note?.id?.let { noteId ->
                noteRepository.updateNoteFont(noteId, fontId, fontSizeSp)
            }
        }
    }

    fun onLassoSelectedStrokes(selected: List<HandwritingStroke>) {
        if (selected.isNotEmpty()) {
            val recognized = HandwritingRecognitionEngine.recognizeStrokes(selected)
            _uiState.value = _uiState.value.copy(
                recognizedTextCandidate = recognized,
                selectedStrokesForConversion = selected
            )
        }
    }

    fun triggerConvertAllStrokesToText() {
        val allStrokes = _uiState.value.strokes
        if (allStrokes.isNotEmpty()) {
            val recognized = HandwritingRecognitionEngine.recognizeStrokes(allStrokes)
            _uiState.value = _uiState.value.copy(
                recognizedTextCandidate = recognized,
                selectedStrokesForConversion = allStrokes
            )
        }
    }

    fun applyHandwritingConversion(text: String, deleteConvertedStrokes: Boolean) {
        val currentBody = _uiState.value.bodyText
        val separator = if (currentBody.isBlank()) "" else "\n\n"
        val updatedBody = currentBody + separator + text

        val newStrokes = if (deleteConvertedStrokes) {
            val removeIds = _uiState.value.selectedStrokesForConversion.map { it.id }.toSet()
            _uiState.value.strokes.filterNot { removeIds.contains(it.id) }
        } else {
            _uiState.value.strokes
        }

        _uiState.value = _uiState.value.copy(
            bodyText = updatedBody,
            strokes = newStrokes,
            recognizedTextCandidate = null,
            selectedStrokesForConversion = emptyList()
        )
        autoSave()
    }

    fun dismissHandwritingConversion() {
        _uiState.value = _uiState.value.copy(
            recognizedTextCandidate = null,
            selectedStrokesForConversion = emptyList()
        )
    }

    fun addNewPage() {
        val note = _uiState.value.note ?: return
        viewModelScope.launch {
            // Save current page state first
            persistCurrentPage()
            val newPage = noteRepository.addPage(note.id, _uiState.value.currentPageIndex)
            val allPages = noteRepository.getPagesDirect(note.id)
            _uiState.value = _uiState.value.copy(
                pages = allPages,
                currentPageIndex = newPage.pageIndex,
                bodyText = newPage.bodyText,
                checklists = emptyList(),
                table = null,
                strokes = emptyList()
            )
        }
    }

    fun goToPage(index: Int) {
        val pages = _uiState.value.pages
        if (index in pages.indices && index != _uiState.value.currentPageIndex) {
            viewModelScope.launch {
                persistCurrentPage()
                val target = pages[index]
                _uiState.value = _uiState.value.copy(
                    currentPageIndex = index,
                    bodyText = target.bodyText,
                    checklists = JsonUtils.jsonToChecklists(target.checklistJson),
                    table = JsonUtils.jsonToTable(target.tableJson),
                    strokes = JsonUtils.jsonToStrokes(target.strokesJson)
                )
            }
        }
    }

    fun deleteCurrentPage() {
        val note = _uiState.value.note ?: return
        val pages = _uiState.value.pages
        if (pages.size <= 1) return

        val currentPage = pages[_uiState.value.currentPageIndex]
        viewModelScope.launch {
            noteRepository.deletePage(note.id, currentPage.id)
            val remaining = noteRepository.getPagesDirect(note.id)
            val nextIndex = _uiState.value.currentPageIndex.coerceAtMost(remaining.size - 1)
            val nextTarget = remaining[nextIndex]
            _uiState.value = _uiState.value.copy(
                pages = remaining,
                currentPageIndex = nextIndex,
                bodyText = nextTarget.bodyText,
                checklists = JsonUtils.jsonToChecklists(nextTarget.checklistJson),
                table = JsonUtils.jsonToTable(nextTarget.tableJson),
                strokes = JsonUtils.jsonToStrokes(nextTarget.strokesJson)
            )
        }
    }

    fun startAudioRecording() {
        val noteId = _uiState.value.note?.id ?: return
        audioManager.startRecording(noteId)
    }

    fun stopAudioRecording() {
        val noteId = _uiState.value.note?.id ?: return
        val recordedFile = audioManager.stopRecording()
        if (recordedFile != null && recordedFile.exists()) {
            viewModelScope.launch {
                val attachment = AttachmentEntity(
                    noteId = noteId,
                    type = AttachmentType.AUDIO.name,
                    localPath = recordedFile.absolutePath,
                    name = "Voice Memo (${java.text.SimpleDateFormat("MMM dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())})",
                    sizeBytes = recordedFile.length()
                )
                noteRepository.addAttachment(attachment)
            }
        }
    }

    fun deleteAudioAttachment(attachmentId: String) {
        viewModelScope.launch {
            noteRepository.deleteAttachment(attachmentId)
        }
    }

    private fun persistCurrentPage() {
        val note = _uiState.value.note ?: return
        val pages = _uiState.value.pages
        val currentIndex = _uiState.value.currentPageIndex
        if (currentIndex !in pages.indices) return

        val currentPage = pages[currentIndex].copy(
            bodyText = _uiState.value.bodyText,
            checklistJson = JsonUtils.checklistsToJson(_uiState.value.checklists),
            tableJson = JsonUtils.tableToJson(_uiState.value.table),
            strokesJson = JsonUtils.strokesToJson(_uiState.value.strokes),
            modifiedAt = System.currentTimeMillis()
        )

        val updatedPages = pages.toMutableList()
        updatedPages[currentIndex] = currentPage

        viewModelScope.launch {
            noteRepository.saveNote(
                note = note.copy(
                    title = _uiState.value.title,
                    previewSnippet = _uiState.value.bodyText.take(120).replace("\n", " ")
                ),
                pages = updatedPages
            )
        }
    }

    private fun autoSave() {
        persistCurrentPage()
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
