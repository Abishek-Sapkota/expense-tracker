package com.abi.expensetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abi.expensetracker.data.Period
import com.abi.expensetracker.data.SettingsStore
import com.abi.expensetracker.data.db.DuplicateRow
import com.abi.expensetracker.di.ServiceLocator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Messages folded into another channel's transaction, filtered by when they arrived. */
@OptIn(ExperimentalCoroutinesApi::class)
class DuplicatesViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ServiceLocator.repository(app)

    /** Today by default: duplicates are reviewed as they arrive, not as history. */
    private val _selection = MutableStateFlow(PeriodSelection())
    private val settings = SettingsStore(app)
    val selection: StateFlow<PeriodSelection> =
        combine(_selection, settings.useNepaliCalendar) { sel, nepali -> sel.copy(nepali = nepali) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, PeriodSelection())

    val rows: StateFlow<List<DuplicateRow>> = selection
        .flatMapLatest { repository.observeDuplicates(it.range) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val useNepaliCalendar: StateFlow<Boolean> = settings.useNepaliCalendar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    fun selectPeriod(period: Period) {
        _selection.value = PeriodSelection(period = period)
    }

    fun selectCustomRange(start: LocalDate, end: LocalDate) {
        _selection.value = PeriodSelection(Period.CUSTOM, start, end)
    }

    fun markNotDuplicate(row: DuplicateRow) = viewModelScope.launch {
        _status.value = if (repository.markNotDuplicate(row.rawId)) {
            "Moved to the ledger as its own transaction."
        } else {
            "Marked not a duplicate, but no rule reads this message as a transaction."
        }
    }

    fun clearStatus() { _status.value = null }
}
