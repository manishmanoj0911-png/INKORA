package com.example.inkora.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AudioRecordingState {
    IDLE,
    RECORDING,
    PAUSED
}

enum class AudioPlaybackState {
    STOPPED,
    PLAYING,
    PAUSED
}

class AudioNoteManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: File? = null

    private val _recordingState = MutableStateFlow(AudioRecordingState.IDLE)
    val recordingState: StateFlow<AudioRecordingState> = _recordingState.asStateFlow()

    private val _playbackState = MutableStateFlow(AudioPlaybackState.STOPPED)
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath.asStateFlow()

    private val audioDir: File
        get() {
            val dir = File(context.filesDir, "audio_notes")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun startRecording(noteId: String): File? {
        try {
            stopPlayback()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(audioDir, "audio_${noteId}_$timestamp.m4a")
            currentRecordingFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            _recordingState.value = AudioRecordingState.RECORDING
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            _recordingState.value = AudioRecordingState.IDLE
            return null
        }
    }

    fun stopRecording(): File? {
        val file = currentRecordingFile
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            _recordingState.value = AudioRecordingState.IDLE
        }
        return file
    }

    fun startPlayback(path: String, onCompletion: () -> Unit = {}) {
        try {
            stopPlayback()
            val file = File(path)
            if (!file.exists()) return

            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _playbackState.value = AudioPlaybackState.STOPPED
                    _currentPlayingPath.value = null
                    onCompletion()
                }
                start()
            }
            mediaPlayer = player
            _currentPlayingPath.value = path
            _playbackState.value = AudioPlaybackState.PLAYING
        } catch (e: Exception) {
            e.printStackTrace()
            _playbackState.value = AudioPlaybackState.STOPPED
            _currentPlayingPath.value = null
        }
    }

    fun pausePlayback() {
        try {
            mediaPlayer?.pause()
            _playbackState.value = AudioPlaybackState.PAUSED
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resumePlayback() {
        try {
            mediaPlayer?.start()
            _playbackState.value = AudioPlaybackState.PLAYING
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            _playbackState.value = AudioPlaybackState.STOPPED
            _currentPlayingPath.value = null
        }
    }

    fun release() {
        stopRecording()
        stopPlayback()
    }
}
