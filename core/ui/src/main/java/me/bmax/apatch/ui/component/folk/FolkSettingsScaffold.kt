package me.bmax.apatch.ui.component.folk

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import me.bmax.apatch.core.ui.R
import me.bmax.apatch.ui.theme.tokens.FolkTheme
import me.bmax.apatch.util.ui.NavigationBarsSpacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box

/**
 * Colours for the settings bar.
 *
 * With a custom background the bar is fully transparent so the image reads
 * straight through it on every page.
 *
 * Otherwise the bar fades from fully transparent to the elevated panel tone.
 * Both ends use the *same* RGB with only the alpha changing - using
 * [Color.Transparent] instead would make Material interpolate the colour from
 * black, which showed up as a grey scrim washing over the title and the
 * content while scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun folkTopAppBarColors(): TopAppBarColors {
    if (FolkTheme.palette.onCustomBackground) {
        return TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        )
    }
    val elevated = folkGroupColor()
    return TopAppBarDefaults.topAppBarColors(
        containerColor = elevated.copy(alpha = 0f),
        scrolledContainerColor = elevated,
    )
}

/**
 * Colours for the screens that build their own bar (home, modules, search):
 * the Material defaults, except that a custom background reads straight
 * through the bar on every page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun folkDefaultAppBarColors(): TopAppBarColors =
    if (FolkTheme.palette.onCustomBackground) {
        TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        )
    } else {
        TopAppBarDefaults.topAppBarColors()
    }

/**
 * Shared chrome for every settings sub-screen.
 *
 * Uses the M3E flexible title so the page starts with a clear heading and collapses to the compact
 * height while scrolling. All settings category pages share this chrome.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FolkSettingsScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = folkTopAppBarColors(),
                navigationIcon = {
                    if (onBack != null) {
                        TopBarBackButton(
                            onClick = onBack,
                            contentDescription = stringResource(R.string.core_action_back),
                        )
                    }
                },
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = Color.Transparent,
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(snackbarHostState)
            }
        },
    ) { innerPadding ->
        // Keep the list viewport below the bar instead of only offsetting the
        // first item: with a translucent bar in wallpaper mode, content that
        // scrolls underneath would show through the title.
        // Cap the list width on large screens so rows stay readable.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = FolkSettingsDimens.ContentMaxWidth)
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                contentPadding = PaddingValues(
                    bottom = innerPadding.calculateBottomPadding() + FolkSettingsDimens.ScreenPadding,
                ),
            ) {
                content()
                item(key = "folk_bottom") {
                    NavigationBarsSpacer()
                }
            }
        }
    }
}
