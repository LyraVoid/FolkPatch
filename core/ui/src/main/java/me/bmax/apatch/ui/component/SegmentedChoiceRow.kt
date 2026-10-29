package me.bmax.apatch.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.bmax.apatch.ui.component.folk.FolkSettingsDimens

@Composable
fun SegmentedChoiceRow(
    items: List<String>,
    selectedIndex: Int,
    onItemSelection: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FolkSettingsDimens.ScreenPadding),
    ) {
        items.forEachIndexed { index, item ->
            SegmentedButton(
                selected = selectedIndex == index,
                onClick = { onItemSelection(index) },
                shape = SegmentedButtonDefaults.itemShape(index, items.size),
            ) {
                Text(item)
            }
        }
    }
}
