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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.ZoneId
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

class TxnViewModel : ViewModel() {
    private val dao: TxnDao = DbProvider.db.txnDao()
    private val categoryDao: CategoryDao = DbProvider.db.categoryDao()

    // ----- Period anchors -----
    private val _currentMonth = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    val currentMonth: StateFlow<LocalDate> = _currentMonth.asStateFlow()

    // Week navigation anchor (any date inside the week)
    private val _currentWeekAnchor = MutableStateFlow(LocalDate.now())

    // ----- View mode state -----
    enum class ViewMode { MONTH, WEEK }

    private val _viewMode = MutableStateFlow(ViewMode.MONTH)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()
    fun setViewMode(mode: ViewMode) { _viewMode.value = mode }

    private val _startOfWeek = MutableStateFlow(DayOfWeek.MONDAY) // default Monday
    val startOfWeek: StateFlow<DayOfWeek> = _startOfWeek.asStateFlow()
    fun setStartOfWeek(day: DayOfWeek) { _startOfWeek.value = day }

    // ----- Time helpers -----
    private fun getStartOfMonth(date: LocalDate): Long {
        val startOfMonth = date.withDayOfMonth(1).atTime(0, 0, 0, 0)
        return startOfMonth.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun getEndOfMonth(date: LocalDate): Long {
        val endOfMonth = date.withDayOfMonth(date.lengthOfMonth()).atTime(23, 59, 59, 999_999_999)
        return endOfMonth.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun monthRange(date: LocalDate): Pair<Long, Long> =
        getStartOfMonth(date) to getEndOfMonth(date)

    private fun weekRange(anchor: LocalDate, startOn: DayOfWeek): Pair<Long, Long> {
        val start = anchor.with(TemporalAdjusters.previousOrSame(startOn))
        val end = start.plusDays(6)
        val startMillis = start.atTime(0, 0, 0, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = end.atTime(23, 59, 59, 999_999_999)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return startMillis to endMillis
    }

    private fun thisWeekStart(startOn: DayOfWeek): LocalDate =
        LocalDate.now().with(TemporalAdjusters.previousOrSame(startOn))

    // ----- Navigation (kept public names for UI compatibility) -----
    fun goToPreviousMonth() {
        when (_viewMode.value) {
            ViewMode.MONTH -> _currentMonth.value = _currentMonth.value.minusMonths(1)
            ViewMode.WEEK  -> _currentWeekAnchor.value = _currentWeekAnchor.value.minusWeeks(1)
        }
    }

    fun goToNextMonth() {
        if (!canGoToNextMonth()) return
        when (_viewMode.value) {
            ViewMode.MONTH -> _currentMonth.value = _currentMonth.value.plusMonths(1)
            ViewMode.WEEK  -> _currentWeekAnchor.value = _currentWeekAnchor.value.plusWeeks(1)
        }
    }

    fun canGoToNextMonth(): Boolean {
        return when (_viewMode.value) {
            ViewMode.MONTH -> {
                _currentMonth.value.isBefore(LocalDate.now().withDayOfMonth(1))
            }
            ViewMode.WEEK -> {
                val startOn = _startOfWeek.value
                val currentStart = _currentWeekAnchor.value.with(TemporalAdjusters.previousOrSame(startOn))
                val realCurrentWeekStart = thisWeekStart(startOn)
                currentStart.isBefore(realCurrentWeekStart)
            }
        }
    }

    // ----- Defaults -----
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

    // ----- Current range that drives the list -----
    val currentRange: StateFlow<Pair<Long, Long>> =
        combine(_viewMode, _currentMonth, _currentWeekAnchor, _startOfWeek) { mode, month, weekAnchor, startDay ->
            when (mode) {
                ViewMode.MONTH -> monthRange(month)
                ViewMode.WEEK  -> weekRange(weekAnchor, startDay)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), monthRange(LocalDate.now()))

    // ----- Streams -----
    val txns = currentRange
        .map { (start, end) ->
            dao.getByDateRange(start, end)
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