package me.bmax.apatch.ui.component.folk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.bmax.apatch.ui.theme.tokens.FolkMotion

object FolkTransitions {
    private const val SlotScale = 0.7f

    val SlotEnter: EnterTransition = fadeIn(FolkMotion.SlotFade) +
        scaleIn(initialScale = SlotScale, animationSpec = FolkMotion.SlotScale) +
        expandHorizontally(animationSpec = FolkMotion.SlotSize, expandFrom = Alignment.End)

    val SlotExit: ExitTransition = fadeOut(FolkMotion.SlotFade) +
        scaleOut(targetScale = SlotScale, animationSpec = FolkMotion.SlotScale) +
        shrinkHorizontally(animationSpec = FolkMotion.SlotSize, shrinkTowards = Alignment.End)
}

@Composable
fun FolkAnimatedSlot(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = FolkTransitions.SlotEnter,
    exit: ExitTransition = FolkTransitions.SlotExit,
    label: String = "FolkAnimatedSlot",
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        label = label,
        content = content,
    )
}
