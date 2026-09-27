package com.abi.expensetracker.ui.components

import androidx.compose.ui.Alignment
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.Popup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import android.os.SystemClock
import android.view.WindowManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

/**
 * A dropdown that opens from its own control, at its own width, and filters as you type.
 *
 * The first tap opens the whole list with no keyboard, so every option is in view. A
 * second tap on the same field brings the keyboard up to filter it, and the list narrows
 * in place. Nothing moves: an earlier version showed search results inline, and the
 * dialog jumped up and down as they came and went.
 *
 * The list is a non-focusable popup. The Material exposed-dropdown popup took keyboard
 * focus, so typing into the field never arrived.
 *
 * Generic over the item so the same control serves senders, banks and anything else; a
 * nullable [T] lets a caller offer an "any"/"none" row as a real item rather than as a
 * special case inside here.
 */
@Composable
fun <T> SearchableDropdown(
    selectedLabel: String,
    items: List<T>,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Search",
    emptyText: String = "No match",
    /** When set, typing a name no item has offers a "Create" row that calls this. */
    onCreate: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val taps = remember { MutableInteractionSource() }
    var fieldWidth by remember { mutableIntStateOf(0) }
    /** Pixels between the field and the bottom of its window, as the popup placement sees it. */
    var roomBelow by remember { mutableIntStateOf(0) }
    val navBar = WindowInsets.navigationBars.getBottom(LocalDensity.current)
    val placement = remember { BelowAnchor { roomBelow = it } }
    val scope = rememberCoroutineScope()
    /** When the field was last pressed; a tap on it must not count as "outside the list". */
    var fieldPressedAt by remember { mutableLongStateOf(0L) }

    fun close() {
        expanded = false
        searching = false
        query = ""
        focusManager.clearFocus()
    }

    // First tap: the list. Second tap while it is open: the keyboard, to filter it.
    LaunchedEffect(taps, enabled) {
        taps.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) fieldPressedAt = SystemClock.uptimeMillis()
            if (interaction is PressInteraction.Release && enabled) {
                if (!expanded) {
                    expanded = true
                } else if (!searching) {
                    searching = true
                }
            }
        }
    }
    LaunchedEffect(searching) { if (searching) focus.requestFocus() }

    val matches = remember(items, query) {
        val text = query.trim()
        if (text.isEmpty()) items
        else items.filter { itemLabel(it).contains(text, ignoreCase = true) }
    }
    val typed = query.trim()
    val offerCreate = onCreate != null && typed.isNotEmpty() &&
        items.none { itemLabel(it).equals(typed, ignoreCase = true) }

    Box(modifier) {
        CompactTextField(
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            // Closed, it states the choice; searching, it is the search box.
            value = if (searching) query else selectedLabel,
            onValueChange = { query = it },
            enabled = enabled,
            readOnly = !searching,
            placeholder = placeholder,
            leadingIcon = if (searching) {
                { Icon(Icons.Default.Search, contentDescription = null) }
            } else null,
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            },
            interactionSource = taps,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .onGloballyPositioned { fieldWidth = it.size.width }
        )

        if (expanded) {
            Popup(
                popupPositionProvider = placement,
                onDismissRequest = {
                    // A tap outside closes it, except a tap on the field itself, which
                    // reaches the list as an outside touch too and would otherwise shut
                    // it on the second tap.
                    scope.launch {
                        delay(150)
                        if (SystemClock.uptimeMillis() - fieldPressedAt > 400) close()
                    }
                },
                // Not focusable, so the keyboard's typing reaches the field. A
                // non-focusable window sits above the keyboard by default and covered its
                // keys; ALT_FOCUSABLE_IM puts it behind. NO_LIMITS lets it run on under
                // the keyboard instead of being shifted up over the field.
                properties = PopupProperties(
                    flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
            ) {
                // The form's own background, set apart only by an accent border: without
                // one the list had no edge and read as part of the form behind it.
                // Unfolds from the field rather than appearing all at once.
                val shown = remember { MutableTransitionState(false) }.apply { targetState = true }
                AnimatedVisibility(
                    visibleState = shown,
                    enter = fadeIn(tween(150)) + expandVertically(tween(180), expandFrom = Alignment.Top)
                ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.width(with(LocalDensity.current) { fieldWidth.toDp() })
                ) {
                    // No taller than the screen below the field: a fixed 320dp ran off the
                    // bottom of the screen and cut the last options. The keyboard, when up,
                    // is allowed to cover it; the screen edge is not.
                    val room = with(LocalDensity.current) { (roomBelow - navBar).toDp() - 16.dp }
                    Column(
                        Modifier.heightIn(
                            max = if (roomBelow == 0) 320.dp else minOf(320.dp, room.coerceAtLeast(96.dp))
                        )
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
                        if (matches.isEmpty() && !offerCreate) {
                            Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(
                                    emptyText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        matches.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(itemLabel(item)) },
                                onClick = {
                                    onSelect(item)
                                    close()
                                }
                            )
                        }
                        // After the matches: the strip above the keyboard shows the
                        // closest existing item first, and creating is the fallback.
                        if (offerCreate) {
                            DropdownMenuItem(
                                text = { Text("Create \u201c$typed\u201d", color = MaterialTheme.colorScheme.primary) },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    onCreate?.invoke(typed)
                                    close()
                                }
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

/**
 * Always directly under the field, left edges aligned: never flipped or shifted over it.
 * Reports the room left below the field, which only placement knows exactly (the window
 * a dialog's field lives in is not the size the field itself can see), so the list can
 * stop at the screen's edge.
 */
private class BelowAnchor(private val onRoom: (Int) -> Unit) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        onRoom(windowSize.height - anchorBounds.bottom)
        return IntOffset(anchorBounds.left, anchorBounds.bottom)
    }
}
