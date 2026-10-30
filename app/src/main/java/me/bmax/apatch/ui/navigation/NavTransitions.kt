package me.bmax.apatch.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import me.bmax.apatch.ui.component.folk.NavOriginState

fun createNavTransitions(
    folkXEngineEnabled: Boolean,
    folkXAnimationType: String?,
    folkXAnimationSpeed: Float,
    bottomBarRoutes: Set<String>,
    useNavigationRail: Boolean = false,
    navOriginState: NavOriginState? = null,
    wallpaperActive: Boolean = false,
): NavHostAnimatedDestinationStyle {
    return object : NavHostAnimatedDestinationStyle() {
        fun preparePushOrigin(scope: AnimatedContentTransitionScope<NavBackStackEntry>) {
            val origins = navOriginState ?: return
            if (wallpaperActive) return
            val targetId = scope.targetState.id
            if (!origins.hasOrigin(targetId) && origins.hasPendingOrigin()) {
                origins.commitPending(targetId)
            }
        }

        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            preparePushOrigin(this)
            if (shouldUseContainerTransform(wallpaperActive, navOriginState?.hasOrigin(targetState.id) == true)) {
                EnterTransition.None
            } else
            if (targetState.destination.route !in bottomBarRoutes) {
                slideInHorizontally(initialOffsetX = { it })
            } else {
                if (folkXEngineEnabled) {
                    val initialRoute = initialState.destination.route
                    val targetRoute = targetState.destination.route
                    val initialIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == initialRoute }
                    val targetIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == targetRoute }

                    val stiffness = 300f * folkXAnimationSpeed * folkXAnimationSpeed
                    val duration300 = (300 / folkXAnimationSpeed).toInt()

                    if (initialIndex != -1 && targetIndex != -1) {
                        when (folkXAnimationType) {
                            "spatial" -> {
                                if (targetIndex > initialIndex) {
                                    scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness)) + fadeIn(animationSpec = tween(duration300))
                                } else {
                                    scaleIn(initialScale = 1.1f, animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness)) + fadeIn(animationSpec = tween(duration300))
                                }
                            }
                            "fade" -> fadeIn(animationSpec = tween(duration300))
                            "vertical" -> {
                                if (targetIndex > initialIndex) {
                                    slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetY = { height -> height }) + fadeIn()
                                } else {
                                    slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetY = { height -> -height }) + fadeIn()
                                }
                            }
                            "diagonal" -> {
                                if (targetIndex > initialIndex) {
                                    slideInHorizontally(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetX = { width -> width }) +
                                    slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetY = { height -> height }) + fadeIn()
                                } else {
                                    slideInHorizontally(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetX = { width -> -width }) +
                                    slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetY = { height -> -height }) + fadeIn()
                                }
                            }
                            else -> {
                                // linear: 侧边导航栏使用上下滑动，底部导航栏使用左右滑动
                                if (useNavigationRail) {
                                    if (targetIndex > initialIndex) {
                                        slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetY = { height -> height }) + fadeIn()
                                    } else {
                                        slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetY = { height -> -height }) + fadeIn()
                                    }
                                } else {
                                    if (targetIndex > initialIndex) {
                                        slideInHorizontally(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetX = { width -> width })
                                    } else {
                                        slideInHorizontally(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetX = { width -> -width })
                                    }
                                }
                            }
                        }
                    } else {
                        fadeIn(animationSpec = tween(340))
                    }
                } else {
                    fadeIn(animationSpec = tween(340))
                }
            }
        }

        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            preparePushOrigin(this)
            if (shouldUseContainerTransform(wallpaperActive, navOriginState?.hasOrigin(targetState.id) == true)) {
                fadeOut(
                    animationSpec = tween(durationMillis = 300),
                    targetAlpha = 0.999f,
                )
            } else
            if (initialState.destination.route in bottomBarRoutes && targetState.destination.route !in bottomBarRoutes) {
                slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut()
            } else {
                if (folkXEngineEnabled && initialState.destination.route in bottomBarRoutes && targetState.destination.route in bottomBarRoutes) {
                    val initialRoute = initialState.destination.route
                    val targetRoute = targetState.destination.route
                    val initialIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == initialRoute }
                    val targetIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == targetRoute }

                    val stiffness = 300f * folkXAnimationSpeed * folkXAnimationSpeed
                    val duration300 = (300 / folkXAnimationSpeed).toInt()
                    val duration600 = (600 / folkXAnimationSpeed).toInt()

                    if (initialIndex != -1 && targetIndex != -1) {
                        when (folkXAnimationType) {
                            "spatial" -> {
                                if (targetIndex > initialIndex) {
                                    scaleOut(targetScale = 1.1f, animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness)) + fadeOut(animationSpec = tween(duration300))
                                } else {
                                    scaleOut(targetScale = 0.9f, animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness)) + fadeOut(animationSpec = tween(duration300))
                                }
                            }
                            "fade" -> fadeOut(animationSpec = tween(duration600))
                            "vertical" -> {
                                if (targetIndex > initialIndex) {
                                    slideOutVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), targetOffsetY = { height -> -height }) + fadeOut()
                                } else {
                                    slideOutVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), targetOffsetY = { height -> height }) + fadeOut()
                                }
                            }
                            "diagonal" -> {
                                if (targetIndex > initialIndex) {
                                    slideOutHorizontally(animationSpec = tween(duration600), targetOffsetX = { width -> -width }) +
                                    slideOutVertically(animationSpec = tween(duration600), targetOffsetY = { height -> -height }) + fadeOut(animationSpec = tween(duration600))
                                } else {
                                    slideOutHorizontally(animationSpec = tween(duration600), targetOffsetX = { width -> width }) +
                                    slideOutVertically(animationSpec = tween(duration600), targetOffsetY = { height -> height }) + fadeOut(animationSpec = tween(duration600))
                                }
                            }
                            else -> {
                                // linear: 侧边导航栏使用上下滑动，底部导航栏使用左右滑动
                                if (useNavigationRail) {
                                    if (targetIndex > initialIndex) {
                                        slideOutVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), targetOffsetY = { height -> -height }) + fadeOut()
                                    } else {
                                        slideOutVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), targetOffsetY = { height -> height }) + fadeOut()
                                    }
                                } else {
                                    if (targetIndex > initialIndex) {
                                        slideOutHorizontally(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), targetOffsetX = { width -> -width })
                                    } else {
                                        slideOutHorizontally(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), targetOffsetX = { width -> width })
                                    }
                                }
                            }
                        }
                    } else {
                        fadeOut(animationSpec = tween(340))
                    }
                } else {
                    fadeOut(animationSpec = tween(340))
                }
            }
        }

        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            if (shouldUseContainerTransform(wallpaperActive, navOriginState?.hasOrigin(initialState.id) == true)) {
                EnterTransition.None
            } else
            if (targetState.destination.route in bottomBarRoutes) {
                if (initialState.destination.route !in bottomBarRoutes || !useNavigationRail) {
                    slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()
                } else {
                    slideInVertically(initialOffsetY = { -it / 4 }) + fadeIn()
                }
            } else {
                fadeIn(animationSpec = tween(340))
            }
        }

        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            if (shouldUseContainerTransform(wallpaperActive, navOriginState?.hasOrigin(initialState.id) == true)) {
                fadeOut(
                    animationSpec = tween(durationMillis = 280),
                    targetAlpha = 0.999f,
                )
            } else
            if (initialState.destination.route !in bottomBarRoutes) {
                scaleOut(targetScale = 0.9f) + fadeOut()
            } else {
                fadeOut(animationSpec = tween(340))
            }
        }
    }
}
