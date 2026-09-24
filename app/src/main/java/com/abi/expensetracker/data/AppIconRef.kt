package com.abi.expensetracker.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/**
 * A bank icon that points at an installed app — eSewa's own launcher icon for the eSewa
 * wallet — instead of an emoji.
 *
 * Kept in the same [com.abi.expensetracker.data.model.Bank.icon] text column as the emoji,
 * tagged with a prefix, rather than in a second column: the choice stays one plain-text
 * field, so a backup written before app icons existed still restores, and a backup
 * restored on a phone without that app falls back to the name monogram instead of showing
 * a blank.
 *
 * The package name is stored, never the image. Icons change with app updates and a bitmap
 * in the database would freeze the old one and bloat every backup.
 */
object AppIconRef {

    private const val PREFIX = "app:"

    /** The stored form of [packageName], e.g. "app:com.f1soft.esewa". */
    fun of(packageName: String): String = PREFIX + packageName.trim()

    /** The package an icon points at, or null when it is an emoji or nothing. */
    fun packageOf(icon: String?): String? =
        icon?.takeIf { it.startsWith(PREFIX) }
            ?.removePrefix(PREFIX)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
}

/** An app on this phone that can be picked as a bank icon. */
data class InstalledApp(val packageName: String, val label: String)

/**
 * Every app with a launcher entry, sorted by name.
 *
 * Launcher entries only, which is what the manifest `<queries>` block allows without the
 * QUERY_ALL_PACKAGES permission — and it is also the right set: the user recognises their
 * banking apps by the icon they tap, not by background services.
 *
 * Blocking work: call it off the main thread.
 */
fun installedLauncherApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(launcher, 0)
        .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

/** The launcher icon for [packageName], or null once that app is uninstalled. */
fun appIconDrawable(context: Context, packageName: String): Drawable? =
    try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

/**
 * The name [packageName] shows under its launcher icon, or null when it is not an installed
 * app — which is also how an SMS sender id is told apart from a notification's package.
 *
 * Cached: the ledger asks for every row, and the label only changes with an app update.
 */
fun appLabel(context: Context, packageName: String): String? =
    appLabels.getOrPut(packageName) {
        try {
            val pm = context.packageManager
            pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }.ifEmpty { null }

private val appLabels = java.util.concurrent.ConcurrentHashMap<String, String>()
