package me.bmax.apatch.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.bmax.apatch.ui.component.folk.FolkSettingsDimens
import me.bmax.apatch.ui.component.folk.folkPreferenceSummaryStyle
import me.bmax.apatch.ui.component.folk.folkSectionTitleColor
import me.bmax.apatch.ui.component.folk.folkSectionTitleStyle
import me.bmax.apatch.ui.component.folk.folkSupportingColor

@Composable
fun SegmentedSection(
    title: String? = null,
    description: String? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = FolkSettingsDimens.SectionSpacing),
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = folkSectionTitleStyle(),
                color = folkSectionTitleColor(),
                modifier = Modifier.padding(
                    start = FolkSettingsDimens.ScreenPadding + FolkSettingsDimens.SectionTitleIndent,
                    end = FolkSettingsDimens.ScreenPadding,
                    bottom = if (description == null) {
                        FolkSettingsDimens.SectionTitleSpacing
                    } else {
                        2.dp
                    },
                ),
            )
        }
        if (description != null) {
            Text(
                text = description,
                style = folkPreferenceSummaryStyle(),
                color = folkSupportingColor(),
                modifier = Modifier.padding(
                    start = FolkSettingsDimens.ScreenPadding + FolkSettingsDimens.SectionTitleIndent,
                    end = FolkSettingsDimens.ScreenPadding,
                    bottom = FolkSettingsDimens.SectionTitleSpacing,
                ),
            )
        }
        content()
    }
}
