package me.bmax.apatch.ui.component.folk

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.bmax.apatch.core.ui.R
import me.bmax.apatch.ui.navigation.LocalBottomBarVisible
import me.bmax.apatch.ui.navigation.LocalIsFloatingNavMode
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment

/**
 * How a [FolkScaffold] presents its title.
 *
 * [Inline] is the default on purpose: a collapsible [Large] title reads wrong on
 * a tab page (that combination caused a rework once already), so making the
 * large title an explicit opt-in keeps the mistake from happening by accident.
 *
 * [Flexible] is the Material 3 Expressive bar for content pages: it starts tall,
 * carries an optional subtitle, and collapses to the inline height on scroll.
 */
enum class FolkTitleStyle { Large, Flexible, Inline, None }

/**
 * The shared chrome for a screen: title, back button, actions, snackbar and the
 * insets that keep content clear of the status bar and the floating bottom bar.
 *
 * The content slot receives the padding to apply; it stays a plain lambda rather
 * than a `LazyListScope` because the screens are not all lists - a settings page
 * is a `LazyColumn`, the theme picker is a staggered grid and the audit log puts
 * a `TabRow` between the bar and the list. A shared "padding + chrome" contract
 * covers all of them without reshaping any of them.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FolkScaffold(
    title: String = "",
    titleStyle: FolkTitleStyle = FolkTitleStyle.Inline,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    titleContent: (@Composable () -> Unit)? = null,
    /** Optional supporting line under a [FolkTitleStyle.Flexible] title. */
    subtitle: String? = null,
    floatingActionButton: @Composable () -> Unit = {},
    /**
     * A persistent bottom bar, such as a command input. Callers that use this
     * should usually pass `addBottomClearance = false`, because the bar already
     * contributes to the content padding.
     */
    bottomBar: (@Composable () -> Unit)? = null,
    /**
     * A fully custom bar. Screens whose bar is not a plain title (an animated
     * search field, a selection bar) supply it here so they still get the shared
     * insets and bottom clearance without reshaping their bar.
     */
    topBar: (@Composable () -> Unit)? = null,
    /**
     * Set false when the content already keeps its own bottom clearance (e.g. a
     * list using `fabNavBottomClearance`), so the space is not reserved twice.
     */
    addBottomClearance: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        // Only the collapsible built-in bar consumes scroll; the others need no
        // connection.
        modifier = if (topBar == null && (titleStyle == FolkTitleStyle.Large || titleStyle == FolkTitleStyle.Flexible)) {
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            Modifier
        },
        topBar = {
            FolkWallpaperChromeZone(edge = FolkChromeEdge.Top) {
                val custom = topBar
                if (custom != null) {
                    custom()
                } else {
                    when (titleStyle) {
                    FolkTitleStyle.Large -> LargeTopAppBar(
                        title = {
                            // A caller-supplied title (e.g. a search field)
                            // replaces the plain label entirely.
                            if (titleContent != null) {
                                titleContent()
                            } else {
                                Text(text = title, fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = folkTopAppBarColors(),
                        navigationIcon = { FolkBackButton(onBack) },
                        actions = actions,
                        scrollBehavior = scrollBehavior,
                    )

                    FolkTitleStyle.Flexible -> LargeFlexibleTopAppBar(
                        title = {
                            if (titleContent != null) {
                                titleContent()
                            } else {
                                Text(text = title, fontWeight = FontWeight.Bold)
                            }
                        },
                        subtitle = if (subtitle != null) {
                            { Text(text = subtitle) }
                        } else {
                            null
                        },
                        colors = folkTopAppBarColors(),
                        navigationIcon = { FolkBackButton(onBack) },
                        actions = actions,
                        scrollBehavior = scrollBehavior,
                    )

                    FolkTitleStyle.Inline -> TopAppBar(
                        title = {
                            if (titleContent != null) {
                                titleContent()
                            } else {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        },
                        colors = folkTopAppBarColors(),
                        navigationIcon = { FolkBackButton(onBack) },
                        actions = actions,
                    )

                    FolkTitleStyle.None -> Unit
                    }
                }
            }
        },
        containerColor = Color.Transparent,
        // The container is transparent so the themed background shows through.
        // Material would then derive the content colour from a transparent
        // container and get Unspecified, which drops any text that does not set
        // its own colour to black - unreadable in dark mode. Pin it to the
        // background's content colour, which is what an opaque page would use.
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(snackbarHostState)
            }
        },
        floatingActionButton = floatingActionButton,
        bottomBar = {
            val custom = bottomBar
            if (custom != null) {
                FolkWallpaperChromeZone(edge = FolkChromeEdge.Bottom) {
                    custom()
                }
            }
        },
    ) { inner ->
        val layoutDirection = LocalLayoutDirection.current
        val clearance = if (addBottomClearance) folkBottomClearance() else 0.dp
        // Keep the content viewport below the bar instead of only offsetting the
        // first item. With a translucent bar in wallpaper mode, content that
        // scrolls underneath would otherwise show through the title.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = inner.calculateTopPadding()),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Cap the content column on large screens so rows do not stretch
            // across a tablet or desktop window; on a phone this is a no-op.
            Box(
                modifier = Modifier
                    .widthIn(max = FolkSettingsDimens.ContentMaxWidth)
                    .fillMaxSize(),
            ) {
                content(
                    PaddingValues(
                        start = inner.calculateStartPadding(layoutDirection),
                        end = inner.calculateEndPadding(layoutDirection),
                        top = 0.dp,
                        bottom = inner.calculateBottomPadding() + clearance,
                    )
                )
            }
        }
    }
}

@Composable
private fun FolkBackButton(onBack: (() -> Unit)?) {
    if (onBack != null) {
        TopBarBackButton(
            onClick = onBack,
            contentDescription = stringResource(R.string.core_action_back),
        )
    }
}

/**
 * Clearance under the last item so it can be scrolled above the floating bottom
 * bar.
 *
 * The overlay only exists on main-tab routes; on a detail page the app hides
 * the bar in both floating and docked mode, and the docked shell pads the nav
 * host only on its main-tab routes. In those cases a small gap under the
 * content is all that is needed, and adding the floating height there would
 * just be dead scroll space.
 */
@Composable
private fun folkBottomClearance(): Dp {
    val floating = LocalIsFloatingNavMode.current
    val barVisible = LocalBottomBarVisible.current.value
    return animateDpAsState(
        targetValue = if (floating && barVisible) FloatingBarClearance else StaticBarClearance,
        label = "folkBottomClearance",
    ).value
}

private val FloatingBarClearance = 80.dp
private val StaticBarClearance = 16.dp
