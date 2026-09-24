package com.abi.expensetracker.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.Icons
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.abi.expensetracker.notification.TxnNotificationListener
import com.abi.expensetracker.ui.theme.PillShape

/**
 * One grant the app needs, and the single action that asks for it.
 *
 * The two grants behave nothing alike — messages is a runtime permission with a system
 * dialog, notification access is a switch buried in system settings with no dialog at all
 * — but every screen that offers them wants the same two things, so they are presented
 * through one shape.
 */
@Stable
class PermissionState(
    val granted: Boolean,
    val request: () -> Unit
)

/**
 * Re-reads [check] whenever the activity resumes.
 *
 * Both grants can change while the app is in the background — the user may be standing in
 * system settings — and neither sends the app an event when it does.
 */
@Composable
private fun rememberGrantedOnResume(check: () -> Boolean): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(check()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = check()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return granted
}

/** Reading the SMS inbox, and catching messages as they arrive. */
@Composable
fun rememberSmsPermissionState(): PermissionState {
    val context = LocalContext.current
    val check = {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
    }
    var granted by remember { mutableStateOf(check()) }
    val resumed = rememberGrantedOnResume(check)
    // A grant made in system app info arrives on resume; one made in the dialog arrives
    // in the launcher callback. Either is enough.
    if (resumed && !granted) granted = true

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> granted = result[Manifest.permission.READ_SMS] == true }

    return PermissionState(granted = granted) {
        launcher.launch(
            arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
        )
    }
}

/**
 * Posting the "what was it for?" question.
 *
 * Granted by default below Android 13, which had no such permission — [PermissionState]
 * reports it as already on there rather than offering a request that would do nothing.
 */
@Composable
fun rememberPostNotificationsState(): PermissionState {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return PermissionState(granted = true) { }
    }

    val check = {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
    var granted by remember { mutableStateOf(check()) }
    val resumed = rememberGrantedOnResume(check)
    if (resumed && !granted) granted = true

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> granted = result[Manifest.permission.POST_NOTIFICATIONS] == true }

    return PermissionState(granted = granted) {
        launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
    }
}

/** Reading bank and wallet notifications. No runtime dialog exists for this one. */
@Composable
fun rememberNotificationAccessState(): PermissionState {
    val context = LocalContext.current
    val granted = rememberGrantedOnResume { TxnNotificationListener.isEnabled(context) }
    return PermissionState(granted = granted) {
        context.startActivity(TxnNotificationListener.settingsIntent())
    }
}

/**
 * A grant as a card: what it buys, whether it is on, and the one button that asks.
 *
 * A granted card keeps its explanation rather than collapsing to a tick, so the screen
 * still answers "what is this app reading?" after everything is on.
 */
@Composable
fun PermissionCard(
    title: String,
    body: String,
    state: PermissionState,
    actionLabel: String,
    grantedActionLabel: String = "Manage",
    footnote: String? = null,
    modifier: Modifier = Modifier
) {
    LedgerCard(modifier) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (state.granted) {
                    StatusChip(
                        text = "On",
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    StatusChip(
                        text = "Off",
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            footnote?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = state.request, shape = PillShape) {
                Text(if (state.granted) grantedActionLabel else actionLabel)
            }
        }
    }
}

/**
 * Reads the SMS inbox into the app from wherever senders are being set up, so a first-time
 * user is not sent to Settings mid-task. Asks for SMS access first when it is missing:
 * querying the inbox without it throws.
 */
@Composable
fun SyncSmsControl(syncing: Boolean, status: String?, onSync: () -> Unit, modifier: Modifier = Modifier) {
    val sms = rememberSmsPermissionState()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (sms.granted) {
                OutlinedButton(onClick = onSync, enabled = !syncing, shape = PillShape) {
                    Icon(Icons.Filled.Sync, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (syncing) "Reading messages…" else "Sync SMS")
                }
            } else {
                Button(onClick = sms.request, shape = PillShape) { Text("Allow SMS access") }
            }
            if (syncing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        status?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
