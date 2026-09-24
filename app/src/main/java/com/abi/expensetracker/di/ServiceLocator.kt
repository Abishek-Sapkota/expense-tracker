package com.abi.expensetracker.di

import android.content.Context
import com.abi.expensetracker.backup.BackupManager
import com.abi.expensetracker.data.ExpenseRepository
import com.abi.expensetracker.data.SettingsStore
import com.abi.expensetracker.data.db.AppDatabase

/**
 * Plain manual wiring. One app, few objects — a DI framework here would be more moving
 * parts than the graph it manages.
 */
object ServiceLocator {

    @Volatile private var repository: ExpenseRepository? = null
    @Volatile private var backup: BackupManager? = null

    fun repository(context: Context): ExpenseRepository {
        val app = context.applicationContext
        return repository ?: synchronized(this) {
            repository ?: ExpenseRepository(
                context = app,
                db = AppDatabase.get(app),
                settings = SettingsStore(app)
            ).also { repository = it }
        }
    }

    fun backupManager(context: Context): BackupManager {
        val app = context.applicationContext
        return backup ?: synchronized(this) {
            backup ?: BackupManager(
                context = app,
                db = AppDatabase.get(app),
                settings = SettingsStore(app)
            ).also { backup = it }
        }
    }
}
