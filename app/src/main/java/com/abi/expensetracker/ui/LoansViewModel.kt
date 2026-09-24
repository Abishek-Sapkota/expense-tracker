package com.abi.expensetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abi.expensetracker.data.SettingsStore
import com.abi.expensetracker.data.SplitSummary
import com.abi.expensetracker.data.Splits
import com.abi.expensetracker.data.model.LoanEntry
import com.abi.expensetracker.data.model.Split
import com.abi.expensetracker.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything recorded with one person, and where it leaves the two of you. */
data class PersonLoans(
    val name: String,
    /** Positive: they owe the user. Negative: the user owes them. */
    val balanceMinor: Long,
    /** Newest first. */
    val entries: List<LoanEntry>
) {
    val lastActivity: Long get() = entries.maxOfOrNull { it.occurredAt } ?: 0L
}

data class LoansState(
    val people: List<PersonLoans> = emptyList(),
    val owedToMeMinor: Long = 0L,
    val iOweMinor: Long = 0L,
    /** Open splits first, newest first within each. */
    val splits: List<SplitSummary> = emptyList()
)

class LoansViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ServiceLocator.repository(app)

    val state: StateFlow<LoansState> = combine(repository.observeLoans(), repository.observeSplits()) { entries, splits ->
        summarise(entries).copy(
            splits = Splits.summarise(splits, entries)
                .sortedWith(compareBy<SplitSummary> { it.isSettled }.thenByDescending { it.split.createdAt })
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoansState())

    /** A friend paid their share in cash. */
    fun recordCashPayment(split: Split, person: String, amountMinor: Long) =
        viewModelScope.launch { repository.recordSplitPayment(split, person, amountMinor, via = null) }

    val useNepaliCalendar: StateFlow<Boolean> = SettingsStore(app).useNepaliCalendar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun save(entry: LoanEntry) = viewModelScope.launch { repository.saveLoan(entry) }

    fun delete(entry: LoanEntry) = viewModelScope.launch { repository.deleteLoan(entry) }

    companion object {
        /**
         * Grouped by name ignoring case and surrounding space, so "Ram" typed once and
         * "ram " typed later are one person. The newest spelling is the one shown.
         */
        fun summarise(entries: List<LoanEntry>): LoansState {
            val people = entries
                .groupBy { it.person.trim().lowercase() }
                .values
                .map { group ->
                    val newestFirst = group.sortedWith(
                        compareByDescending<LoanEntry> { it.occurredAt }.thenByDescending { it.id }
                    )
                    PersonLoans(
                        name = newestFirst.first().person.trim(),
                        balanceMinor = group.sumOf { it.signedMinor },
                        entries = newestFirst
                    )
                }
                // Open balances first, largest first; settled people after, most recent first.
                .sortedWith(
                    compareBy<PersonLoans> { it.balanceMinor == 0L }
                        .thenByDescending { kotlin.math.abs(it.balanceMinor) }
                        .thenByDescending { it.lastActivity }
                )
            return LoansState(
                people = people,
                owedToMeMinor = people.filter { it.balanceMinor > 0 }.sumOf { it.balanceMinor },
                iOweMinor = people.filter { it.balanceMinor < 0 }.sumOf { -it.balanceMinor }
            )
        }
    }
}
