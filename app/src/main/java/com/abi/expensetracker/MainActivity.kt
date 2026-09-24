package com.abi.expensetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import com.abi.expensetracker.di.ServiceLocator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abi.expensetracker.data.SettingsStore
import com.abi.expensetracker.ui.Destination
import com.abi.expensetracker.ui.HomeScreen
import com.abi.expensetracker.ui.LedgerBottomBar
import com.abi.expensetracker.ui.components.AddFab
import com.abi.expensetracker.ui.AccountsScreen
import com.abi.expensetracker.ui.OnboardingScreen
import com.abi.expensetracker.ui.LoansScreen
import com.abi.expensetracker.ui.SettingsScreen
import com.abi.expensetracker.ui.TrendsScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import com.abi.expensetracker.ui.theme.AccentColor
import com.abi.expensetracker.ui.theme.ExpenseTrackerTheme
import com.abi.expensetracker.ui.theme.NeutralPalette
import com.abi.expensetracker.ui.theme.ThemeMode
import com.abi.expensetracker.ui.theme.accentPaletteFrom
import com.abi.expensetracker.ui.theme.palette
import com.abi.expensetracker.ui.theme.PillShape
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val settings = remember { SettingsStore(context) }
            val accentName by settings.accentName.collectAsStateWithLifecycle(initialValue = null)
            val customAccent by settings.customAccentArgb.collectAsStateWithLifecycle(initialValue = null)
            val themeModeName by settings.themeMode.collectAsStateWithLifecycle(initialValue = null)
            val neutralName by settings.neutralPalette.collectAsStateWithLifecycle(initialValue = null)

            val accent = if (accentName == SettingsStore.CUSTOM_ACCENT && customAccent != null) {
                accentPaletteFrom(Color(customAccent!!))
            } else {
                AccentColor.fromName(accentName).palette()
            }
            val darkTheme = when (ThemeMode.fromName(themeModeName)) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            ExpenseTrackerTheme(
                accent = accent,
                neutrals = NeutralPalette.fromName(neutralName),
                darkTheme = darkTheme
            ) {
                val tabs = Destination.entries
                val pagerState = rememberPagerState(pageCount = { tabs.size })
                val scope = rememberCoroutineScope()
                val current = tabs[pagerState.currentPage]
                // Owned here because the add button lives on the bar while the dialog
                // belongs to the ledger: the bar cannot reach into HomeScreen's state.
                var addOpen by remember { mutableStateOf(false) }
                /** Taps on the tab already open, per tab; each screen resets when its count rises. */
                val reselects = remember { mutableStateMapOf<Destination, Int>() }

                fun go(destination: Destination) {
                    val target = tabs.indexOf(destination)
                    scope.launch {
                        // Animating across several tabs composes every page in between;
                        // a jump of more than one tab switches instantly instead.
                        if (kotlin.math.abs(target - pagerState.currentPage) > 1) {
                            pagerState.scrollToPage(target)
                        } else {
                            pagerState.animateScrollToPage(target)
                        }
                    }
                }

                // Back returns to the ledger rather than leaving the app, which is what
                // the first tab being "home" implies.
                BackHandler(enabled = pagerState.currentPage != 0) {
                    scope.launch {
                        if (pagerState.currentPage > 1) pagerState.scrollToPage(0)
                        else pagerState.animateScrollToPage(0)
                    }
                }

                // First run gets the setup guide instead of the ledger. Null until DataStore
                // has answered, so neither screen flashes before the other.
                val onboardingDone by settings.onboardingDone
                    .collectAsStateWithLifecycle(initialValue = null)
                LaunchedEffect(onboardingDone) {
                    // An install that already has accounts was set up before the guide
                    // existed; it does not need walking through it.
                    if (onboardingDone == false &&
                        ServiceLocator.repository(context).bankCount() > 0
                    ) settings.setOnboardingDone(true)
                }
                when (onboardingDone) {
                    null -> {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                        return@ExpenseTrackerTheme
                    }
                    false -> {
                        OnboardingScreen(onFinish = { scope.launch { settings.setOnboardingDone(true) } })
                        return@ExpenseTrackerTheme
                    }
                    true -> Unit
                }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    // No insets of its own: each tab's top bar already pads for the status
                    // bar, and padding here too left a blank band above every title.
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        LedgerBottomBar(
                            selected = current,
                            // The tab already open resets to how it first opens; any other
                            // tab is simply shown, in whatever state it was left.
                            onSelect = { destination ->
                                if (destination == current) {
                                    reselects[destination] = (reselects[destination] ?: 0) + 1
                                } else go(destination)
                            }
                        )
                    },
                    floatingActionButton = {
                        // Only where it does something. On Templates or Settings it would be
                        // an action pointing at a list that is not on screen.
                        if (current == Destination.HOME) {
                            AddFab(onClick = { addOpen = true })
                        }
                    }
                ) { barPadding ->
                    // A pager rather than a NavHost: these four are siblings the user
                    // swipes between, not a stack with history. It also means the tab
                    // bar cannot disagree with what is on screen — both read currentPage.
                    HorizontalPager(
                        state = pagerState,
                        // The neighbours are composed ahead, so a swipe slides in a page
                        // that is already built instead of building it mid-gesture.
                        beyondViewportPageCount = 1,
                        // Consumed so the tabs' own scaffolds do not pad for the navigation
                        // bar again beneath the bottom bar that already covers it.
                        modifier = Modifier.padding(barPadding).consumeWindowInsets(barPadding),
                        key = { tabs[it].name }
                    ) { page ->
                        when (tabs[page]) {
                            Destination.HOME -> HomeScreen(
                                onOpenSettings = { go(Destination.SETTINGS) },
                                showAddDialog = addOpen,
                                onAddDialogClose = { addOpen = false },
                                resetSignal = reselects[Destination.HOME] ?: 0
                            )
                            Destination.TRENDS -> TrendsScreen(resetSignal = reselects[Destination.TRENDS] ?: 0)
                            Destination.LOANS -> LoansScreen(resetSignal = reselects[Destination.LOANS] ?: 0)
                            Destination.ACCOUNTS -> AccountsScreen(resetSignal = reselects[Destination.ACCOUNTS] ?: 0)
                            Destination.SETTINGS -> SettingsScreen(resetSignal = reselects[Destination.SETTINGS] ?: 0)
                        }
                    }
                }
            }
        }
    }
}
