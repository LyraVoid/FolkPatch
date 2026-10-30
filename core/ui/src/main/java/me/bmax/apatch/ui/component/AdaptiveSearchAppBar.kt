package me.bmax.apatch.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.bmax.apatch.core.ui.R
import me.bmax.apatch.ui.component.folk.FolkAnimatedSlot
import me.bmax.apatch.ui.component.folk.TopBarBackButton
import me.bmax.apatch.ui.component.folk.folkDefaultAppBarColors
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.theme.tokens.FolkTheme
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AdaptiveSearchAppBar(
    title: @Composable () -> Unit,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearClick: () -> Unit,
    searchPlaceholder: String,
    actions: @Composable RowScope.() -> Unit,
    navigationIcon: (@Composable () -> Unit)?,
    snackbarHostState: SnackbarHostState?,
    scrollBehavior: TopAppBarScrollBehavior?,
    largeTitle: Boolean,
    topSearch: Boolean,
    collapsibleSearch: Boolean,
    defaultContent: @Composable BoxScope.(bottomPadding: Dp, closeSearch: () -> Unit) -> Unit,
    searchContent: @Composable BoxScope.(bottomPadding: Dp, closeSearch: () -> Unit) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    val searchBarState = rememberContainedSearchBarState()
    val textFieldState = rememberTextFieldState()
    val currentQuery = textFieldState.text.toString()
    val latestSearchText by rememberUpdatedState(searchText)
    val isSearchExpanded =
        searchBarState.currentValue != SearchBarValue.Collapsed ||
            searchBarState.targetValue != SearchBarValue.Collapsed
    val searchScrollBehavior =
        (scrollBehavior as? SearchAppBarScrollBehavior)?.takeIf { collapsibleSearch && largeTitle }
    val searchBarFraction = searchScrollBehavior?.searchBarExpandedFraction ?: 1f
    val isSearchBarCollapsed = searchBarFraction <= 0.01f
    val isSearchBarCollapsing = searchScrollBehavior != null && searchBarFraction < 0.99f
    var previousSearchBarValue by remember { mutableStateOf(searchBarState.currentValue) }
    var shouldClearOnCollapse by remember { mutableStateOf(true) }

    fun hideSearchChrome() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    fun clearSearchText() {
        textFieldState.setTextAndPlaceCursorAtEnd("")
        onClearClick()
    }

    fun closeSearch(clearQuery: Boolean = true) {
        if (clearQuery) clearSearchText()
        shouldClearOnCollapse = false
        scope.launch { searchBarState.animateToCollapsed() }
        hideSearchChrome()
    }

    LaunchedEffect(isSearchBarCollapsing) {
        if (isSearchBarCollapsing) hideSearchChrome()
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
                    if (shouldClearOnCollapse) clearSearchText()
                    shouldClearOnCollapse = true
                    hideSearchChrome()
                }
            }
    }
    DisposableEffect(Unit) {
        onDispose { keyboardController?.hide() }
    }
    BackHandler(enabled = isSearchExpanded) { closeSearch() }

    val searchIcon: @Composable () -> Unit = {
        if (topSearch) {
            IconButton(onClick = { scope.launch { searchBarState.animateToExpanded() } }) {
                Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.core_action_search))
            }
        }
    }
    val appBarActions: @Composable RowScope.() -> Unit = {
        actions()
        searchIcon()
        if (searchScrollBehavior != null) {
            FolkAnimatedSlot(visible = isSearchBarCollapsed) {
                IconButton(onClick = { scope.launch { searchScrollBehavior.expandSearchBar() } }) {
                    Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.core_action_search))
                }
            }
        }
    }
    val inputField: @Composable () -> Unit = {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = { hideSearchChrome() },
            placeholder = {
                if (searchPlaceholder.isNotEmpty()) Text(searchPlaceholder)
            },
            leadingIcon = {
                if (isSearchExpanded) {
                    TopBarBackButton(
                        horizontalPadding = FolkDimens.Size0,
                        onClick = { closeSearch() },
                        contentDescription = stringResource(R.string.core_action_back),
                    )
                } else {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                }
            },
            trailingIcon = {
                if (isSearchExpanded && currentQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { clearSearchText() },
                        colors = IconButtonDefaults.iconButtonColors(),
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.core_action_clear))
                    }
                }
            },
            interactionSource = interactionSource,
        )
    }
    val wallpaperTokens = LocalFolkWallpaperTokens.current
    val searchBackground = wallpaperTokens?.raisedOverChrome() ?: FolkTheme.palette.groupedInset

    Surface(
        color = FolkTheme.palette.groupedBackground,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (largeTitle) {
                LargeFlexibleTopAppBar(
                    title = title,
                    colors = folkDefaultAppBarColors(),
                    navigationIcon = { navigationIcon?.invoke() },
                    actions = appBarActions,
                    windowInsets = WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    ),
                    scrollBehavior = scrollBehavior,
                )
            } else {
                TopAppBar(
                    title = title,
                    colors = folkDefaultAppBarColors(),
                    navigationIcon = { navigationIcon?.invoke() },
                    actions = appBarActions,
                    windowInsets = WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    ),
                )
            }

            val largeSearchBar: @Composable () -> Unit = {
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                        )
                        .padding(horizontal = FolkDimens.Size16)
                        .padding(bottom = FolkDimens.Size13),
                    state = searchBarState,
                    inputField = inputField,
                    colors = SearchBarDefaults.colors(containerColor = searchBackground),
                )
            }
            when {
                searchScrollBehavior != null -> AnimatedVisibility(
                    visible = !isSearchBarCollapsed,
                    enter = EnterTransition.None,
                    exit = ExitTransition.None,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(searchBarFraction)
                        .collapseWithTopAppBar(searchBarFraction),
                ) {
                    largeSearchBar()
                }

                !topSearch -> largeSearchBar()
            }
        }
    }

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
                snackbarHostState?.let { hostState ->
                    SnackbarHost(
                        hostState = hostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(bottom = FolkDimens.Size16),
                    )
                }
            }
        },
    )
}
