package com.abi.expensetracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.abi.expensetracker.ui.components.PermissionCard
import com.abi.expensetracker.ui.components.PermissionState

/**
 * The grants, asked for on the way in rather than found later in settings.
 *
 * An expense tracker with no permissions is an empty list, and an empty list is
 * indistinguishable from a broken app — so the ask happens before the user reaches a
 * ledger that could only ever be blank.
 *
 * It is one dialog with two independent asks, not a wizard: either grant is useful on its
 * own, and neither blocks the app. Declining is a real answer — [onDismiss] records it and
 * the dialog does not return, leaving the same two cards in Accounts for whenever the user
 * changes their mind.
 */
@Composable
fun StartupPermissionDialog(
    sms: PermissionState,
    notifications: PermissionState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = {
            Text(
                "Your spending, read from your own phone",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Nothing is uploaded. There is no account and no network — the app " +
                        "cannot reach one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PermissionCard(
                    title = "Read messages",
                    body = "Bank SMS already on this phone become your ledger, going " +
                        "back as far as your inbox does.",
                    state = sms,
                    actionLabel = "Allow messages"
                )
                PermissionCard(
                    title = "Read notifications",
                    body = "Banks and wallets that no longer send an SMS still post a " +
                        "notification. Only ones naming an amount are kept.",
                    state = notifications,
                    actionLabel = "Open settings",
                    footnote = "Android has no popup for this one — it is a switch in a " +
                        "system list."
                )
            }
        },
        // No confirm button: the cards are the actions. This is the way out, and it is
        // deliberately the plainest thing on the dialog rather than a dead-end.
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
}
