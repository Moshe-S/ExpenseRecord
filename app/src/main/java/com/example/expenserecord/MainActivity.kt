package com.example.expenserecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

data class UiTxn(
    val id: Long = 0L,
    val occurredAt: LocalDateTime,
    val category: String,
    val amount: Double,
    val title: String?,
    val manuallySetDateTime: Boolean
)

/** Short vertical separator with fixed height */
@Composable
private fun VSep(heightDp: Int = 20) {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp) // breathing room from text
            .width(1.dp)
            .height(heightDp.dp)
            .background(Color.Black.copy(alpha = 0.25f))
    )
}

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
    var focusedField by remember { mutableStateOf<String?>(null) }

    var pickedDate by remember { mutableStateOf(LocalDate.now()) }
    var pickedTime by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var dateTimeChangedManually by remember { mutableStateOf(false) }

    var futureWarn: Pair<LocalDateTime, (() -> Unit)?>? by remember { mutableStateOf(null) }

    val txns by vm.txns.collectAsState()
    val focus = LocalFocusManager.current

    val tsFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") } // for preview field
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd MMM") }         // "24 Sep"
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }          // "23:59"

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val editing by vm.editing.collectAsState()

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
            vm.addCategoryToHistory(category.text.trim())
            amount = ""
            title = ""
            focus.clearFocus()
            pickedDate = LocalDate.now()
            pickedTime = LocalTime.now().withSecond(0).withNano(0)
            scope.launch { snackbarHostState.showSnackbar("Added", duration = SnackbarDuration.Short) }
            dateTimeChangedManually = false
        }
        if (occurred.isAfter(LocalDateTime.now())) {
            futureWarn = occurred to commit
        } else commit()
    }

    LaunchedEffect(editing?.id) {
        editing?.let { tx ->
            category = TextFieldValue(tx.category)
            title = tx.title ?: ""
            amount = tx.amount.toString()
            val dt = tx.occurredAt
            pickedDate = dt.toLocalDate()
            pickedTime = dt.toLocalTime().withSecond(0).withNano(0)
            dateTimeChangedManually = tx.manuallySetDateTime
        }
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
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                androidx.compose.material3.Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = data.visuals.message,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        if (data.visuals.actionLabel != null) {
                            TextButton(
                                onClick = { data.performAction() },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Text(data.visuals.actionLabel!!)
                            }
                        }
                    }
                }

            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Inputs row 1: Category + Details
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .handleTabNext()
                                .onFocusChanged { focusState ->
                                    focusedField = if (focusState.isFocused) "category" else null
                                    if (focusState.isFocused) {
                                        category = category.copy(
                                            selection = TextRange(0, category.text.length)
                                        )
                                    }
                                },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) })
                        )

                        val recentCategories by vm.recentCategories.collectAsState()
                        val suggestions = remember(category.text, recentCategories) {
                            if (category.text.isBlank()) {
                                recentCategories.take(5)
                            } else {
                                val q = category.text.trim().lowercase()
                                val recentMatches = recentCategories.filter { it.lowercase().contains(q) }
                                val otherMatches = recentCategories
                                    .filter { it.lowercase().contains(q) && it !in recentMatches }
                                    .sortedBy { it.lowercase() }
                                (recentMatches + otherMatches).take(5)
                            }
                        }

                        if (suggestions.isNotEmpty() && focusedField == "category") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 60.dp)
                            ) {
                                LazyColumn(modifier = Modifier.height(120.dp)) {
                                    items(suggestions) { categoryName ->
                                        Text(
                                            text = categoryName,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    category = TextFieldValue(categoryName)
                                                    focusedField = null
                                                    focus.moveFocus(FocusDirection.Next)
                                                }
                                                .padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Details (optional)") },
                        modifier = Modifier
                            .weight(1f)
                            .handleTabNext(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) })
                    )
                }
            }

            // Inputs row 2: Amount + Add/Save/Cancel
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
                            if (
                                e.type == KeyEventType.KeyUp &&
                                (e.key == Key.Enter || e.key == Key.NumPadEnter)
                            ) { addIfValid(); true } else false
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Number),
                    keyboardActions = KeyboardActions(onDone = { addIfValid() })
                )


            }

            // Date/time preview + change button
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val preview = LocalDateTime.of(pickedDate, pickedTime).format(tsFmt)

                Box(
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    OutlinedTextField(
                        value = preview,
                        onValueChange = {},
                        label = { Text("Date/Time${if (dateTimeChangedManually) " 🛈" else ""}") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit date and time",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    )

                    // click-through overlay to ensure whole field opens the picker
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { showDatePicker = true }
                    )
                }
            }

            if (editing == null) {
                Button(
                    onClick = { addIfValid() },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .height(56.dp)

                ) { Text("Add") }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {

                Button(
                        onClick = {
                            val normalized = amount.replace(',', '.')
                            val a = normalized.toDoubleOrNull() ?: return@Button
                            val occurred = LocalDateTime.of(pickedDate, pickedTime)
                            val currentId = vm.editing.value?.id ?: return@Button

                            val updated = UiTxn(
                                id = currentId,
                                occurredAt = occurred,
                                category = category.text.trim(),
                                amount = a,
                                title = title.takeIf { it.isNotBlank() },
                                manuallySetDateTime = dateTimeChangedManually
                            )

                            vm.saveEdit(updated)
                            scope.launch { snackbarHostState.showSnackbar("Saved", duration = SnackbarDuration.Short) }
                            category = TextFieldValue("")
                            title = ""
                            amount = ""
                            dateTimeChangedManually = false
                            focus.clearFocus()
                            pickedDate = LocalDate.now()
                            pickedTime = LocalTime.now().withSecond(0).withNano(0)
                        },
                        modifier = Modifier.height(56.dp)
                    ) { Text("Save") }

                    TextButton(
                        onClick = {
                            vm.cancelEdit()
                            scope.launch { snackbarHostState.showSnackbar("Edit canceled", duration = SnackbarDuration.Short) }
                            category = TextFieldValue("")
                            title = ""
                            amount = ""
                            dateTimeChangedManually = false
                            focus.clearFocus()
                        },
                        modifier = Modifier.height(56.dp)
                    ) { Text("Cancel") }
                }
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
            Text(
                "Total: ${"%.2f".format(total)}",
                style = MaterialTheme.typography.titleMedium
            )

            // Month controls
            val currentMonth by vm.currentMonth.collectAsState()
            val canGoNext = vm.canGoToNextMonth()

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { vm.goToPreviousMonth() }) { Text("← Previous") }
                Text(
                    currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium
                )
                Button(onClick = { vm.goToNextMonth() }, enabled = canGoNext) { Text("Next →") }
            }

            // ===== Table header =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Date",
                    modifier = Modifier
                        .width(54.dp)
                        .padding(horizontal = 6.dp),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                VSep(20)

                Text(
                    "Time",
                    modifier = Modifier
                        .width(46.dp)
                        .padding(horizontal = 6.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                VSep(20)

                Text(
                    "Category",
                    modifier = Modifier
                        .weight(38f)
                        .padding(horizontal = 6.dp),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                VSep(20)

                Text(
                    "Spent",
                    modifier = Modifier
                        .weight(24f)
                        .padding(horizontal = 6.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                VSep(20)

                Text(
                    "Details",
                    modifier = Modifier
                        .weight(36f)
                        .padding(horizontal = 6.dp),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // underline under header
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = Color.Black.copy(alpha = 0.25f)
            )

            var showActionSheet by remember { mutableStateOf(false) }
            var selectedTxn: UiTxn? by remember { mutableStateOf(null) }

            // ===== Table rows =====
            LazyColumn {
                items(filtered, key = { it.id }) { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                                onLongClick = {
                                    selectedTxn = t
                                    showActionSheet = true
                                }
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t.occurredAt.format(dateFmt),
                            modifier = Modifier
                                .width(54.dp)
                                .padding(horizontal = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        VSep(28)

                        Text(
                            text = t.occurredAt.format(timeFmt) + if (t.manuallySetDateTime) " 🛈" else "",
                            modifier = Modifier
                                .width(46.dp)
                                .padding(horizontal = 6.dp),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        VSep(28)

                        Text(
                            text = t.category,
                            modifier = Modifier
                                .weight(38f)
                                .padding(horizontal = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        VSep(28)

                        Text(
                            text = "%.2f".format(t.amount),
                            modifier = Modifier
                                .weight(24f)
                                .padding(horizontal = 6.dp),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        VSep(28)

                        Text(
                            text = t.title ?: "",
                            modifier = Modifier
                                .weight(36f)
                                .padding(horizontal = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (showActionSheet) {
                ModalBottomSheet(onDismissRequest = { showActionSheet = false }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { showActionSheet = false },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            )
                            {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close"
                                )
                            }
                        }

                        TextButton(onClick = {
                            selectedTxn?.let { vm.beginEdit(it) }
                            showActionSheet = false
                        },modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                        { Text("Edit") }

                        TextButton(onClick = {
                            val deleted = selectedTxn ?: return@TextButton
                            vm.rememberDeleted(deleted)
                            showActionSheet = false
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
                        }, modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                        { Text("Delete") }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = java.time.ZonedDateTime.now()
                    .toInstant()
                    .toEpochMilli()
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
    }


    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = { TextButton(onClick = { showTimePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
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
            dismissButton = { TextButton(onClick = { futureWarn = null }) { Text("Cancel") } },
            title = { Text("Future date") },
            text = { Text("Selected time (${whenPicked.format(tsFmt)}) is in the future. Are you sure?") }
        )
    }
}
