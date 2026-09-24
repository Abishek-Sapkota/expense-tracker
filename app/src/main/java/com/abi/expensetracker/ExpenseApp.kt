package com.abi.expensetracker

import com.abi.expensetracker.data.SettingsStore
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
            // Cheap count checks: a fresh install needs its rules and categories.
            repository.seedRulesIfEmpty()
            repository.seedCategoriesIfEmpty()

            // The rest is maintenance for a new build, and this process also starts in the
            // background for every incoming SMS and for the notification listener. Doing
            // it on each of those starts rescanned every stored message for nothing, so it
            // runs once per install or update, keyed on the package's install time.
            val settings = SettingsStore(this@ExpenseApp)
            val installed = packageManager.getPackageInfo(packageName, 0).lastUpdateTime
            if (settings.maintenanceStampOnce() == installed) return@launch

            // Copies stored before the ingest guard existed, and the transactions they
            // produced, go together: dropping the message alone would leave its row in
            // the ledger with nothing behind it.
            val removed = repository.removeDuplicateMessages()
            // A build that adds a parsing rule has to reread what the old rules could not
            // parse; a build that changes parsing or de-duplication bumps PARSER_VERSION.
            if (repository.syncBuiltInRules() || removed > 0 || repository.parserOutdated()) {
                repository.reparseAll()
            }
            // Catches history that predates categories. Category edits and backup imports
            // run their own pass, so once per build is enough here.
            repository.categorizeUncategorized()
            settings.setMaintenanceStamp(installed)
        }
    }
}
