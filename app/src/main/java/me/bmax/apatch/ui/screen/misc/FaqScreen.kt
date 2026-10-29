package me.bmax.apatch.ui.screen.misc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkSettingsDimens
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.ui.component.folk.folkGroupColor
import me.bmax.apatch.ui.component.folk.folkPressScale
import org.json.JSONObject
import android.content.Context
import java.util.Locale
import androidx.compose.animation.core.animateDpAsState
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.semantics.Role

private const val FAQ_FALLBACK_ASSET = "faq/default.json"

// Modern Android reports some language codes while bundled assets may use the
// legacy qualifier (Indonesian "id" vs "in"), so try both spellings.
private val FAQ_LANGUAGE_ALIASES = mapOf("id" to "in", "he" to "iw", "yi" to "ji")

internal data class FaqItem(val title: String, val body: String)

@Destination<RootGraph>
@Composable
fun FaqScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val items by produceState(initialValue = emptyList<FaqItem>()) {
        value = withContext(Dispatchers.IO) { loadFaqItems(context) }
    }
    var expandedIds by remember { mutableStateOf(emptySet<Int>()) }

    FolkScaffold(
        title = stringResource(R.string.settings_faq),
        titleStyle = FolkTitleStyle.Flexible,
        onBack = dropUnlessResumed { navigator.popBackStack() },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = FolkSettingsDimens.ScreenPadding,
                end = FolkSettingsDimens.ScreenPadding,
                top = 12.dp,
                bottom = FolkSettingsDimens.ScreenPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(items) { index, item ->
                FaqCard(
                    item = item,
                    expanded = index in expandedIds,
                    onToggle = {
                        expandedIds = if (index in expandedIds) expandedIds - index else expandedIds + index
                    },
                )
            }
        }
    }
}

@Composable
private fun FaqCard(item: FaqItem, expanded: Boolean, onToggle: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "FaqChevronRotation",
    )
    // Grow the corner while the answer is open so the card reads as lifted, and
    // give a smaller lift while the row is held.
    val pressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = when {
            expanded -> 28.dp
            pressed -> 24.dp
            else -> 20.dp
        },
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "FaqCardCorner",
    )

    Surface(
        color = folkGroupColor(),
        shape = ContinuousCornerShape(corner),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .folkPressScale(interactionSource, true)
                    .clickable(role = Role.Button, interactionSource = interactionSource, indication = null) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggle()
                    }
                    .padding(
                        horizontal = FolkSettingsDimens.ItemHorizontalPadding,
                        vertical = FolkSettingsDimens.ItemVerticalPadding,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(FolkSettingsDimens.TrailingSpacing))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(FolkSettingsDimens.ChevronSize)
                        .rotate(rotation),
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
            ) {
                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = FolkSettingsDimens.ItemHorizontalPadding,
                        end = FolkSettingsDimens.ItemHorizontalPadding,
                        bottom = FolkSettingsDimens.ItemVerticalPadding,
                    ),
                )
            }
        }
    }
}

private fun loadFaqItems(context: Context): List<FaqItem> = runCatching {
    val text = context.assets.open(resolveFaqAsset(context)).bufferedReader().use { it.readText() }
    val array = JSONObject(text).optJSONArray("items") ?: return@runCatching emptyList()
    buildList {
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val title = obj.optString("title").trim()
            if (title.isEmpty()) continue
            add(FaqItem(title = title, body = obj.optString("body").trim()))
        }
    }
}.getOrDefault(emptyList())

/**
 * Picks the FAQ file matching the current locale: `faq/<lang>-<REGION>.json`, then
 * `faq/<lang>.json`, then the bundled `faq/default.json` fallback.
 */
private fun resolveFaqAsset(context: Context): String {
    val locales = context.resources.configuration.locales
    val locale = if (locales.isEmpty) Locale.getDefault() else locales[0]
    val language = locale.language
    val region = locale.country
    val languages = buildList {
        if (language.isNotEmpty()) add(language)
        FAQ_LANGUAGE_ALIASES[language]?.let { add(it) }
    }
    val candidates = buildList {
        languages.forEach { lang ->
            if (region.isNotEmpty()) add("faq/$lang-r$region.json")
        }
        languages.forEach { lang -> add("faq/$lang.json") }
        add(FAQ_FALLBACK_ASSET)
    }
    return candidates.firstOrNull { candidate ->
        runCatching { context.assets.open(candidate).close() }.isSuccess
    } ?: FAQ_FALLBACK_ASSET
}
