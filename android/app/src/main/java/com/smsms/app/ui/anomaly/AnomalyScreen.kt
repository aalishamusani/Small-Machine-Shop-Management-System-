package com.smsms.app.ui.anomaly

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smsms.app.model.AnomalyFlag
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnomalyScreen(
    onBack: () -> Unit,
    viewModel: AnomalyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Anomaly Detection") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is AnomalyUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is AnomalyUiState.Error ->
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadFlags() }) { Text("Retry") }
                    }

                is AnomalyUiState.Loaded -> AnomalyContent(state, onRun = { viewModel.runDetection() })
            }
        }
    }
}

@Composable
private fun AnomalyContent(state: AnomalyUiState.Loaded, onRun: () -> Unit) {
    val outliers = state.outliers

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (state.flags.isEmpty()) {
                Text(
                    "No results yet. Tap \"Run anomaly detection\" below. " +
                        "It needs at least 5 quality logs to work."
                )
            } else {
                Text(
                    "${state.flags.size} quality logs analysed · ${outliers.size} flagged",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "A log is flagged when its defect rate is above ${formatPercent(state.flags.first().threshold)} " +
                        "(the average defect rate plus 2 standard deviations).",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (state.flags.isNotEmpty() && outliers.isEmpty()) {
            Text(
                "No unusual defect rates found — all quality checks look normal.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(outliers) { flag -> FlagRow(flag) }
        }

        state.runError?.let { message ->
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Button(
            onClick = onRun,
            enabled = !state.running,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (state.running) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("Run anomaly detection")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlagRow(flag: AnomalyFlag) {
    ListItem(
        headlineContent = { Text("Batch #${flag.batch_id} — ${formatPercent(flag.value)} defect rate") },
        supportingContent = {
            Text("${flag.defect_count} defective of ${flag.qty_checked} checked · ${flag.check_date}")
        }
    )
    HorizontalDivider()
}

// "0.4000" -> "40.0%". The backend stores rates as fractions (0.0-1.0).
private fun formatPercent(fraction: String): String =
    String.format(Locale.US, "%.1f%%", (fraction.toDoubleOrNull() ?: 0.0) * 100)
