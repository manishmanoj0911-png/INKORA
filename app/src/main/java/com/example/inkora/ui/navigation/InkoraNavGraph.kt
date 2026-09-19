package com.example.inkora.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.inkora.data.audio.AudioNoteManager
import com.example.inkora.data.auth.AuthManager
import com.example.inkora.data.local.InkoraDatabase
import com.example.inkora.data.repository.FolderRepository
import com.example.inkora.data.repository.NoteRepository
import com.example.inkora.data.sync.FirebaseSyncEngine
import com.example.inkora.ui.editor.NoteEditorScreen
import com.example.inkora.ui.editor.NoteEditorViewModel
import com.example.inkora.ui.home.HomeScreen
import com.example.inkora.ui.home.HomeViewModel
import com.example.inkora.ui.settings.SettingsScreen

object Destinations {
    const val HOME = "home"
    const val EDITOR = "editor/{noteId}"
    const val SETTINGS = "settings"

    fun editorRoute(noteId: String) = "editor/$noteId"
}

@Composable
fun InkoraNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val database = remember { InkoraDatabase.getInstance(context) }
    val noteRepository = remember { NoteRepository(database) }
    val folderRepository = remember { FolderRepository(database) }
    val authManager = remember { AuthManager(context) }
    val syncEngine = remember { FirebaseSyncEngine(context, database, authManager) }
    val audioManager = remember { AudioNoteManager(context) }

    NavHost(
        navController = navController,
        startDestination = Destinations.HOME
    ) {
        composable(Destinations.HOME) {
            val homeViewModel: HomeViewModel = viewModel {
                HomeViewModel(noteRepository, folderRepository, syncEngine, authManager)
            }
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToEditor = { noteId ->
                    navController.navigate(Destinations.editorRoute(noteId))
                },
                onNavigateToSettings = {
                    navController.navigate(Destinations.SETTINGS)
                }
            )
        }

        composable(
            route = Destinations.EDITOR,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            val editorViewModel: NoteEditorViewModel = viewModel {
                NoteEditorViewModel(noteRepository, folderRepository, audioManager).apply {
                    loadNote(noteId)
                }
            }

            NoteEditorScreen(
                viewModel = editorViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                database = database,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
