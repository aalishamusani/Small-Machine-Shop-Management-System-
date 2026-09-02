package com.smsms.app.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smsms.app.model.Batch
import com.smsms.app.model.Order

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: Int,
    onBack: () -> Unit,
    viewModel: OrderDetailViewModel = viewModel(factory = OrderDetailViewModelFactory(orderId))
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddBatchDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order #$orderId") },
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
                is OrderDetailUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is OrderDetailUiState.Error ->
                    Text(
                        state.message,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )

                is OrderDetailUiState.Loaded -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        OrderSummary(state.order)
                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Batches", style = MaterialTheme.typography.titleMedium)
                            TextButton(
                                onClick = { showAddBatchDialog = true },
                                enabled = state.order.status == "active" && !state.actionInProgress
                            ) { Text("+ Add batch") }
                        }

                        if (state.order.batches.isEmpty()) {
                            Text("No batches yet.", modifier = Modifier.padding(vertical = 8.dp))
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(state.order.batches) { batch ->
                                    BatchRow(
                                        batch = batch,
                                        enabled = !state.actionInProgress,
                                        onAdvance = { next -> viewModel.advanceBatch(batch.batch_id, next) }
                                    )
                                }
                            }
                        }

                        state.actionError?.let { message ->
                            Spacer(Modifier.height(8.dp))
                            Text(message, color = MaterialTheme.colorScheme.error)
                        }

                        Spacer(Modifier.height(16.dp))
                        // Closing is deliberately allowed to fail server-side
                        // (e.g. "2 batch(es) not completed yet") rather than
                        // us trying to perfectly replicate that rule here —
                        // the server is the one source of truth for it.
                        Button(
                            onClick = { viewModel.closeOrder() },
                            enabled = state.order.status == "active" && !state.actionInProgress,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Close order") }
                    }

                    if (showAddBatchDialog) {
                        AddBatchDialog(
                            onDismiss = { showAddBatchDialog = false },
                            onConfirm = { qty ->
                                showAddBatchDialog = false
                                viewModel.addBatch(qty)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderSummary(order: Order) {
    Column {
        Text(order.customer_name, style = MaterialTheme.typography.headlineSmall)
        Text(order.job_type ?: "—", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text("Ordered: ${order.order_date}   Due: ${order.due_date ?: "—"}")
        Text("Quantity: ${order.total_qty}   Amount: ${order.total_amount ?: "—"}")
        Spacer(Modifier.height(4.dp))
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                order.status,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchRow(batch: Batch, enabled: Boolean, onAdvance: (String) -> Unit) {
    // Forward-only progression, mirroring ALLOWED_TRANSITIONS in the
    // backend's batches.controller.js — "completed" has no next step.
    val nextStatus = when (batch.status) {
        "pending" -> "in_progress"
        "in_progress" -> "completed"
        else -> null
    }
    ListItem(
        headlineContent = { Text("Batch #${batch.batch_id} — qty ${batch.quantity}") },
        supportingContent = { Text("Status: ${batch.status}") },
        trailingContent = {
            if (nextStatus != null) {
                TextButton(onClick = { onAdvance(nextStatus) }, enabled = enabled) {
                    Text(if (nextStatus == "in_progress") "Start" else "Complete")
                }
            }
        }
    )
    HorizontalDivider()
}

@Composable
private fun AddBatchDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var qtyText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add batch") },
        text = {
            OutlinedTextField(
                value = qtyText,
                onValueChange = { qtyText = it },
                label = { Text("Quantity") }
            )
        },
        confirmButton = {
            TextButton(onClick = { qtyText.toIntOrNull()?.let(onConfirm) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
