package me.bmax.apatch.ui.screen.module

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.BackHandler
import com.ramcosta.composedestinations.generated.destinations.OnlineKPMScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import me.bmax.apatch.ui.component.folk.folkDefaultAppBarColors
import me.bmax.apatch.core.ui.R as CoreR
import me.bmax.apatch.ui.theme.tokens.FolkShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KPMTopBar(
    navigator: DestinationsNavigator,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var onSearch by remember { mutableStateOf(false) }

    if (onSearch) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    BackHandler(
        enabled = onSearch,
        onBack = {
            keyboardController?.hide()
            onSearchQueryChange("")
            onSearch = false
        }
    )

    TopAppBar(
        colors = folkDefaultAppBarColors(),
        title = {
            Box {
                // 标题（搜索框未显示时）
                AnimatedVisibility(
                    modifier = Modifier.align(Alignment.CenterStart),
                    visible = !onSearch,
                    enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                    exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
                    content = { Text(stringResource(R.string.kpm)) }
                )

                // 搜索框（搜索时显示）
                AnimatedVisibility(
                    visible = onSearch,
                    enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                    exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = FolkDimens.Size2, bottom = FolkDimens.Size2, end = FolkDimens.Size16)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) onSearch = true
                            },
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        shape = FolkShape.Corner16,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    onSearch = false
                                    keyboardController?.hide()
                                    onSearchQueryChange("")
                                },
                                content = { Icon(Icons.Outlined.Close, stringResource(CoreR.string.core_action_close)) }
                            )
                        },
                        maxLines = 1,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions {
                            keyboardController?.hide()
                        },
                    )
                }
            }
        },
        actions = {
            AnimatedVisibility(
                visible = !onSearch
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 搜索按钮
                    IconButton(onClick = { onSearch = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = stringResource(CoreR.string.core_action_search)
                        )
                    }
                    // 下载按钮
                    IconButton(onClick = dropUnlessResumed { navigator.navigate(OnlineKPMScreenDestination) }) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = stringResource(R.string.online_kpm_title)
                        )
                    }
                }
            }
        }
    )
}



