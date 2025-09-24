package com.example.expenserecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expenserecord.data.TxnDao
import com.example.expenserecord.data.TxnEntity
import com.example.expenserecord.data.CategoryDao
import com.example.expenserecord.data.CategoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.ZoneId

class TxnViewModel : ViewModel() {
    private val dao: TxnDao = DbProvider.db.txnDao()
    private val categoryDao: CategoryDao = DbProvider.db.categoryDao()

    private val _currentMonth = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    val currentMonth: StateFlow<LocalDate> = _currentMonth.asStateFlow()

    private fun getStartOfMonth(date: LocalDate): Long {
        val startOfMonth = date.withDayOfMonth(1).atTime(0, 0, 0, 0)
        return startOfMonth.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun getEndOfMonth(date: LocalDate): Long {
        val endOfMonth = date.withDayOfMonth(date.lengthOfMonth()).atTime(23, 59, 59, 999999999)
        return endOfMonth.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun goToPreviousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun goToNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun canGoToNextMonth(): Boolean {
        return _currentMonth.value.isBefore(LocalDate.now().withDayOfMonth(1))
    }
    private fun initializeDefaultCategories() {
        viewModelScope.launch {
            val defaults = listOf("Supermarket", "Transport", "Rent", "Bills", "Shopping", "Pharmacy")
            defaults.forEach { categoryName ->
                categoryDao.insertOrUpdate(
                    CategoryEntity(
                        name = categoryName,
                        lastUsed = System.currentTimeMillis(),
                        usageCount = 0
                    )
                )
            }
        }
    }

    init {
        initializeDefaultCategories()
    }

    val txns = _currentMonth
        .map { month ->
            dao.getByDateRange(getStartOfMonth(month), getEndOfMonth(month))
        }
        .flatMapLatest { it }
        .map { list -> list.map { it.toUi() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentCategories = categoryDao.getRecentCategories()
        .map { list -> list.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCategoryToHistory(category: String) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val categoryEntity = CategoryEntity(
                name = category,
                lastUsed = currentTime,
                usageCount = 1
            )
            categoryDao.insertOrUpdate(categoryEntity)
        }
    }

    // ----- Editing state -----
    private val _editing = MutableStateFlow<UiTxn?>(null)
    val editing: StateFlow<UiTxn?> = _editing.asStateFlow()

    fun beginEdit(ui: UiTxn) { _editing.value = ui }
    fun cancelEdit() { _editing.value = null }

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
            addCategoryToHistory(ui.category)
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

private fun UiTxn.toEntityForInsert(): TxnEntity =
    TxnEntity(
        id = 0,
        occurredAtEpochMillis = occurredAt.atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli(),
        category = category,
        amount = amount,
        title = title,
        manuallySetDateTime = manuallySetDateTime
    )

private fun UiTxn.toEntityForUpdate(): TxnEntity =
    TxnEntity(
        id = id,
        occurredAtEpochMillis = occurredAt.atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli(),
        category = category,
        amount = amount,
        title = title,
        manuallySetDateTime = manuallySetDateTime
    )

