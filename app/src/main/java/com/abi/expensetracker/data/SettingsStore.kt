package com.abi.expensetracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.abi.expensetracker.data.model.LimitBasis
import com.abi.expensetracker.data.model.SpendingLimit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val lastSyncedKey = longPreferencesKey("lastSyncedSmsDate")
    private val accentKey = stringPreferencesKey("accentColor")
    private val permissionPromptKey = booleanPreferencesKey("permissionPromptAnswered")
    private val onboardingDoneKey = booleanPreferencesKey("onboardingDone")
    private val limitAmountKey = longPreferencesKey("spendingLimitMinor")
    private val limitBasisKey = stringPreferencesKey("spendingLimitBasis")
    private val themeModeKey = stringPreferencesKey("themeMode")
    private val neutralPaletteKey = stringPreferencesKey("neutralPalette")
    private val customAccentKey = intPreferencesKey("customAccentArgb")
    private val askUncategorisedKey = booleanPreferencesKey("askUncategorised")
    private val notificationAppsKey = stringSetPreferencesKey("notificationApps")
    private val nepaliCalendarKey = booleanPreferencesKey("useNepaliCalendar")
    private val parserVersionKey = intPreferencesKey("parserVersion")
    private val maintenanceStampKey = longPreferencesKey("maintenanceStamp")

    /** Watermark so repeat backfills read only what arrived since the last one. */
    val lastSyncedSmsDate: Flow<Long> =
        context.dataStore.data.map { it[lastSyncedKey] ?: 0L }

    suspend fun lastSyncedSmsDateOnce(): Long = lastSyncedSmsDate.first()

    suspend fun setLastSyncedSmsDate(value: Long) {
        context.dataStore.edit { it[lastSyncedKey] = value }
    }

    /**
     * Whether the startup permission dialog has had its answer.
     *
     * Set once the user grants everything or says "Not now". Declining is an answer, so
     * the dialog does not come back on the next launch to ask again — the same two cards
     * stay in Accounts for whenever the user wants them.
     */
    val permissionPromptAnswered: Flow<Boolean> =
        context.dataStore.data.map { it[permissionPromptKey] ?: false }

    /** Whether the first-run guide has been finished (or skipped for an existing setup). */
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[onboardingDoneKey] ?: false }

    /**
     * Whether the guide flag was ever written. Unset only on an install that has never
     * finished or rerun the guide, which is the one case where existing accounts should
     * skip it; a "Run setup guide again" writes false and must not be skipped.
     */
    val onboardingDecided: Flow<Boolean> = context.dataStore.data.map { onboardingDoneKey in it }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[onboardingDoneKey] = done }
    }

    suspend fun setPermissionPromptAnswered() {
        context.dataStore.edit { it[permissionPromptKey] = true }
    }

    /**
     * Whether dates are read on the Bikram Sambat calendar.
     *
     * One switch for the whole app rather than a choice per screen: a month that means
     * Ashwin in the budget and September in the trends is two different months called by
     * one name, and the user would have to hold both in their head to read either.
     */
    val useNepaliCalendar: Flow<Boolean> =
        context.dataStore.data.map { it[nepaliCalendarKey] ?: false }

    suspend fun setUseNepaliCalendar(enabled: Boolean) {
        context.dataStore.edit { it[nepaliCalendarKey] = enabled }
    }

    /**
     * The spending limit, or null when none is set.
     *
     * A zero or negative stored amount reads as "no limit" rather than as a limit of
     * nothing: it is what an emptied field would otherwise leave behind, and a limit of
     * zero would put every ledger permanently over budget.
     */
    val spendingLimit: Flow<SpendingLimit?> = context.dataStore.data.map { prefs ->
        val amount = prefs[limitAmountKey] ?: 0L
        if (amount <= 0L) null
        else SpendingLimit(amount, LimitBasis.fromName(prefs[limitBasisKey]))
    }

    suspend fun setSpendingLimit(amountMinor: Long, basis: LimitBasis) {
        context.dataStore.edit {
            it[limitAmountKey] = amountMinor
            it[limitBasisKey] = basis.name
        }
    }

    suspend fun clearSpendingLimit() {
        context.dataStore.edit { it.remove(limitAmountKey) }
    }

    /**
     * Name of the chosen accent preset, or [CUSTOM_ACCENT] when the user mixed their own.
     * Unset means the design's default terracotta.
     */
    val accentName: Flow<String?> = context.dataStore.data.map { it[accentKey] }

    suspend fun setAccentName(value: String) {
        context.dataStore.edit { it[accentKey] = value }
    }

    /**
     * The hand-picked accent as packed ARGB, or null when a preset is in use.
     *
     * Kept even while a preset is selected, so switching back to Custom returns to the
     * colour the user mixed rather than to a default they would have to find again.
     */
    val customAccentArgb: Flow<Int?> = context.dataStore.data.map { it[customAccentKey] }

    suspend fun setCustomAccent(argb: Int) {
        context.dataStore.edit {
            it[customAccentKey] = argb
            it[accentKey] = CUSTOM_ACCENT
        }
    }

    /** Light, dark, or whatever the system is doing. */
    val themeMode: Flow<String?> = context.dataStore.data.map { it[themeModeKey] }

    /** The background palette's enum name; null means the default. */
    val neutralPalette: Flow<String?> = context.dataStore.data.map { it[neutralPaletteKey] }

    suspend fun setNeutralPalette(value: String) {
        context.dataStore.edit { it[neutralPaletteKey] = value }
    }

    suspend fun setThemeMode(value: String) {
        context.dataStore.edit { it[themeModeKey] = value }
    }

    /** Whether a new payment no category matched gets a reply-to-name notification. On by default. */
    val askUncategorised: Flow<Boolean> = context.dataStore.data.map { it[askUncategorisedKey] ?: true }

    suspend fun setAskUncategorised(enabled: Boolean) {
        context.dataStore.edit { it[askUncategorisedKey] = enabled }
    }

    /**
     * Packages whose notifications are read, or null when the list was never set (an
     * install from before the list existed; the repository seeds it once).
     *
     * An allowlist, not every app: a notification from an app not on it is dropped before
     * it is stored, so a chat that mentions "Rs 500" never reaches the database.
     */
    val notificationApps: Flow<Set<String>?> = context.dataStore.data.map { it[notificationAppsKey] }

    suspend fun setNotificationApps(packages: Set<String>) {
        context.dataStore.edit { it[notificationAppsKey] = packages }
    }

    suspend fun setNotificationApp(packageName: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[notificationAppsKey] ?: emptySet()
            prefs[notificationAppsKey] = if (enabled) current + packageName else current - packageName
        }
    }

    /** The [com.abi.expensetracker.data.PARSER_VERSION] stored messages were last parsed with. */
    suspend fun parserVersionOnce(): Int =
        context.dataStore.data.map { it[parserVersionKey] ?: 0 }.first()

    suspend fun setParserVersion(value: Int) {
        context.dataStore.edit { it[parserVersionKey] = value }
    }

    /** The install time the one-off startup maintenance last ran for; see ExpenseApp. */
    suspend fun maintenanceStampOnce(): Long =
        context.dataStore.data.map { it[maintenanceStampKey] ?: 0L }.first()

    suspend fun setMaintenanceStamp(value: Long) {
        context.dataStore.edit { it[maintenanceStampKey] = value }
    }

    companion object {
        /** Sentinel in [accentName]: the accent is [customAccentArgb], not a preset. */
        const val CUSTOM_ACCENT = "CUSTOM"
    }
}
