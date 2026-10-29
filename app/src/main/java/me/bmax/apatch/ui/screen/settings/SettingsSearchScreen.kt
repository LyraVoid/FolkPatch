package me.bmax.apatch.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkNavigationPreference
import me.bmax.apatch.ui.component.folk.FolkSettingsDimens
import me.bmax.apatch.ui.component.folk.FolkSettingsGroup
import me.bmax.apatch.ui.component.folk.FolkStateView
import me.bmax.apatch.ui.component.folk.folkPressScale

internal data class SettingsSearchShortcut(
    val label: String,
    val icon: ImageVector,
    val onOpen: () -> Unit,
)

internal fun settingsSearchTokens(query: String): List<String> =
    query.trim().split(Regex("\\s+")).filter(String::isNotEmpty)

internal fun settingsSearchResults(
    query: String,
    entries: List<ResolvedEntry>,
): List<ResolvedEntry> {
    val tokens = settingsSearchTokens(query)
    if (tokens.isEmpty()) return emptyList()
    return entries.filter { entry ->
        tokens.all { token ->
            entry.title.contains(token, ignoreCase = true) ||
                entry.summary.contains(token, ignoreCase = true) ||
                entry.categoryName.contains(token, ignoreCase = true)
        }
    }
}

internal fun settingsMatchRanges(text: String, tokens: List<String>): List<IntRange> {
    if (text.isEmpty() || tokens.isEmpty()) return emptyList()
    val ranges = buildList {
        tokens.forEach { token ->
            var start = text.indexOf(token, ignoreCase = true)
            while (start >= 0) {
                add(start until start + token.length)
                start = text.indexOf(token, start + 1, ignoreCase = true)
            }
        }
    }.sortedBy { it.first }
    if (ranges.isEmpty()) return emptyList()

    val merged = mutableListOf(ranges.first())
    ranges.drop(1).forEach { range ->
        val current = merged.last()
        if (range.first <= current.last + 1) {
            merged[merged.lastIndex] = current.first..maxOf(current.last, range.last)
        } else {
            merged += range
        }
    }
    return merged
}

@Composable
internal fun SettingsSearchShortcuts(
    shortcuts: List<SettingsSearchShortcut>,
    onOpen: (SettingsSearchShortcut) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item(key = "shortcut_spacer") { Spacer(Modifier.height(FolkSettingsDimens.SectionSpacing)) }
        item(key = "shortcut_group") {
            FolkSettingsGroup {
                shortcuts.forEach { shortcut ->
                    item(key = shortcut.label) {
                        FolkNavigationPreference(
                            icon = shortcut.icon,
                            title = shortcut.label,
                            onClick = { onOpen(shortcut) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsSearchResults(
    query: String,
    entries: List<ResolvedEntry>,
    categories: Set<SettingCategory>,
    onSelect: (ResolvedEntry) -> Unit,
    contentPadding: PaddingValues,
) {
    val tokens = remember(query) { settingsSearchTokens(query) }
    val results = remember(entries, query, categories) {
        settingsSearchResults(query, entries.filter { it.entry.category in categories })
    }
    if (results.isEmpty()) {
        FolkStateView(
            title = stringResource(R.string.settings_no_results),
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            icon = Icons.Filled.SearchOff,
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        results
            .groupBy { it.entry.category }
            .toList()
            .sortedBy { (category, _) -> SearchCategoryOrder.indexOf(category) }
            .forEach { (category, categoryResults) ->
                item(key = "header_${category.key}") {
                    Text(
                        text = categoryResults.first().categoryName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = 12.dp,
                            bottom = 8.dp,
                        ),
                    )
                }
                item(key = "group_${category.key}") {
                    FolkSettingsGroup {
                        categoryResults.forEach { result ->
                            item(key = result.entry.key) {
                                SettingsSearchResultRow(
                                    result = result,
                                    tokens = tokens,
                                    onClick = { onSelect(result) },
                                )
                            }
                        }
                    }
                }
            }
    }
}

private val SearchCategoryOrder = listOf(
    SettingCategory.GENERAL,
    SettingCategory.APPEARANCE,
    SettingCategory.BEHAVIOR,
    SettingCategory.FUNCTION,
    SettingCategory.SECURITY,
    SettingCategory.BACKUP,
    SettingCategory.MODULE,
    SettingCategory.MULTIMEDIA,
)

@Composable
private fun SettingsSearchResultRow(
    result: ResolvedEntry,
    tokens: List<String>,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .folkPressScale(interactionSource)
            .clickable(
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            )
            .padding(
                start = FolkSettingsDimens.ItemHorizontalPadding,
                end = FolkSettingsDimens.ItemEndPadding,
                top = FolkSettingsDimens.ItemVerticalPaddingWithSummary,
                bottom = FolkSettingsDimens.ItemVerticalPaddingWithSummary,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = categoryIcon(result.entry.category),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(FolkSettingsDimens.IconSize),
        )
        Spacer(Modifier.width(FolkSettingsDimens.IconSpacing))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = highlightedText(result.title, settingsMatchRanges(result.title, tokens)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (result.summary.isNotEmpty()) {
                Text(
                    text = highlightedText(
                        result.summary,
                        settingsMatchRanges(result.summary, tokens),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(FolkSettingsDimens.TrailingSpacing))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(FolkSettingsDimens.ChevronSize),
        )
    }
}

private fun highlightedText(text: String, ranges: List<IntRange>): AnnotatedString =
    buildAnnotatedString {
        append(text)
        ranges.forEach { range ->
            addStyle(
                SpanStyle(fontWeight = FontWeight.SemiBold),
                start = range.first,
                end = (range.last + 1).coerceAtMost(text.length),
            )
        }
    }

internal fun categoryIcon(category: SettingCategory): ImageVector = when (category) {
    SettingCategory.GENERAL -> Icons.Outlined.Settings
    SettingCategory.APPEARANCE -> Icons.Outlined.Brush
    SettingCategory.BEHAVIOR -> Icons.Outlined.TouchApp
    SettingCategory.FUNCTION -> Icons.Outlined.Handyman
    SettingCategory.SECURITY -> Icons.Outlined.Lock
    SettingCategory.BACKUP -> Icons.Outlined.CloudUpload
    SettingCategory.MODULE -> Icons.Outlined.Extension
    SettingCategory.MULTIMEDIA -> Icons.Outlined.LibraryMusic
}
