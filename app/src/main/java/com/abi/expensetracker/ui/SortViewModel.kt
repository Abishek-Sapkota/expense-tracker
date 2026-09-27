package com.abi.expensetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abi.expensetracker.data.model.Category
import com.abi.expensetracker.data.model.Txn
import com.abi.expensetracker.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One payment waiting for a category, with the account it came through. */
data class SortItem(val txn: Txn, val bankName: String?, val bankIcon: String?)

/**
 * Uncategorised spending, one payment at a time.
 *
 * A list of two hundred rows to fix never gets opened twice; one payment with its
 * categories under it takes a tap, and a keyword filed with it clears the others like it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SortViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ServiceLocator.repository(app)

    /** Skipped this visit only: coming back later should offer them again. */
    private val skipped = MutableStateFlow(emptySet<String>())

    val queue: StateFlow<List<SortItem>> = combine(
        repository.observeUncategorisedDebits(),
        repository.observeBanks(),
        repository.observeSenderLinks(),
        skipped
    ) { rows, banks, links, skip ->
        val resolver = repository.resolver(banks, links)
        val byId = banks.associateBy { it.id }
        rows.filter { it.txn.id !in skip }.map { row ->
            val bank = if (row.txn.isManual) row.txn.bankId?.let { byId[it] }
            else resolver.bankFor(row.sender, row.body)
            SortItem(row.txn, bank?.name, bank?.icon)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The category this payment's merchant or remark was filed under before. */
    val suggestion: StateFlow<Long?> = queue
        .mapLatest { it.firstOrNull()?.txn }
        .distinctUntilChanged()
        .mapLatest { txn -> txn?.let { repository.likelyCategory(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    fun file(item: SortItem, category: Category, keyword: String?) = viewModelScope.launch {
        val more = repository.fileAs(item.txn, category.id, keyword)
        _status.value = "Filed under ${category.name}." +
            if (more > 0) " \"$keyword\" filed $more more." else ""
    }

    fun skip(item: SortItem) { skipped.value = skipped.value + item.txn.id }
}
