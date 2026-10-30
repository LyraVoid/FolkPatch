package me.bmax.apatch.ui.component

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.bmax.apatch.ui.component.folk.FolkSettingsDimens
import me.bmax.apatch.ui.component.folk.folkSupportingColor
import me.bmax.apatch.ui.component.folk.folkIconColor
import me.bmax.apatch.ui.component.folk.folkPressScale
import me.bmax.apatch.ui.component.folk.folkPreferenceTitleStyle
import me.bmax.apatch.ui.component.folk.folkPreferenceSummaryStyle
import me.bmax.apatch.ui.component.folk.folkPreferenceValueStyle
import me.bmax.apatch.ui.component.folk.navOrigin
import me.bmax.apatch.ui.component.folk.folkGroupColor
import me.bmax.apatch.ui.theme.tokens.FolkDimens

@Composable
fun SegmentedPreferenceItem(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    iconTint: Color = folkIconColor(enabled),
    summary: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val clickModifier = if (onClick != null) {
        Modifier
            .folkPressScale(interactionSource, enabled)
            .combinedClickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = onLongClick,
            )
    } else {
        Modifier
    }

    SegmentedRowContent(
        title = title,
        modifier = modifier.then(clickModifier),
        icon = icon,
        enabled = enabled,
        iconTint = iconTint,
        summary = summary,
        selected = selected,
        trailing = trailing,
    )
}

@Composable
fun SegmentedSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    summary: String? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    SegmentedRowContent(
        title = title,
        modifier = modifier
            .folkPressScale(interactionSource, enabled)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(it)
                },
            ),
        icon = icon,
        iconTint = folkIconColor(enabled),
        summary = summary,
        enabled = enabled,
        trailing = {
            ExpressiveSwitch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        },
    )
}

@Composable
fun SegmentedCheckboxItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    summary: String? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    SegmentedRowContent(
        title = title,
        modifier = modifier
            .folkPressScale(interactionSource, enabled)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(it)
                },
            ),
        icon = icon,
        iconTint = folkIconColor(enabled),
        summary = summary,
        enabled = enabled,
        trailing = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        },
    )
}

@Composable
fun SegmentedNavigationItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    summary: String? = null,
    enabled: Boolean = true,
) {
    SegmentedPreferenceItem(
        title = title,
        modifier = modifier.navOrigin(cornerRadius = FolkDimens.Size20, color = folkGroupColor()),
        icon = icon,
        summary = summary,
        enabled = enabled,
        onClick = onClick,
        trailing = { SegmentedChevron(enabled) },
    )
}

@Composable
fun SegmentedValueItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    summary: String? = null,
    value: String? = null,
    showChevron: Boolean = true,
    enabled: Boolean = true,
) {
    SegmentedPreferenceItem(
        title = title,
        modifier = modifier,
        icon = icon,
        summary = summary,
        enabled = enabled,
        onClick = onClick,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(
                        text = value,
                        style = folkPreferenceValueStyle(),
                        color = if (enabled) {
                            folkSupportingColor()
                        } else {
                            folkSupportingColor().copy(alpha = 0.38f)
                        },
                    )
                    if (showChevron) Spacer(Modifier.width(6.dp))
                }
                if (showChevron) SegmentedChevron(enabled)
            }
        },
    )
}

@Composable
fun SegmentedSelectableItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    SegmentedRowContent(
        title = title,
        modifier = modifier
            .folkPressScale(interactionSource, enabled)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            ),
        summary = summary,
        selected = selected,
        enabled = enabled,
        trailing = { RadioButton(selected = selected, onClick = null, enabled = enabled) },
    )
}

@Composable
fun SegmentedChevron(enabled: Boolean = true) {
    Icon(
        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = folkSupportingColor().copy(
            alpha = if (enabled) 0.7f else 0.3f,
        ),
        modifier = Modifier.size(FolkSettingsDimens.ChevronSize),
    )
}

@Composable
private fun SegmentedRowContent(
    title: String,
    modifier: Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    iconTint: Color = folkIconColor(enabled),
    summary: String? = null,
    selected: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val titleColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val summaryColor = when {
        !enabled -> folkSupportingColor().copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        else -> folkSupportingColor()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = FolkSettingsDimens.ItemMinHeight)
            .padding(
                start = FolkSettingsDimens.ItemHorizontalPadding,
                end = FolkSettingsDimens.ItemEndPadding,
                top = FolkSettingsDimens.ItemVerticalPadding,
                bottom = FolkSettingsDimens.ItemVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(FolkSettingsDimens.IconSize),
            )
            Spacer(Modifier.width(FolkSettingsDimens.IconSpacing))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = folkPreferenceTitleStyle(),
                color = titleColor,
            )
            if (summary != null) {
                Spacer(Modifier.height(FolkSettingsDimens.TitleSummarySpacing))
                Text(
                    text = summary,
                    style = folkPreferenceSummaryStyle(),
                    color = summaryColor,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(FolkSettingsDimens.TrailingSpacing))
            Box(contentAlignment = Alignment.Center) { trailing() }
        }
    }
}
