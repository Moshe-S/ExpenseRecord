package com.example.expenserecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expenserecord.data.TxnDao
import com.example.expenserecord.data.TxnEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class TxnViewModel : ViewModel() {
    private val dao: TxnDao = DbProvider.db.txnDao()

    val txns = dao.all()
        .map { list -> list.map { it.toUi() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Add (used everywhere, including Undo)
    fun add(ui: UiTxn) {
        viewModelScope.launch {
            dao.insert(ui.toEntity())
        }
    }

    // Delete by ID (matches your current DAO: deleteById)
    fun delete(id: Long) {
        viewModelScope.launch {
            dao.deleteById(id)
        }
    }

    // Keep last deleted (for Snackbar Undo)
    var recentlyDeleted: UiTxn? = null
        private set

    fun rememberDeleted(ui: UiTxn) {
        recentlyDeleted = ui
    }

    fun restoreLastDeleted() {
        viewModelScope.launch {
            recentlyDeleted?.let { add(it) }
            recentlyDeleted = null
        }
    }
}

private fun TxnEntity.toUi(): UiTxn =
    UiTxn(
        id = id,
        occurredAt = Instant.ofEpochMilli(occurredAtEpochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime(),
        category = category,
        amount = amount,
        title = title,
        manuallySetDateTime = manuallySetDateTime
    )

private fun UiTxn.toEntity(): TxnEntity =
    TxnEntity(
        id = 0, // auto-generate on insert
        occurredAtEpochMillis = occurredAt.atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli(),
        category = category,
        amount = amount,
        title = title,
        manuallySetDateTime = manuallySetDateTime
    )
