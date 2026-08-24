package com.example.pdfreader

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: PdfReaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PdfReaderScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun PdfReaderScreen(viewModel: PdfReaderViewModel) {
    val state by viewModel.uiState.collectAsState()

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadPdf(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        TopAppBar(
            title = { Text(state.fileName ?: "PDF Reader") },
            actions = {
                IconButton(onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) }) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Upload PDF")
                }
            }
        )

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                }
            }

            state.lines.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Tap the upload icon to choose a PDF. Its text will appear here, " +
                            "line by line, and can be read aloud.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            else -> {
                LineListView(
                    lines = state.lines,
                    currentIndex = state.currentIndex,
                    onLineTap = { viewModel.jumpToLine(it) },
                    modifier = Modifier.weight(1f)
                )

                PlaybackControls(
                    isPlaying = state.isPlaying,
                    currentIndex = state.currentIndex,
                    totalLines = state.lines.size,
                    speechRate = state.speechRate,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onStop = { viewModel.stop() },
                    onRateChange = { viewModel.setSpeechRate(it) }
                )
            }
        }
    }
}

@Composable
private fun LineListView(
    lines: List<String>,
    currentIndex: Int,
    onLineTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll so the current line stays in view as reading progresses.
    LaunchedEffect(currentIndex) {
        scope.launch {
            listState.animateScrollToItem(index = maxOf(0, currentIndex - 2))
        }
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxWidth()) {
        itemsIndexed(lines) { index, line ->
            val isCurrent = index == currentIndex
            Text(
                text = line,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLineTap(index) }
                    .background(
                        if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    currentIndex: Int,
    totalLines: Int,
    speechRate: Float,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onRateChange: (Float) -> Unit
) {
    Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Line ${currentIndex + 1} of $totalLines")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text("Speed", modifier = Modifier.width(56.dp))
                Slider(
                    value = speechRate,
                    onValueChange = onRateChange,
                    valueRange = 0.5f..2.0f,
                    modifier = Modifier.weight(1f)
                )
                Text(String.format("%.1fx", speechRate), modifier = Modifier.width(40.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(onClick = onPlayPause, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPlaying) "Pause" else "Play")
                }
                OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stop")
                }
            }
        }
    }
}
