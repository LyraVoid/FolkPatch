package me.bmax.apatch.ui.screen.settings

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.ui.component.folk.folkInsetColor
import me.bmax.apatch.ui.component.splicedLazyColumnGroup
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.ui.NavigationBarsSpacer
import java.util.Locale

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerScreen(navigator: DestinationsNavigator) {
    val languages = stringArrayResource(id = R.array.languages)
    val languagesValues = stringArrayResource(id = R.array.languages_values)
    val currentLocale = AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()
    val flat = BackgroundConfig.isCustomBackgroundEnabled || BackgroundConfig.settingsBackgroundUri != null
    val languageList = remember { languages.toList() }

    // Track the most recent tap so the new selection is reflected instantly;
    // setApplicationLocales() applies asynchronously and currentLocale only
    // updates once the activity recreates.
    var lastSelectedIndex by remember { mutableStateOf(-1) }
    LaunchedEffect(currentLocale) { lastSelectedIndex = -1 }

    FolkScaffold(
        title = stringResource(R.string.settings_app_language),
        titleStyle = FolkTitleStyle.Flexible,
        onBack = { navigator.popBackStack() },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(FolkDimens.Size0),
        ) {
            item { Spacer(Modifier.height(FolkDimens.Size12)) }

            splicedLazyColumnGroup(
                items = languageList,
                key = { index, _ -> languagesValues[index] },
            ) { index, item ->
                val isSelected = if (lastSelectedIndex != -1) {
                    lastSelectedIndex == index
                } else if (index == 0) {
                    currentLocale == null || currentLocale.isEmpty()
                } else {
                    currentLocale == languagesValues[index]
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            lastSelectedIndex = index
                            if (index == 0) {
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.getEmptyLocaleList()
                                )
                            } else {
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags(languagesValues[index])
                                )
                            }
                        }
                        .padding(horizontal = FolkDimens.Size16, vertical = FolkDimens.Size16),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (index == 0) {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(FolkDimens.Size24),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(FolkDimens.Size32)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else folkInsetColor(),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = getLanguageCode(languagesValues[index]),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.width(FolkDimens.Size16))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(FolkDimens.Size16)) }
            item { NavigationBarsSpacer() }
        }
    }
}

private fun getLanguageCode(tag: String): String = when {
    tag.contains("-") -> tag.substringBefore("-").uppercase()
    else -> tag.uppercase()
}
