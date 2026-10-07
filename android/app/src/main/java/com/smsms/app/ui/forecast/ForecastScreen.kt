package com.smsms.app.ui.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smsms.app.model.SalesForecast
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    onBack: () -> Unit,
    viewModel: ForecastViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Forecast") },
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
                is ForecastUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is ForecastUiState.Error ->
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadForecast() }) { Text("Retry") }
                    }

                is ForecastUiState.Loaded -> ForecastContent(
                    state,
                    onRunForecast = { viewModel.runForecast() },
                    onAddActualClick = { viewModel.openAddActual() },
                    onAddActualDismiss = { viewModel.dismissAddActual() },
                    onAddActualSave = { amount -> viewModel.addActual(amount) }
                )
            }
        }
    }
}

@Composable
private fun ForecastContent(
    state: ForecastUiState.Loaded,
    onRunForecast: () -> Unit,
    onAddActualClick: () -> Unit,
    onAddActualDismiss: () -> Unit,
    onAddActualSave: (Double) -> Unit
) {
    if (state.addActualOpen && state.nextMonth != null) {
        AddActualDialog(
            month = state.nextMonth,
            saving = state.savingActual,
            error = state.actualError,
            onDismiss = onAddActualDismiss,
            onSave = onAddActualSave
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.rows.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                Text("No forecast yet. Tap \"Run forecast\" below to train the model on the shop's sales history.")
            }
        } else {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Model: ${state.modelUsed ?: "—"}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (state.mae != null && state.rmse != null) {
                        "Evaluated on ${state.rows.count { it.actual_value != null }} known months — " +
                            "MAE ${formatCurrency(state.mae)}, RMSE ${formatCurrency(state.rmse)}"
                    } else {
                        "No evaluated months yet."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                ForecastChart(
                    rows = state.rows,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                )
                Spacer(Modifier.height(8.dp))
                Legend()
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.rows) { row -> ForecastRow(row) }
            }
        }

        state.runError?.let { message ->
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (state.nextMonth != null) {
            OutlinedButton(
                onClick = onAddActualClick,
                enabled = !state.running,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            ) {
                Text("Add ${monthYear(state.nextMonth)} sales")
            }
        }

        Button(
            onClick = onRunForecast,
            enabled = !state.running,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (state.running) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("Run forecast")
            }
        }
    }
}

// Asks for the real total sales of the next finished month. The month itself
// is NOT editable: the server only accepts the next month in sequence (no
// gaps), so the app just shows which month that is.
@Composable
private fun AddActualDialog(
    month: String,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    val amount = amountText.replace(",", "").trim().toDoubleOrNull()
    val isValid = amount != null && amount > 0

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Add ${monthYear(month)} sales") },
        text = {
            Column {
                Text("Enter the total value of everything dispatched in ${monthYear(month)}, in rupees.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Total sales (₹)") },
                    singleLine = true,
                    enabled = !saving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(amount!!) }, enabled = isValid && !saving) {
                if (saving) CircularProgressIndicator(modifier = Modifier.height(18.dp)) else Text("Save & re-run forecast")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") }
        }
    )
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row {
            LegendSwatch(MaterialTheme.colorScheme.primary, dashed = false)
            Spacer(Modifier.width(4.dp))
            Text("Actual", style = MaterialTheme.typography.labelSmall)
        }
        Row {
            LegendSwatch(MaterialTheme.colorScheme.tertiary, dashed = true)
            Spacer(Modifier.width(4.dp))
            Text("Predicted", style = MaterialTheme.typography.labelSmall)
        }
    }
}

// The swatch needs a FIXED width. Previously its Canvas used fillMaxWidth(),
// so it stretched across the whole row and pushed the "Actual"/"Predicted"
// text off screen. The predicted swatch is dashed to match the dashed line
// it represents on the chart.
@Composable
private fun LegendSwatch(color: Color, dashed: Boolean) {
    Canvas(modifier = Modifier.width(20.dp).height(10.dp)) {
        drawLine(
            color,
            Offset(0f, size.height / 2),
            Offset(size.width, size.height / 2),
            strokeWidth = 3.dp.toPx(),
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8f, 5f)) else null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForecastRow(row: SalesForecast) {
    val isFuture = row.actual_value == null
    ListItem(
        headlineContent = { Text(row.forecast_date) },
        supportingContent = {
            Text(
                if (isFuture) {
                    "Predicted ${formatCurrency(row.predicted_value.toDouble())} (upcoming)"
                } else {
                    "Predicted ${formatCurrency(row.predicted_value.toDouble())} · Actual ${formatCurrency(row.actual_value!!.toDouble())}"
                }
            )
        }
    )
    HorizontalDivider()
}

// A hand-rolled line chart on Compose's Canvas rather than a charting
// library — one fewer third-party dependency to version-match against
// the rest of the Gradle setup, and simple enough (two polylines plus a
// handful of dots) that drawing it directly is genuinely less code than
// wiring up a library would be.
@Composable
private fun ForecastChart(rows: List<SalesForecast>, modifier: Modifier = Modifier) {
    if (rows.isEmpty()) return

    val actualColor = MaterialTheme.colorScheme.primary
    val predictedColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    val predictedValues = rows.map { it.predicted_value.toDouble() }
    val actualPoints = rows.mapIndexedNotNull { i, r -> r.actual_value?.let { i to it.toDouble() } }
    val allValues = predictedValues + actualPoints.map { it.second }
    val minValue = allValues.min()
    val maxValue = allValues.max()
    val valueRange = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

    Canvas(modifier = modifier) {
        val n = rows.size

        // Margins reserved around the plot area: left for the value labels,
        // bottom for the month labels, top for the "Forecast" tag. They also
        // keep the edge dots from being cut off by the canvas boundary.
        val leftMargin = 52.dp.toPx()
        val rightMargin = 8.dp.toPx()
        val topMargin = 14.dp.toPx()
        val bottomMargin = 22.dp.toPx()
        val plotWidth = size.width - leftMargin - rightMargin
        val plotHeight = size.height - topMargin - bottomMargin

        // Each point sits in the centre of its own equal-width column, so a
        // month label drawn at the same x lines up exactly under its dot.
        fun xFor(index: Int): Float = leftMargin + plotWidth * (index + 0.5f) / n

        fun yFor(value: Double): Float =
            topMargin + plotHeight - ((value - minValue) / valueRange * plotHeight).toFloat()

        val labelPaint = android.graphics.Paint().apply {
            color = labelColor
            textSize = 10.sp.toPx()
            isAntiAlias = true
        }

        // Three horizontal gridlines (min / mid / max), each labelled with
        // its value in lakhs so the chart can be read without the list.
        listOf(0f, 0.5f, 1f).forEach { frac ->
            val y = topMargin + plotHeight * (1 - frac)
            drawLine(gridColor, Offset(leftMargin, y), Offset(size.width - rightMargin, y), strokeWidth = 1.dp.toPx())
            labelPaint.textAlign = android.graphics.Paint.Align.RIGHT
            drawContext.canvas.nativeCanvas.drawText(
                formatLakh(minValue + valueRange * frac),
                leftMargin - 6.dp.toPx(),
                y + labelPaint.textSize / 3,
                labelPaint
            )
        }

        // Month label under every point.
        labelPaint.textAlign = android.graphics.Paint.Align.CENTER
        rows.forEachIndexed { i, r ->
            drawContext.canvas.nativeCanvas.drawText(
                monthLabel(r.forecast_date),
                xFor(i),
                size.height - 4.dp.toPx(),
                labelPaint
            )
        }

        // Divider between months with known actuals and future forecast months.
        val firstFutureIndex = rows.indexOfFirst { it.actual_value == null }
        if (firstFutureIndex > 0) {
            val dividerX = leftMargin + plotWidth * firstFutureIndex / n
            drawLine(
                gridColor,
                Offset(dividerX, topMargin),
                Offset(dividerX, topMargin + plotHeight),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
            labelPaint.textAlign = android.graphics.Paint.Align.LEFT
            drawContext.canvas.nativeCanvas.drawText(
                "Forecast →",
                dividerX + 4.dp.toPx(),
                topMargin - 4.dp.toPx(),
                labelPaint
            )
        }

        val predictedPath = Path().apply {
            rows.forEachIndexed { i, r ->
                val x = xFor(i)
                val y = yFor(r.predicted_value.toDouble())
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            predictedPath,
            color = predictedColor,
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
            )
        )

        if (actualPoints.size >= 2) {
            val actualPath = Path().apply {
                actualPoints.forEachIndexed { idx, (i, v) ->
                    val x = xFor(i)
                    val y = yFor(v)
                    if (idx == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(actualPath, color = actualColor, style = Stroke(width = 3.dp.toPx()))
        }

        actualPoints.forEach { (i, v) ->
            drawCircle(actualColor, radius = 4.dp.toPx(), center = Offset(xFor(i), yFor(v)))
        }
        rows.forEachIndexed { i, r ->
            if (r.actual_value == null) {
                drawCircle(predictedColor, radius = 4.dp.toPx(), center = Offset(xFor(i), yFor(r.predicted_value.toDouble())))
            }
        }
    }
}

private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

// "2026-08-01" -> "Aug 2026"
private fun monthYear(date: String): String {
    val month = date.substring(5, 7).toIntOrNull()?.let { MONTH_NAMES.getOrNull(it - 1) }
    return if (month != null) "$month ${date.substring(0, 4)}" else date
}

// "2026-04-01" -> "Apr 26". Falls back to the raw string if it isn't a date.
private fun monthLabel(date: String): String {
    val month = date.substring(5, 7).toIntOrNull()?.let { MONTH_NAMES.getOrNull(it - 1) }
    return if (month != null) "$month ${date.substring(2, 4)}" else date
}

// 3,270,612 -> "₹32.7L" (1 lakh = 100,000) — short enough for an axis label.
private fun formatLakh(value: Double): String =
    "₹" + String.format(Locale.US, "%.1fL", value / 100_000)

private fun formatCurrency(value: Double): String =
    "₹" + String.format(Locale.US, "%,.0f", value)
