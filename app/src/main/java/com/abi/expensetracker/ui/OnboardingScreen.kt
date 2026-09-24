package com.abi.expensetracker.ui

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.IntegrationInstructions
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abi.expensetracker.di.ServiceLocator
import com.abi.expensetracker.ui.components.LedgerCard
import com.abi.expensetracker.ui.components.PermissionCard
import com.abi.expensetracker.ui.components.SyncSmsControl
import com.abi.expensetracker.ui.components.rememberNotificationAccessState
import com.abi.expensetracker.ui.components.rememberSmsPermissionState
import com.abi.expensetracker.ui.theme.AppTheme
import com.abi.expensetracker.ui.theme.PillShape
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State the guide needs beyond what the screens it borrows already hold. */
class OnboardingViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ServiceLocator.repository(app)

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()
    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    val accountCount: StateFlow<Int> = repository.observeBanks().map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** The whole inbox, once: this is the first read, so there is no watermark yet. */
    fun sync() = viewModelScope.launch {
        _syncing.value = true
        _syncStatus.value = runCatching { repository.backfillFromInbox() }
            .fold(
                { "Read ${it.messagesRead} messages from your inbox." },
                { "Could not read messages: ${it.message}" }
            )
        _syncing.value = false
    }
}

private enum class Step { WELCOME, PERMISSIONS, SYNC, ACCOUNTS, DONE }

/**
 * First-run guide: welcome, permissions, read the inbox, set up accounts, done.
 *
 * Each step leads to the next, and every one can be skipped: either permission is useful
 * on its own, and an account can be added any time from the Accounts tab. The accounts step
 * is the real Accounts screen, so what the user sets up here is exactly what they will
 * manage later.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit, vm: OnboardingViewModel = viewModel()) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = Step.entries[index]
    val syncing by vm.syncing.collectAsStateWithLifecycle()
    val syncStatus by vm.syncStatus.collectAsStateWithLifecycle()
    val accounts by vm.accountCount.collectAsStateWithLifecycle()

    BackHandler(enabled = index > 0) { index-- }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            if (step != Step.WELCOME) {
                LinearProgressIndicator(
                    progress = { index / (Step.entries.size - 1f) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    drawStopIndicator = {}
                )
            }

            Box(Modifier.weight(1f)) {
                when (step) {
                    Step.WELCOME -> WelcomeStep()
                    Step.PERMISSIONS -> PermissionsStep()
                    Step.SYNC -> SyncStep(syncing, syncStatus, vm::sync)
                    Step.ACCOUNTS -> AccountsStep()
                    Step.DONE -> DoneStep(accounts)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (index > 0 && step != Step.DONE) {
                    TextButton(onClick = { index-- }) { Text("Back") }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { if (step == Step.DONE) onFinish() else index++ },
                    enabled = !syncing,
                    shape = PillShape
                ) {
                    Text(
                        when (step) {
                            Step.WELCOME -> "Get started"
                            Step.DONE -> "Open ledger"
                            else -> "Continue"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepHeader(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StepColumn(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) { content() }
}

@Composable
private fun WelcomeStep() {
    StepColumn {
        Spacer(Modifier.height(32.dp))
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
            Icon(
                Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(18.dp).size(36.dp)
            )
        }
        StepHeader(
            "Your spending, read from your own phone",
            "Expense Tracker turns the bank and wallet messages already on this phone into a " +
                "ledger. Setup takes a minute."
        )
        Point(Icons.Outlined.Sms, "Reads bank SMS and money notifications", "Only messages naming an amount are kept.")
        Point(Icons.Outlined.AccountBalance, "You link each sender to an account", "So the ledger shows \"Nabil\", not \"AX-NABIL\".")
        Point(Icons.Outlined.CloudOff, "Nothing leaves this phone", "The app has no internet permission at all.")
    }
}

@Composable
private fun Point(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(20.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PermissionsStep() {
    val sms = rememberSmsPermissionState()
    val notifications = rememberNotificationAccessState()
    StepColumn {
        StepHeader(
            "Let the app read your money messages",
            "Grant either or both. You can change this later in Settings › Permissions."
        )
        PermissionCard(
            title = "Read messages",
            body = "Bank SMS already on this phone become your ledger, going back as far as " +
                "your inbox does.",
            state = sms,
            actionLabel = "Allow messages"
        )
        PermissionCard(
            title = "Read notifications",
            body = "Banks and wallets that no longer send an SMS still post a notification. " +
                "Only ones naming an amount are kept.",
            state = notifications,
            actionLabel = "Open settings",
            footnote = "Android has no popup for this one — it is a switch in a system list."
        )
    }
}

@Composable
private fun SyncStep(syncing: Boolean, status: String?, onSync: () -> Unit) {
    StepColumn {
        StepHeader(
            "Read your inbox",
            "Brings in the bank messages already on your phone, so the next step can list the " +
                "senders to link. New messages are read automatically from now on."
        )
        LedgerCard {
            Box(Modifier.padding(16.dp)) {
                SyncSmsControl(syncing = syncing, status = status, onSync = onSync)
            }
        }
        Text(
            "Skipped SMS access? Continue — notifications still work, and you can sync later " +
                "from Accounts › Find a sender.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AccountsStep() {
    Column(Modifier.fillMaxSize()) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Set up your accounts", style = MaterialTheme.typography.titleMedium)
                Text(
                    "1. Tap Add for each bank or wallet.\n" +
                        "2. In its box, add the apps that notify you (e.g. the bank app, Gmail).\n" +
                        "3. Tap Find a sender to link each bank's SMS sender.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(Modifier.weight(1f)) { AccountsScreen() }
    }
}

@Composable
private fun DoneStep(accounts: Int) {
    StepColumn {
        Spacer(Modifier.height(24.dp))
        StepHeader(
            "You're set",
            if (accounts > 0) "$accounts " + (if (accounts == 1) "account" else "accounts") +
                " ready. Transactions land in the ledger as messages arrive."
            else "You can add accounts any time from the Accounts tab."
        )
        Text("Good to know", style = MaterialTheme.typography.titleMedium)
        Point(
            Icons.Outlined.IntegrationInstructions, "Messages the app could not read",
            "Settings › Parser templates lists them; tap one to teach the app its format."
        )
        Point(
            Icons.Outlined.Label, "Categories fill themselves",
            "Give a category a few keywords and matching transactions are filed automatically."
        )
        Point(
            Icons.Outlined.TouchApp, "Tap to edit, hold to select",
            "Tap a transaction to fix it, mark it as a loan or split it; hold to select and delete."
        )
        Text(
            "Nothing you set up here leaves this phone.",
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.finance.credit
        )
    }
}
