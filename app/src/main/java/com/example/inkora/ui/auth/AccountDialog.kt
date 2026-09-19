package com.example.inkora.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.inkora.data.auth.AuthManager
import com.example.inkora.data.sync.FirebaseSyncEngine
import com.example.inkora.data.sync.SyncEngineState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountDialog(
    authManager: AuthManager,
    syncEngine: FirebaseSyncEngine,
    onDismiss: () -> Unit
) {
    val currentUser by authManager.currentUser.collectAsState()
    val syncState by syncEngine.syncState.collectAsState()
    val scope = rememberCoroutineScope()

    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "Inkora Author") }
    var email by remember { mutableStateOf(currentUser?.email ?: "author@inkora.app") }
    var isEditingProfile by remember { mutableStateOf(false) }

    val formattedSyncTime = remember(syncState.lastSyncTime) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(syncState.lastSyncTime))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Account & Cloud Sync", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // User Profile Header
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (currentUser?.displayName?.take(1) ?: "I").uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = currentUser?.displayName ?: "Inkora Author",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUser?.email ?: "author@inkora.app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (currentUser != null && !currentUser!!.isAnonymous) "Cloud Account Connected" else "Offline-First Mode",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Sync Status Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cloud Status", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (syncState.isSyncing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = if (syncState.isSyncing) "Syncing..." else syncState.status.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(syncState.message, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(2.dp))
                        Text("Last Sync: $formattedSyncTime", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (isEditingProfile) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Account Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Sync Now Action
                Button(
                    onClick = {
                        scope.launch {
                            if (isEditingProfile) {
                                authManager.signInLocalOrGuest(displayName, email)
                                isEditingProfile = false
                            }
                            syncEngine.triggerSync(force = true)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (syncState.isSyncing) "Syncing..." else "Sync Now")
                }

                OutlinedButton(
                    onClick = { isEditingProfile = !isEditingProfile },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isEditingProfile) "Cancel Edit" else "Edit Account Details")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
