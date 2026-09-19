package com.example.inkora.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inkora.data.auth.AuthManager
import com.example.inkora.data.local.entity.FolderEntity
import com.example.inkora.data.local.entity.NoteEntity
import com.example.inkora.data.local.entity.TagEntity
import com.example.inkora.data.repository.FolderRepository
import com.example.inkora.data.repository.NoteRepository
import com.example.inkora.data.sync.FirebaseSyncEngine
import com.example.inkora.data.sync.SyncEngineState
import com.example.inkora.model.PaperBackground
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class HomeFilter(val label: String) {
    ALL("All Notes"),
    PINNED("Pinned"),
    FAVORITES("Favorites"),
    TRASH("Trash")
}

data class HomeUiState(
    val notes: List<NoteEntity> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val selectedFolderId: String? = null,
    val selectedFilter: HomeFilter = HomeFilter.ALL,
    val searchQuery: String = "",
    val isGridLayout: Boolean = true,
    val syncEngineState: SyncEngineState = SyncEngineState(),
    val isTrashEmptying: Boolean = false,
    val infoMessage: String? = null
)

class HomeViewModel(
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository,
    private val syncEngine: FirebaseSyncEngine,
    val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadFoldersAndTags()
        observeNotes()
        observeSync()
    }

    private fun loadFoldersAndTags() {
        viewModelScope.launch {
            folderRepository.getAllFolders().collectLatest { folders ->
                _uiState.value = _uiState.value.copy(folders = folders)
            }
        }
        viewModelScope.launch {
            folderRepository.getAllTags().collectLatest { tags ->
                _uiState.value = _uiState.value.copy(tags = tags)
            }
        }
    }

    private fun observeSync() {
        viewModelScope.launch {
            syncEngine.syncState.collectLatest { syncState ->
                _uiState.value = _uiState.value.copy(syncEngineState = syncState)
            }
        }
    }

    private fun observeNotes() {
        viewModelScope.launch {
            combine(
                _uiState.map { it.selectedFilter }.distinctUntilChanged(),
                _uiState.map { it.selectedFolderId }.distinctUntilChanged(),
                _uiState.map { it.searchQuery }.distinctUntilChanged()
            ) { filter, folderId, query ->
                Triple(filter, folderId, query)
            }.flatMapLatest { (filter, folderId, query) ->
                when {
                    query.isNotBlank() -> noteRepository.searchNotes(query)
                    filter == HomeFilter.TRASH -> noteRepository.getTrashNotes()
                    filter == HomeFilter.PINNED -> noteRepository.getPinnedNotes()
                    filter == HomeFilter.FAVORITES -> noteRepository.getFavoriteNotes()
                    folderId != null -> noteRepository.getNotesByFolder(folderId)
                    else -> noteRepository.getActiveNotes()
                }
            }.collectLatest { notesList ->
                _uiState.value = _uiState.value.copy(notes = notesList)
            }
        }
    }

    fun setFilter(filter: HomeFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter, selectedFolderId = null)
    }

    fun selectFolder(folderId: String?) {
        _uiState.value = _uiState.value.copy(selectedFolderId = folderId, selectedFilter = HomeFilter.ALL)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleLayoutMode() {
        _uiState.value = _uiState.value.copy(isGridLayout = !_uiState.value.isGridLayout)
    }

    fun triggerSync() {
        viewModelScope.launch {
            syncEngine.triggerSync(force = true)
        }
    }

    fun createNote(title: String = "Untitled Note", onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val noteId = noteRepository.createNote(
                title = title,
                folderId = _uiState.value.selectedFolderId,
                paperBackground = PaperBackground.ClassicCream
            )
            onCreated(noteId)
        }
    }

    fun toggleFavorite(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.setFavorite(note.id, !note.isFavorite)
        }
    }

    fun togglePinned(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.setPinned(note.id, !note.isPinned)
        }
    }

    fun toggleLock(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.setLocked(note.id, !note.isLocked)
        }
    }

    fun moveToTrash(noteId: String) {
        viewModelScope.launch {
            noteRepository.setTrash(noteId, true)
        }
    }

    fun restoreFromTrash(noteId: String) {
        viewModelScope.launch {
            noteRepository.setTrash(noteId, false)
        }
    }

    fun deletePermanently(noteId: String) {
        viewModelScope.launch {
            noteRepository.deletePermanently(noteId)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            noteRepository.emptyTrash()
        }
    }

    fun createFolder(name: String, colorHex: String = "#4F46E5") {
        viewModelScope.launch {
            folderRepository.createFolder(name, colorHex)
        }
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }
}
