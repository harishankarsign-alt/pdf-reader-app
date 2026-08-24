package com.example.pdfreader

import android.app.Application
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

data class ReaderUiState(
    val fileName: String? = null,
    val lines: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val speechRate: Float = 1.0f
)

class PdfReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                ttsReady = true
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onError(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                // Move to the next line automatically once speech for this line finishes.
                val state = _uiState.value
                if (!state.isPlaying) return

                val nextIndex = state.currentIndex + 1
                if (nextIndex < state.lines.size) {
                    _uiState.update { it.copy(currentIndex = nextIndex) }
                    speakLine(nextIndex)
                } else {
                    _uiState.update { it.copy(isPlaying = false) }
                }
            }
        })
    }

    fun loadPdf(uri: Uri) {
        stop()
        _uiState.value = ReaderUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val name = withContext(Dispatchers.IO) {
                    PdfTextExtractor.resolveFileName(context, uri)
                }
                val lines = withContext(Dispatchers.IO) {
                    PdfTextExtractor.extractLines(context, uri)
                }

                if (lines.isEmpty()) {
                    _uiState.value = ReaderUiState(
                        error = "No readable text was found in this PDF. It may be a scanned image."
                    )
                } else {
                    _uiState.value = ReaderUiState(fileName = name, lines = lines)
                }
            } catch (e: Exception) {
                _uiState.value = ReaderUiState(error = "Couldn't read that PDF: ${e.message}")
            }
        }
    }

    fun togglePlayPause() {
        val state = _uiState.value
        if (state.lines.isEmpty()) return

        if (state.isPlaying) {
            // Pause
            tts?.stop()
            _uiState.update { it.copy(isPlaying = false) }
        } else {
            // Play / resume from currentIndex
            _uiState.update { it.copy(isPlaying = true) }
            speakLine(state.currentIndex)
        }
    }

    fun stop() {
        tts?.stop()
        _uiState.update { it.copy(isPlaying = false, currentIndex = 0) }
    }

    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate)
        _uiState.update { it.copy(speechRate = rate) }
    }

    /** Lets the user tap a line directly to jump the reader there. */
    fun jumpToLine(index: Int) {
        val wasPlaying = _uiState.value.isPlaying
        tts?.stop()
        _uiState.update { it.copy(currentIndex = index) }
        if (wasPlaying) speakLine(index)
    }

    private fun speakLine(index: Int) {
        val state = _uiState.value
        if (!ttsReady || index !in state.lines.indices) return
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(state.lines[index], TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }
}
