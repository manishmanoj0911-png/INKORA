package com.example.inkora.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.inkora.data.audio.AudioPlaybackState
import com.example.inkora.data.audio.AudioRecordingState
import com.example.inkora.data.local.entity.AttachmentEntity

@Composable
fun VoiceMemoBar(
    recordingState: AudioRecordingState,
    playbackState: AudioPlaybackState,
    currentPlayingPath: String?,
    audioAttachments: List<AttachmentEntity>,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPlay: (String) -> Unit,
    onPause: () -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header with recording button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (recordingState == AudioRecordingState.RECORDING) Color.Red else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (recordingState == AudioRecordingState.RECORDING) "Recording audio memo..." else "Voice Memos",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                if (recordingState == AudioRecordingState.RECORDING) {
                    Button(
                        onClick = onStopRecording,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Stop")
                    }
                } else {
                    OutlinedButton(onClick = onStartRecording) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = "Record", tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Record Audio")
                    }
                }
            }

            // Existing audio attachments list
            if (audioAttachments.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                audioAttachments.forEach { audio ->
                    val isPlayingThis = currentPlayingPath == audio.localPath && playbackState == AudioPlaybackState.PLAYING
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(
                                onClick = {
                                    if (isPlayingThis) onPause() else onPlay(audio.localPath)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlayingThis) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(audio.name, style = MaterialTheme.typography.bodySmall)
                                Text("${audio.sizeBytes / 1024} KB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        IconButton(onClick = { onDelete(audio.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
