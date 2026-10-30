package me.bmax.apatch.ui.screen.module

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.generated.destinations.OnlineKPMScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.R
import me.bmax.apatch.ui.LayoutVariant
import me.bmax.apatch.ui.LocalLayoutVariant
import me.bmax.apatch.ui.component.SearchAppBar
import me.bmax.apatch.ui.component.SearchAppBarScrollBehavior
import me.bmax.apatch.ui.component.folk.navOrigin

@Composable
fun KPMTopBar(
    navigator: DestinationsNavigator,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    scrollBehavior: SearchAppBarScrollBehavior?,
    searchContent: @Composable (bottomPadding: Dp, closeSearch: () -> Unit) -> Unit,
) {
    val m3eLayout = LocalLayoutVariant.current == LayoutVariant.M3E

    SearchAppBar(
        largeTitle = m3eLayout,
        topSearch = !m3eLayout,
        collapsibleSearch = m3eLayout,
        searchPlaceholder = stringResource(R.string.kpm_search_hint),
        scrollBehavior = scrollBehavior,
        title = { Text(stringResource(R.string.kpm)) },
        searchText = searchQuery,
        onSearchTextChange = onSearchQueryChange,
        onClearClick = { onSearchQueryChange("") },
        navigationIcon = {
            IconButton(
                onClick = dropUnlessResumed { navigator.navigate(OnlineKPMScreenDestination) },
                modifier = androidx.compose.ui.Modifier.navOrigin(captureBitmap = true),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Cloud,
                    contentDescription = stringResource(R.string.online_kpm_title)
                )
            }
        },
        defaultContent = { bottomPadding, closeSearch ->
            searchContent(bottomPadding, closeSearch)
        },
        searchContent = { bottomPadding, closeSearch ->
            searchContent(bottomPadding, closeSearch)
        },
    )
}
