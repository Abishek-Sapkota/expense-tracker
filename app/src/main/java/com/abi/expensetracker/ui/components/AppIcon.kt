package com.abi.expensetracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.abi.expensetracker.data.appIconDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Icons already rasterised, so a ledger full of one bank's rows decodes it once. */
private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

/** The pixel size icons are decoded at — larger than any place they are drawn, so a 40dp
 *  swatch and a 48dp picker cell both stay sharp on a 3x screen. */
private const val ICON_PX = 144

/**
 * The launcher icon of [packageName], or null while it loads and for good once that app
 * is gone.
 *
 * Callers draw their own fallback on null rather than a placeholder image: an uninstalled
 * app should read the same as a bank with no icon set, not as a broken one.
 */
@Composable
fun rememberAppIcon(packageName: String?): ImageBitmap? {
    val context = LocalContext.current
    var icon by remember(packageName) { mutableStateOf(packageName?.let(iconCache::get)) }

    LaunchedEffect(packageName) {
        if (packageName == null || icon != null) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            appIconDrawable(context, packageName)
                ?.toBitmap(ICON_PX, ICON_PX)
                ?.asImageBitmap()
        }
        if (loaded != null) iconCache[packageName] = loaded
        icon = loaded
    }

    return icon
}
