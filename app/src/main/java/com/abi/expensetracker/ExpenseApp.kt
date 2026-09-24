package com.abi.expensetracker

import android.app.Application
import com.abi.expensetracker.di.ServiceLocator
import com.abi.expensetracker.notification.RemarkPrompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ExpenseApp : Application() {

    override fun onCreate() {
        super.onCreate()
        RemarkPrompt.ensureChannel(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val repository = ServiceLocator.repository(this@ExpenseApp)
            repository.seedRulesIfEmpty()
            repository.seedCategoriesIfEmpty()
            // A build that adds a parsing rule has to reread what the old rules could not
            // parse, or the messages it now understands stay invisible until the user
            // happens to find Reparse.
            // Copies stored before the ingest guard existed, and the transactions they
            // produced, go together: dropping the message alone would leave its row in
            // the ledger with nothing behind it.
            val removed = repository.removeDuplicateMessages()
            // A build that changes parsing or de-duplication logic bumps PARSER_VERSION.
            if (repository.syncBuiltInRules() || removed > 0 || repository.parserOutdated()) {
                repository.reparseAll()
            }
            // Catches history that predates categories, and anything a restored backup
            // brought in. A no-op once everything carries one.
            repository.categorizeUncategorized()
        }
    }
}
