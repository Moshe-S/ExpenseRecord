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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import kotlin.math.floor
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import com.example.expenserecord.ui.theme.focusHighlight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ViewWeek

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
    val sepColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp) // breathing room from text
            .width(1.dp)
            .fillMaxHeight()
            .drawBehind {
                val x = floor(size.width / 2f) + 0.5f
                drawLine(
                    color = sepColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
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
    var searchOpen by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
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
    val amountFocusRequester = remember { FocusRequester() }

    val editing by vm.editing.collectAsState()
    val viewMode by vm.viewMode.collectAsState()
    val listState = rememberLazyListState()
    var sortNewestFirst by remember { mutableStateOf(true) }

    val filtered = remember(txns, query, selectedCategories) {
        val q = query.trim().lowercase()
        val base = if (q.isEmpty()) {
            txns
        } else {
            txns.filter {
                it.category.lowercase().contains(q) ||
                        (it.title ?: "").lowercase().contains(q)
            }
        }

        if (selectedCategories.isNotEmpty()) {
            base.filter { it.category in selectedCategories }
        } else {
            base
        }
    }

    val total = filtered.sumOf { it.amount }
    val allCategories = remember(txns) { txns.map { it.category }.distinct().sorted() }
    val displayed = remember(filtered, sortNewestFirst) {
        if (sortNewestFirst) filtered.sortedByDescending { it.occurredAt }
        else filtered.sortedBy { it.occurredAt }
    }
    LaunchedEffect(sortNewestFirst) {
        listState.scrollToItem(0)
    }

    fun addIfValid() {
        val normalized = amount.replace(',', '.')
        val a = normalized.toDoubleOrNull()
        if (category.text.isBlank() || a == null || a <= 0.0) return

        val occurred = LocalDateTime.of(pickedDate, pickedTime)
        val catN = category.text.trim().lowercase().replaceFirstChar { it.uppercase() }
        val commit: () -> Unit = {
        vm.add(
                UiTxn(
                    occurredAt = occurred,
                    category = catN,
                    amount = a,
                    title = title.takeIf { it.isNotBlank() },
                    manuallySetDateTime = dateTimeChangedManually
                )
            )
            val catN = category.text.trim().lowercase().replaceFirstChar { it.titlecase() }

            vm.addCategoryToHistory(catN)
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
                amountFocusRequester.requestFocus()
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
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Inputs row 1: Category + Details
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.focusHighlight(isFocused = (focusedField == "category"))
                        ) {
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
                                keyboardActions = KeyboardActions(onNext = { amountFocusRequester.requestFocus() }))
                        }


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
                                                    focus.clearFocus()
                                                    category = TextFieldValue(categoryName, selection = TextRange(categoryName.length))
                                                    amountFocusRequester.requestFocus()

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
                            .focusHighlight(isFocused = (focusedField == "details"))
                            .weight(1f)
                            .handleTabNext()
                            .onFocusChanged { focusState ->
                                focusedField = if (focusState.isFocused) "details" else null
                            },
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
                        .focusHighlight(isFocused = (focusedField == "amount"))
                        .focusRequester(amountFocusRequester)
                        .weight(0.4f)
                        .onPreviewKeyEvent { e ->
                            if (
                                e.type == KeyEventType.KeyUp &&
                                (e.key == Key.Enter || e.key == Key.NumPadEnter)
                            ) { addIfValid(); true } else false
                        }
                        .onFocusChanged { focusState ->
                            focusedField = if (focusState.isFocused) "amount" else null
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Number),
                    keyboardActions = KeyboardActions(onDone = { addIfValid() })
                )

                val preview = LocalDateTime.of(pickedDate, pickedTime).format(tsFmt)
                Box(modifier = Modifier.weight(0.6f)) {
                    OutlinedTextField(
                        value = preview,
                        onValueChange = {},
                        label = { Text("Date/Time${if (dateTimeChangedManually) " 🛈" else ""}") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                focusedField = null
                                focus.clearFocus()
                                showDatePicker = true
                            })
                            {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit date and time",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }

                            )
                            {
                                focusedField = null
                                focus.clearFocus()
                                showDatePicker = true
                            }

                    )
                }
            }

            if (editing == null) {
                Button(
                    onClick = { addIfValid() },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .height(56.dp)
                        .widthIn(min = 150.dp)
                ) { Text("Add", fontSize = 18.sp) }
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
                            val catN = category.text.trim().lowercase().replaceFirstChar { it.uppercase() }
                            val currentId = vm.editing.value?.id ?: return@Button

                            val updated = UiTxn(
                                id = currentId,
                                occurredAt = occurred,
                                category = catN,
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
            val searchFocusRequester = remember { FocusRequester() }

            if (searchOpen) {
                LaunchedEffect(searchOpen) {
                    if (searchOpen) {
                        searchFocusRequester.requestFocus()
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search (category or details)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .handleTabNext()
                        .focusRequester(searchFocusRequester),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            query = ""
                            searchOpen = false
                            focus.clearFocus()
                        }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close search")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() })
                )
            }


// Total + actions (Sort | Total | Search)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp) // was 8.dp
                ) {
                    IconButton(
                        onClick = {
                            sortNewestFirst = !sortNewestFirst
                            scope.launch {
                                val msg = if (sortNewestFirst) "Sorting: Newest first" else "Sorting: Oldest first"
                                val current = snackbarHostState.currentSnackbarData
                                if (current?.visuals?.message?.startsWith("Sorting:") == true) {
                                    current.dismiss()
                                }
                                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                            }
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "▲",
                                fontSize = 22.sp,
                                fontWeight = if (sortNewestFirst) FontWeight.Bold else FontWeight.Normal,
                                color = if (sortNewestFirst)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                            Text(
                                text = "▲",
                                fontSize = 22.sp,
                                fontWeight = if (!sortNewestFirst) FontWeight.Bold else FontWeight.Normal,
                                color = if (!sortNewestFirst)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                modifier = Modifier.padding(start = 0.dp).scale(scaleX = 1f, scaleY = -1f)
                            )
                        }
                    }

                    TextButton(onClick = {
                        vm.setViewMode(
                            if (viewMode == TxnViewModel.ViewMode.MONTH) TxnViewModel.ViewMode.WEEK
                            else TxnViewModel.ViewMode.MONTH
                        )
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (viewMode == TxnViewModel.ViewMode.WEEK)
                                    Icons.Filled.ViewWeek
                                else
                                    Icons.Filled.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (viewMode == TxnViewModel.ViewMode.WEEK) "Week" else "Month",
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Text(
                    "Total: ${"%.2f".format(total)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(imageVector = Icons.Filled.FilterList, contentDescription = "Filter")
                    }
                    IconButton(onClick = { searchOpen = true }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    }
                }
            }


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
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Date/Time",
                    modifier = Modifier
                        .width(96.dp)
                        .padding(horizontal = 6.dp),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                VSep(20)


                Text(
                    "Category",
                    modifier = Modifier
                        .weight(44f)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                VSep(20)

                Text(
                "₪",
                    modifier = Modifier
                        .weight(22f)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                VSep(20)

                Text(
                    "Details",
                    modifier = Modifier
                        .weight(34f)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
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
            LazyColumn(state = listState, modifier = Modifier.weight(1f, fill = true).fillMaxWidth()) {
                itemsIndexed(displayed, key = { _, it -> it.id }) { index, t ->
                Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                                onLongClick = {
                                    selectedTxn = t
                                    showActionSheet = true
                                }
                            )
                            .background(
                                if (index % 2 == 1)
                                    Color(0xFFEFF3F6).copy(alpha = 0.85f)
                                else
                                    Color.Transparent
                            ),

                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Text(
                        text = "${t.occurredAt.format(dateFmt)}, ${t.occurredAt.format(timeFmt)}",
                        modifier = Modifier
                            .width(96.dp)
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    VSep(28)



                        Text(
                            text = t.category,
                            modifier = Modifier
                                .weight(44f)
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        VSep(28)

                        Text(
                            text = "%.2f".format(t.amount),
                            modifier = Modifier
                                .weight(22f)
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        VSep(28)

                        Text(
                            text = t.title ?: "",
                            modifier = Modifier
                                .weight(34f)
                                .padding(horizontal = 6.dp, vertical = 6.dp),
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

    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

            Text("Filter by category", style = MaterialTheme.typography.titleMedium)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { showFilterSheet = false },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { selectedCategories = emptySet() }) { Text("Clear") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { showFilterSheet = false }) { Text("Apply") }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {



                items(allCategories) { cat ->
                        val checked = cat in selectedCategories
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCategories = if (checked) selectedCategories - cat else selectedCategories + cat
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selectedCategories = if (checked) selectedCategories - cat else selectedCategories + cat
                                }
                            )
                            Text(cat, modifier = Modifier.padding(start = 8.dp))
                        }
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
                        focusedField = null
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
