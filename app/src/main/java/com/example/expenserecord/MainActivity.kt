package com.example.expenserecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

data class UiTxn(
    val date: LocalDate,
    val category: String,
    val amount: Double,
    val title: String?
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
    var category by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var txns by remember { mutableStateOf(listOf<UiTxn>()) }
    var query by remember { mutableStateOf("") }

    val focus = LocalFocusManager.current

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
        val a = amount.toDoubleOrNull()
        if (category.isNotBlank() && a != null && a > 0.0) {
            txns = listOf(
                UiTxn(LocalDate.now(), category.trim(), a, title.takeIf { it.isNotBlank() })
            ) + txns
            amount = ""
            title = ""
            focus.clearFocus()
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
        topBar = { TopAppBar(title = { Text("Expense Record") }) }
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

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { e ->
                            if (e.type == KeyEventType.KeyUp &&
                                (e.key == Key.Enter || e.key == Key.NumPadEnter)
                            ) {
                                addIfValid()
                                true
                            } else false
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

            HorizontalDivider()

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search (category or details)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .handleTabNext(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() })
            )
            Text("Total: ${"%.2f".format(total)}", style = MaterialTheme.typography.titleMedium)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered) { t ->
                    Text(
                        "${t.date} • ${t.category} • ${"%.2f".format(t.amount)}" +
                                (t.title?.let { " • $it" } ?: "")
                    )
                }
            }
        }
    }
}
