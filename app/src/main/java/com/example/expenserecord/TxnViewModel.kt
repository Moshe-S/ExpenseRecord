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

    fun add(ui: UiTxn) {
        viewModelScope.launch {
            dao.insert(ui.toEntity())
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            dao.deleteById(id)
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
        id = 0, // Room will auto-generate ID on insert
        occurredAtEpochMillis = occurredAt.atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli(),
        category = category,
        amount = amount,
        title = title,
        manuallySetDateTime = manuallySetDateTime
    )
