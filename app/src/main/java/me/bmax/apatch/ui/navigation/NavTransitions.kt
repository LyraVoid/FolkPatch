package me.bmax.apatch.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import me.bmax.apatch.ui.component.folk.NavOriginState

fun createNavTransitions(
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
                fadeIn(animationSpec = tween(340))
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
                fadeOut(animationSpec = tween(340))
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
