package com.abi.expensetracker.ui

import android.content.Context
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.abi.expensetracker.ui.theme.PillShape

/**
 * The optional lock in front of the app's screens.
 *
 * It locks the screens, not the work: SMS and notifications are still read and booked,
 * the weekly backup still runs and "What was it for?" can still be answered from the
 * shade, because none of those open the app. Fingerprint or face where the phone has
 * them, with the phone's own PIN or pattern as the fallback, so a failed sensor never
 * locks the user out of their own data.
 */
object AppLock {

    /** Weak biometrics as well as strong, plus the device credential as the fallback. */
    private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    /**
     * How long the app may sit in the background before it asks again. Long enough that
     * checking a bank app or answering a message and coming straight back does not ask,
     * short enough that a phone left unlocked on a table does not show the ledger.
     */
    private const val GRACE_MILLIS = 60_000L

    /**
     * Whether the screens are locked right now. Process-wide, so a new process (the phone
     * restarted, the app was killed) starts locked, and Compose state, so unlocking from
     * the Settings switch is seen by the activity at once.
     */
    var locked by mutableStateOf(true)
        private set
    private var backgroundedAt: Long? = null

    /** Whether the phone has something to unlock with: a screen lock at the very least. */
    fun available(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    fun onBackground() {
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    /** Called as the app comes back to the front: locks again after the grace period. */
    fun onForeground() {
        val since = backgroundedAt
        if (since != null && SystemClock.elapsedRealtime() - since > GRACE_MILLIS) locked = true
        backgroundedAt = null
    }

    fun markUnlocked() {
        locked = false
    }

    /** Asks for the fingerprint, face or screen lock; [onResult] gets whether it passed. */
    fun prompt(activity: FragmentActivity, title: String, onResult: (Boolean) -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onResult(true)
                }

                // A single unrecognised finger is not a failure: the prompt stays up for
                // another try. Only an error (cancelled, too many tries) ends it.
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onResult(false)
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()
        )
    }
}

/** What shows instead of the app while it is locked: nothing of the ledger behind it. */
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text("Expense tracker is locked", style = MaterialTheme.typography.titleLarge)
            Text(
                "Use your fingerprint, face or screen lock to open it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onUnlock, shape = PillShape) { Text("Unlock") }
        }
    }
}
