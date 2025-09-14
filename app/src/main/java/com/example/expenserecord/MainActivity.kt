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
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.TextFieldValue


data class UiTxn(
    val id: Long = 0L,
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
fun BudgetScreen(vm: TxnViewModel = viewModel()) {
    var category by remember { mutableStateOf(TextFieldValue("")) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }

    var pickedDate by remember { mutableStateOf(LocalDate.now()) }
    var pickedTime by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var dateTimeChangedManually by remember { mutableStateOf(false) }

    var futureWarn: Pair<LocalDateTime, (() -> Unit)?>? by remember { mutableStateOf(null) }

    val txns by vm.txns.collectAsState()
    val focus = LocalFocusManager.current
    val tsFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()


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
        if (category.text.isBlank() || a == null || a <= 0.0) return

        val occurred = LocalDateTime.of(pickedDate, pickedTime)
        val commit: () -> Unit = {
            vm.add(
                UiTxn(
                    occurredAt = occurred,
                    category = category.text.trim(),
                    amount = a,
                    title = title.takeIf { it.isNotBlank() },
                    manuallySetDateTime = dateTimeChangedManually
                )
            )
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
        topBar = { TopAppBar(title = { Text("Expense Record") }) },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }

    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier
                        .weight(1f)
                        .handleTabNext()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                category = category.copy(selection = TextRange(0, category.text.length))
                            }
                        },
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

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
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

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search (category or details)") },
                modifier = Modifier.fillMaxWidth().handleTabNext(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() })
            )

            Text("Total: ${"%.2f".format(total)}", style = MaterialTheme.typography.titleMedium)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered, key = { it.id }) { t ->
                    val line = buildString {
                        append(t.occurredAt.format(tsFmt))
                        if (t.manuallySetDateTime) append(" 🛈")
                        append(" • ")
                        append(t.category)
                        append(" • ")
                        append("%.2f".format(t.amount))
                        t.title?.let { append(" • ").append(it) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(line, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            val deleted = t
                            vm.rememberDeleted(
                                UiTxn(
                                    id = deleted.id,
                                    occurredAt = deleted.occurredAt,
                                    category = deleted.category,
                                    amount = deleted.amount,
                                    title = deleted.title,
                                    manuallySetDateTime = deleted.manuallySetDateTime
                                )
                            )
                            scope.launch {
                                vm.delete(deleted.id)
                                val result = snackbarHostState.showSnackbar(
                                    message = "Deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    vm.restoreLastDeleted()
                                }
                            }
                        }) {
                            Text("Delete")
                        }

                    }
                }
            }
        }
    }

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
