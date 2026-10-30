package me.bmax.apatch.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens

/**
 * The plain docked bottom bar (nav_mode = "bottom"). It uses a Material3
 * [NavigationBar], but replaces the default pill indicator with a rounded-rect
 * block plus a small spring pop, and keeps every label visible.
 */
@Composable
internal fun PlainBottomNavigationBar(
    visibleDestinations: List<BottomBarDestination>,
    effectiveSelectedIndex: Int,
    containerColor: Color,
    superuserCount: Int,
    apmModuleCount: Int,
    kernelModuleCount: Int,
    enableSuperUserBadge: Boolean,
    enableApmBadge: Boolean,
    enableKernelBadge: Boolean,
    navController: NavHostController,
    onUserInteraction: (() -> Unit)?
) {
    val context = LocalContext.current
    val navigator = navController.rememberDestinationsNavigator()
    val wallpaper = LocalFolkWallpaperTokens.current

    NavigationBar(
        tonalElevation = if (BackgroundConfig.isCustomBackgroundEnabled) 0.dp else 8.dp,
        containerColor = containerColor
    ) {
        visibleDestinations.forEachIndexed { index, destination ->
            key(destination) {
                val isCurrentDestOnBackStack by navController.isRouteOnBackStackAsState(destination.direction)
                val isSelected = index == effectiveSelectedIndex

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        onUserInteraction?.invoke()
                        if (me.bmax.apatch.ui.theme.SoundEffectConfig.scope == me.bmax.apatch.ui.theme.SoundEffectConfig.SCOPE_BOTTOM_BAR) {
                            me.bmax.apatch.util.SoundEffectManager.play(context)
                        }
                        if (me.bmax.apatch.ui.theme.VibrationConfig.scope == me.bmax.apatch.ui.theme.VibrationConfig.SCOPE_BOTTOM_BAR) {
                            me.bmax.apatch.util.VibrationManager.vibrate(context)
                        }
                        if (isCurrentDestOnBackStack) {
                            navigator.popBackStack(destination.direction, false)
                        }
                        navigator.navigate(destination.direction) {
                            popUpTo(NavGraphs.root) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    ),
                    icon = {
                        val badgeContent = when {
                            destination == BottomBarDestination.SuperUser && enableSuperUserBadge -> superuserCount
                            destination == BottomBarDestination.AModule && enableApmBadge -> apmModuleCount
                            destination == BottomBarDestination.KModule && enableKernelBadge -> kernelModuleCount
                            else -> 0
                        }

                        // Custom rounded-rect selection block. The M3 pill is made
                        // transparent above and we draw a softer "squircle" here with a
                        // small spring pop. Plain bar only, so floating/rail stay untouched.
                        val selectionProgress by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "plainBarSelection"
                        )
                        val indicatorColor = if (wallpaper != null) {
                            Color.Transparent
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }

                        BadgedBox(
                            modifier = Modifier.drawBehind {
                                val alpha = selectionProgress.coerceIn(0f, 1f)
                                if (alpha > 0.001f) {
                                    val pop = selectionProgress.coerceIn(0f, 1.3f)
                                    val width = 56.dp.toPx() * (0.72f + 0.28f * pop)
                                    val height = 34.dp.toPx() * (0.72f + 0.28f * pop)
                                    val corner = 12.dp.toPx()
                                    drawRoundRect(
                                        color = indicatorColor.copy(alpha = alpha),
                                        topLeft = Offset(
                                            (size.width - width) / 2f,
                                            (size.height - height) / 2f
                                        ),
                                        size = Size(width, height),
                                        cornerRadius = CornerRadius(corner, corner)
                                    )
                                }
                            },
                            badge = {
                                if (badgeContent > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                        Text(text = badgeContent.toString())
                                    }
                                }
                            }
                        ) {
                            val iconScale = 1f + 0.06f * selectionProgress.coerceIn(0f, 1.3f)
                            if (isSelected) {
                                NavBarIcon(
                                    destination,
                                    isSelected = true,
                                    modifier = Modifier.scale(iconScale)
                                )
                            } else {
                                NavBarIcon(
                                    destination,
                                    isSelected = false,
                                    modifier = Modifier.scale(iconScale)
                                )
                            }
                        }
                    },
                    label = {
                        // Long locales (e.g. Russian) can overflow their slot; shrink
                        // the label to fit before falling back to an ellipsis.
                        BasicText(
                            text = stringResource(destination.label),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                textAlign = TextAlign.Center,
                                color = if (isSelected) {
                                    wallpaper?.chrome?.contentColor ?: MaterialTheme.colorScheme.onSurface
                                }
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            autoSize = TextAutoSize.StepBased(
                                minFontSize = 8.sp,
                                maxFontSize = 12.sp,
                                stepSize = 0.5.sp
                            )
                        )
                    },
                    alwaysShowLabel = true
                )
            }
        }
    }
}
