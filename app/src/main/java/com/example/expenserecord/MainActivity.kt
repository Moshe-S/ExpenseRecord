package com.example.expenserecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Material3 pickers (inside AlertDialog for wide compatibility)
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState

data class UiTxn(
    val occurredAt: LocalDateTime,
    val category: String,
    val amount: Double,
    val title: String?,
    val manuallySetDateTime: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BudgetScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen() {
    // form state
    var category by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }

    // date/time pick state
    var pickedDate by remember { mutableStateOf(LocalDate.now()) }
    var pickedTime by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var dateTimeChangedManually by remember { mutableStateOf(false) }

    // dialogs
    var futureWarn: Pair<LocalDateTime, (() -> Unit)?>? by remember { mutableStateOf(null) }

    // list state
    var txns by remember { mutableStateOf(listOf<UiTxn>()) }

    val focus = LocalFocusManager.current
    val tsFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }

    // filtering
    val filtered = remember(txns, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) txns
        else txns.filter {
            it.category.lowercase().contains(q) ||
                    (it.title ?: "").lowercase().contains(q)
        }
    }
    val total = filtered.sumOf { it.amount }

    fun addIfValid() {
        val normalized = amount.replace(',', '.')
        val a = normalized.toDoubleOrNull()
        if (category.isBlank() || a == null || a <= 0.0) return

        val occurred = LocalDateTime.of(pickedDate, pickedTime)
        val commit: () -> Unit = {
            txns = listOf(
                UiTxn(
                    occurredAt = occurred,
                    category = category.trim(),
                    amount = a,
                    title = title.takeIf { it.isNotBlank() },
                    manuallySetDateTime = dateTimeChangedManually
                )
            ) + txns
            // reset only fields that make sense; keep date/time as last used
            amount = ""
            title = ""
            focus.clearFocus()
        }
        if (occurred.isAfter(LocalDateTime.now())) {
            futureWarn = occurred to commit
        } else commit()
    }

    fun Modifier.handleTabNext(): Modifier =
        this.onPreviewKeyEvent { e ->
            if (e.type == KeyEventType.KeyUp && e.key == Key.Tab) {
                focus.moveFocus(FocusDirection.Next)
                true
            } else false
        }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Expense Record") }) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Category + Title
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.weight(1f).handleTabNext(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) })
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Details (optional)") },
                    modifier = Modifier.weight(1f).handleTabNext(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) })
                )
            }

            // Row 2: Amount + Add
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        // allow digits and a single dot; comma becomes dot
                        val s = input.replace(',', '.')
                        val cleaned = s.filter { it.isDigit() || it == '.' }
                        val firstDot = cleaned.indexOf('.')
                        amount = if (firstDot == -1) cleaned else {
                            val head = cleaned.substring(0, firstDot + 1)
                            val tail = cleaned.substring(firstDot + 1).replace(".", "")
                            head + tail
                        }
                    },
                    label = { Text("Amount") },
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { e ->
                            if (e.type == KeyEventType.KeyUp &&
                                (e.key == Key.Enter || e.key == Key.NumPadEnter)
                            ) { addIfValid(); true } else false
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addIfValid() })
                )
                Button(
                    onClick = { addIfValid() },
                    modifier = Modifier.height(56.dp)
                ) { Text("Add") }
            }

            // Row 3: Date/Time + Change + indicator
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val preview = LocalDateTime.of(pickedDate, pickedTime).format(tsFmt)
                OutlinedTextField(
                    value = preview,
                    onValueChange = {},
                    label = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Date/Time")
                            if (dateTimeChangedManually) Text("🛈", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    readOnly = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.height(56.dp)
                ) { Text("Change date/time") }
            }

            HorizontalDivider()

            // Search
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search (category or details)") },
                modifier = Modifier.fillMaxWidth().handleTabNext(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() })
            )

            // Total
            Text("Total: ${"%.2f".format(total)}", style = MaterialTheme.typography.titleMedium)

            // List
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered) { t ->
                    val line = buildString {
                        append(t.occurredAt.format(tsFmt))
                        if (t.manuallySetDateTime) append(" 🛈")
                        append(" • ")
                        append(t.category)
                        append(" • ")
                        append("%.2f".format(t.amount))
                        t.title?.let { append(" • ").append(it) }
                    }
                    Text(line)
                }
            }
        }
    }

    // DatePicker (AlertDialog host)
    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
            text = {
                val state = rememberDatePickerState(
                    initialSelectedDateMillis = java.time.ZonedDateTime.now().toInstant().toEpochMilli()
                )
                DatePicker(state = state)
                LaunchedEffect(state.selectedDateMillis) {
                    state.selectedDateMillis?.let { millis ->
                        val ld = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        pickedDate = ld
                        dateTimeChangedManually = true
                    }
                }
            }
        )
    }

    // TimePicker (AlertDialog host)
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                val init = pickedTime
                val state = rememberTimePickerState(init.hour, init.minute, is24Hour = true)
                TimePicker(state = state)
                LaunchedEffect(state.hour, state.minute) {
                    pickedTime = LocalTime.of(state.hour, state.minute)
                    dateTimeChangedManually = true
                }
            }
        )
    }

    // Future warning dialog
    futureWarn?.let { (whenPicked, onConfirm) ->
        AlertDialog(
            onDismissRequest = { futureWarn = null },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm?.invoke()
                    futureWarn = null
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { futureWarn = null }) { Text("Cancel") }
            },
            title = { Text("Future date") },
            text = { Text("Selected time (${whenPicked.format(tsFmt)}) is in the future. Are you sure?") }
        )
    }
}
