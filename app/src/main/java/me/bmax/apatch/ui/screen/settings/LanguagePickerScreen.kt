package me.bmax.apatch.ui.screen.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.splicedLazyColumnGroup
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.ui.NavigationBarsSpacer
import java.util.Locale

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerScreen(navigator: DestinationsNavigator) {
    val languages = stringArrayResource(id = R.array.languages)
    val languagesValues = stringArrayResource(id = R.array.languages_values)
    val currentLocale = AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()
    val flat = BackgroundConfig.isCustomBackgroundEnabled || BackgroundConfig.settingsBackgroundUri != null
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val languageList = remember { languages.toList() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_app_language),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item { Spacer(Modifier.height(12.dp)) }

            splicedLazyColumnGroup(
                items = languageList,
                key = { index, _ -> languagesValues[index] },
            ) { index, item ->
                val isSelected = if (index == 0) {
                    currentLocale == null || currentLocale.isEmpty()
                } else {
                    currentLocale == languagesValues[index]
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
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
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (index == 0) {
                        Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
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
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
            item { NavigationBarsSpacer() }
        }
    }
}

private fun getLanguageCode(tag: String): String = when {
    tag.contains("-") -> tag.substringBefore("-").uppercase()
    else -> tag.uppercase()
}
