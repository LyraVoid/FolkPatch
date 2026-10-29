package me.bmax.apatch.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.bmax.apatch.core.ui.R
import me.bmax.apatch.ui.component.folk.TopBarBackButton
import me.bmax.apatch.ui.component.folk.folkDefaultAppBarColors

/**
 * Compact M3E app bar whose search action opens the contained full-screen search surface.
 *
 * Empty queries render [defaultContent]; typed queries render [searchContent]. The search text is
 * cleared when the surface collapses so reopening it starts from the shortcuts again.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3eSearchAppBar(
    title: @Composable () -> Unit,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    searchPlaceholder: String,
    actions: @Composable RowScope.() -> Unit = {},
    defaultContent: @Composable BoxScope.(bottomPadding: Dp, closeSearch: () -> Unit) -> Unit,
    searchContent: @Composable BoxScope.(bottomPadding: Dp, closeSearch: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val searchBarState = rememberContainedSearchBarState()
    val textFieldState = rememberTextFieldState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val latestSearchText by rememberUpdatedState(searchText)
    val currentQuery = textFieldState.text.toString()
    val isExpanded =
        searchBarState.currentValue != SearchBarValue.Collapsed ||
            searchBarState.targetValue != SearchBarValue.Collapsed
    var previousSearchBarValue by remember { mutableStateOf(searchBarState.currentValue) }

    fun hideSearchChrome() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    fun clearSearchText() {
        textFieldState.setTextAndPlaceCursorAtEnd("")
        onSearchTextChange("")
    }

    fun closeSearch(clearQuery: Boolean = true) {
        if (clearQuery) clearSearchText()
        scope.launch { searchBarState.animateToCollapsed() }
        hideSearchChrome()
    }

    LaunchedEffect(searchText) {
        if (textFieldState.text.toString() != searchText) {
            textFieldState.setTextAndPlaceCursorAtEnd(searchText)
        }
    }
    LaunchedEffect(textFieldState, latestSearchText) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { value ->
                if (value != latestSearchText) onSearchTextChange(value)
            }
    }
    LaunchedEffect(searchBarState) {
        snapshotFlow { searchBarState.currentValue }
            .distinctUntilChanged()
            .collect { value ->
                val collapsedFromExpanded =
                    previousSearchBarValue != SearchBarValue.Collapsed &&
                        value == SearchBarValue.Collapsed
                previousSearchBarValue = value
                if (collapsedFromExpanded) {
                    clearSearchText()
                    hideSearchChrome()
                }
            }
    }
    DisposableEffect(Unit) {
        onDispose { keyboardController?.hide() }
    }
    BackHandler(enabled = isExpanded) { closeSearch() }

    val inputField: @Composable () -> Unit = {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = { hideSearchChrome() },
            placeholder = { Text(searchPlaceholder) },
            leadingIcon = {
                if (isExpanded) {
                    TopBarBackButton(
                        horizontalPadding = 0.dp,
                        onClick = { closeSearch() },
                        contentDescription = stringResource(R.string.core_action_back),
                    )
                } else {
                    Icon(Icons.Filled.Search, contentDescription = null)
                }
            },
            trailingIcon = {
                if (isExpanded && currentQuery.isNotEmpty()) {
                    IconButton(onClick = { clearSearchText() }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.core_action_clear),
                        )
                    }
                }
            },
        )
    }

    TopAppBar(
        title = title,
        colors = folkDefaultAppBarColors(),
        actions = {
            IconButton(onClick = { scope.launch { searchBarState.animateToExpanded() } }) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = stringResource(R.string.core_action_search),
                )
            }
            actions()
        },
    )

    ExpandedFullScreenContainedSearchBar(
        state = searchBarState,
        inputField = inputField,
        windowInsets = {
            SearchBarDefaults.fullScreenWindowInsets.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
            )
        },
        content = {
            val bottomPadding =
                SearchBarDefaults.fullScreenWindowInsets.asPaddingValues().calculateBottomPadding()
            Box(modifier = Modifier.fillMaxSize()) {
                if (currentQuery.isNotEmpty()) {
                    searchContent(bottomPadding) { closeSearch() }
                } else {
                    defaultContent(bottomPadding) { closeSearch() }
                }
            }
        },
    )
}
