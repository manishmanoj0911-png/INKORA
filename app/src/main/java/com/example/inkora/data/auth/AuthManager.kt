package com.example.inkora.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "inkora_auth_prefs")

data class InkoraUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val isAnonymous: Boolean = false
)

class AuthManager(private val context: Context) {

    private val KEY_UID = stringPreferencesKey("user_uid")
    private val KEY_EMAIL = stringPreferencesKey("user_email")
    private val KEY_NAME = stringPreferencesKey("user_name")
    private val KEY_PHOTO = stringPreferencesKey("user_photo")

    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        null
    }

    private val _currentUser = MutableStateFlow<InkoraUser?>(null)
    val currentUser: StateFlow<InkoraUser?> = _currentUser.asStateFlow()

    init {
        // Restore cached local user if offline or available
        val fbUser = auth?.currentUser
        if (fbUser != null) {
            _currentUser.value = mapFirebaseUser(fbUser)
        }
    }

    suspend fun loadCachedUser() {
        try {
            val prefs = context.dataStore.data.first()
            val uid = prefs[KEY_UID]
            if (!uid.isNullOrBlank()) {
                _currentUser.value = InkoraUser(
                    uid = uid,
                    email = prefs[KEY_EMAIL] ?: "offline@inkora.app",
                    displayName = prefs[KEY_NAME] ?: "Inkora Author",
                    photoUrl = prefs[KEY_PHOTO]
                )
            }
        } catch (_: Exception) {}
    }

    private fun mapFirebaseUser(user: FirebaseUser): InkoraUser {
        return InkoraUser(
            uid = user.uid,
            email = user.email ?: "author@inkora.app",
            displayName = user.displayName ?: "Inkora Author",
            photoUrl = user.photoUrl?.toString(),
            isAnonymous = user.isAnonymous
        )
    }

    suspend fun signInLocalOrGuest(displayName: String, email: String) {
        val user = InkoraUser(
            uid = "local_${System.currentTimeMillis()}",
            email = email.ifBlank { "author@inkora.app" },
            displayName = displayName.ifBlank { "Inkora Author" },
            isAnonymous = true
        )
        saveUserToDataStore(user)
        _currentUser.value = user
    }

    suspend fun onFirebaseUserAuthenticated(user: FirebaseUser) {
        val inkoraUser = mapFirebaseUser(user)
        saveUserToDataStore(inkoraUser)
        _currentUser.value = inkoraUser
    }

    private suspend fun saveUserToDataStore(user: InkoraUser) {
        try {
            context.dataStore.edit { prefs ->
                prefs[KEY_UID] = user.uid
                prefs[KEY_EMAIL] = user.email
                prefs[KEY_NAME] = user.displayName
                user.photoUrl?.let { prefs[KEY_PHOTO] = it }
            }
        } catch (_: Exception) {}
    }

    suspend fun signOut() {
        try {
            auth?.signOut()
            context.dataStore.edit { prefs ->
                prefs.clear()
            }
        } catch (_: Exception) {}
        _currentUser.value = null
    }

    fun isUserSignedIn(): Boolean = _currentUser.value != null
}
