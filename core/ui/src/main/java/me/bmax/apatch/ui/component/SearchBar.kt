package me.bmax.apatch.ui.component

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import me.bmax.apatch.ui.component.folk.folkDefaultAppBarColors
import me.bmax.apatch.ui.component.folk.TopBarBackButton
import me.bmax.apatch.core.ui.R
import androidx.compose.ui.res.stringResource
import me.bmax.apatch.ui.theme.tokens.FolkShape
import me.bmax.apatch.ui.theme.tokens.FolkTheme
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens

private const val TAG = "SearchBar"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAppBar(
    title: @Composable () -> Unit,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearClick: () -> Unit = {},
    searchPlaceholder: String = "",
    actions: @Composable RowScope.() -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    onConfirm: (() -> Unit)? = null,
    dropdownContent: @Composable (() -> Unit)? = null,
    leadingActions: @Composable (() -> Unit)? = null,
    trailingActions: @Composable (() -> Unit)? = null,
    startInSearchMode: Boolean = false,
    navigationIcon: (@Composable () -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    largeTitle: Boolean = false,
    topSearch: Boolean = false,
    collapsibleSearch: Boolean = false,
    defaultContent: (@Composable BoxScope.(bottomPadding: Dp, closeSearch: () -> Unit) -> Unit)? = null,
    searchContent: (@Composable BoxScope.(bottomPadding: Dp, closeSearch: () -> Unit) -> Unit)? = null,
) {
    if (defaultContent != null || searchContent != null || largeTitle || topSearch ||
        collapsibleSearch || scrollBehavior != null
    ) {
        AdaptiveSearchAppBar(
            title = title,
            searchText = searchText,
            onSearchTextChange = onSearchTextChange,
            onClearClick = onClearClick,
            searchPlaceholder = searchPlaceholder,
            actions = {
                leadingActions?.invoke()
                actions()
                dropdownContent?.invoke()
                trailingActions?.invoke()
            },
            navigationIcon = navigationIcon ?: onBackClick?.let { backClick ->
                {
                    TopBarBackButton(
                        onClick = backClick,
                        contentDescription = stringResource(R.string.core_action_back),
                    )
                }
            },
            snackbarHostState = snackbarHostState,
            scrollBehavior = scrollBehavior,
            largeTitle = largeTitle,
            topSearch = topSearch || (!largeTitle && (defaultContent != null || searchContent != null)),
            collapsibleSearch = collapsibleSearch,
            defaultContent = defaultContent ?: { _, _ -> },
            searchContent = searchContent ?: { _, _ -> },
        )
        return
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var onSearch by rememberSaveable { mutableStateOf(startInSearchMode) }

    // 使用 TextFieldValue 管理文本+光标位置，避免页面导航返回后光标跳到文本开头
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(searchText, TextRange(searchText.length)))
    }
    LaunchedEffect(searchText) {
        if (textFieldValue.text != searchText) {
            textFieldValue = TextFieldValue(searchText, TextRange(searchText.length))
        }
    }

    if (onSearch) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    BackHandler(
        enabled = onSearch,
        onBack = {
            keyboardController?.hide()
            onClearClick()
            onSearch = !onSearch
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
        }
    }

    TopAppBar(
        colors = folkDefaultAppBarColors(),
        title = {
            Box {
                AnimatedVisibility(
                    modifier = Modifier.align(Alignment.CenterStart),
                    visible = !onSearch,
                    enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                    exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
                    content = { title() }
                )

                AnimatedVisibility(
                    visible = onSearch,
                    enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                    exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
                ) {
                    val wallpaper = LocalFolkWallpaperTokens.current
                    val searchFill = wallpaper?.raisedOverChrome() ?: FolkTheme.palette.groupedInset
                    val searchFieldColors = TextFieldDefaults.colors(
                        focusedContainerColor = searchFill,
                        unfocusedContainerColor = searchFill,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    )
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 2.dp,
                                bottom = 2.dp,
                                end = if (onBackClick != null) 0.dp else 16.dp
                            )
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) onSearch = true
                                Log.d(TAG, "onFocusChanged: $focusState")
                            },
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            onSearchTextChange(newValue.text)
                        },
                        shape = FolkShape.CornerFull,
                        colors = searchFieldColors,
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        },
                        placeholder = { Text(stringResource(R.string.core_action_search)) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    onSearch = false
                                    keyboardController?.hide()
                                    onClearClick()
                                },
                                content = { Icon(Icons.Outlined.Close, stringResource(R.string.core_action_clear)) }
                            )
                        },
                        maxLines = 1,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions {
                            defaultKeyboardAction(ImeAction.Search)
                            keyboardController?.hide()
                            onConfirm?.invoke()
                        },
                    )
                }
            }
        },
        navigationIcon = {
            if (onBackClick != null) {
                TopBarBackButton(
                    onClick = onBackClick,
                    contentDescription = stringResource(R.string.core_action_back),
                )
            }
        },
        actions = {
            AnimatedVisibility(
                visible = !onSearch
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leadingActions?.invoke()
                    IconButton(
                        onClick = { onSearch = true },
                        content = { Icon(Icons.Outlined.Search, stringResource(R.string.core_action_search)) }
                    )
                }
            }

            dropdownContent?.invoke()
            trailingActions?.invoke()

        }
    )
}

@Preview
@Composable
private fun SearchAppBarPreview() {
    var searchText by remember { mutableStateOf("") }
    SearchAppBar(
        title = { Text("Search text") },
        searchText = searchText,
        onSearchTextChange = { searchText = it },
        onClearClick = { searchText = "" }
    )
}
