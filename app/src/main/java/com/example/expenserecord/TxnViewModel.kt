package com.example.expenserecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expenserecord.data.TxnDao
import com.example.expenserecord.data.TxnEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // ----- Editing state -----
    private val _editing = MutableStateFlow<UiTxn?>(null)
    val editing: StateFlow<UiTxn?> = _editing.asStateFlow()

    fun beginEdit(ui: UiTxn) { _editing.value = ui }
    fun cancelEdit() { _editing.value = null }

    /**
     * Save an edited transaction.
     * The UI should pass the updated fields as UiTxn (the id is taken from current editing).
     */
    fun saveEdit(updated: UiTxn) {
        val current = _editing.value ?: return
        val toUpdate = updated.copy(id = current.id)
        viewModelScope.launch {
            dao.update(toUpdate.toEntityForUpdate())
            _editing.value = null
        }
    }

    // ----- Create / Delete / Undo -----
    fun add(ui: UiTxn) {
        viewModelScope.launch {
            dao.insert(ui.toEntityForInsert())
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            dao.deleteById(id)
        }
    }

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

// ----- Mappers -----

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

/**
 * For inserts: let the DB auto-generate the id.
 */
private fun UiTxn.toEntityForInsert(): TxnEntity =
    TxnEntity(
        id = 0, // auto-generate
        occurredAtEpochMillis = occurredAt.atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli(),
        category = category,
        amount = amount,
        title = title,
        manuallySetDateTime = manuallySetDateTime
    )

/**
 * For updates: preserve the existing id.
 */
private fun UiTxn.toEntityForUpdate(): TxnEntity =
    TxnEntity(
        id = id, // must keep the existing id for @Update
        occurredAtEpochMillis = occurredAt.atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli(),
        category = category,
        amount = amount,
        title = title,
        manuallySetDateTime = manuallySetDateTime
    )
