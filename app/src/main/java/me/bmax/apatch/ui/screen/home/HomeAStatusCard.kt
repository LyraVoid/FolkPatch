@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package me.bmax.apatch.ui.screen.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import me.bmax.apatch.ui.theme.BackgroundConfig
import androidx.compose.material3.Card
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.core.ui.R as CoreR
import me.bmax.apatch.ui.theme.tokens.FolkShape

@Composable
fun AStatusCard(apState: APApplication.State) {
    Card(
        shape = FolkShape.Corner20,
        colors = CardDefaults.cardColors(
            // 壁纸模式下用中性 surface/onSurface，避免把强调容器降 alpha 后
            // onSecondaryContainer 文字直接压在壁纸上而失去对比度。
            containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    text = stringResource(R.string.android_patch),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (apState) {
                    APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> {
                        Icon(Icons.Outlined.Block, stringResource(R.string.home_not_installed))
                    }

                    APApplication.State.ANDROIDPATCH_INSTALLING -> {
                        Icon(Icons.Outlined.InstallMobile, stringResource(R.string.home_installing))
                    }

                    APApplication.State.ANDROIDPATCH_INSTALLED -> {
                        Icon(Icons.Outlined.CheckCircle, stringResource(R.string.home_working))
                    }

                    APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                        Icon(Icons.Outlined.SystemUpdate, stringResource(R.string.home_kp_need_update))
                    }

                    else -> {
                        Icon(
                            Icons.AutoMirrored.Outlined.HelpOutline,
                            stringResource(R.string.home_install_unknown)
                        )
                    }
                }
                Column(
                    Modifier
                        .weight(2f)
                        .padding(start = 16.dp)
                ) {

                    when (apState) {
                        APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> {
                            Text(
                                text = stringResource(R.string.home_not_installed),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        APApplication.State.ANDROIDPATCH_INSTALLING -> {
                            Text(
                                text = stringResource(R.string.home_installing),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        APApplication.State.ANDROIDPATCH_INSTALLED -> {
                            Text(
                                text = stringResource(R.string.home_working),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                            Text(
                                text = stringResource(R.string.home_kp_need_update),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        else -> {
                            Text(
                                text = stringResource(R.string.home_install_unknown),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                if (apState != APApplication.State.UNKNOWN_STATE) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Button(onClick = {
                            when (apState) {
                                APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> {
                                    APApplication.installApatch()
                                }

                                APApplication.State.ANDROIDPATCH_UNINSTALLING -> {
                                    // Do nothing
                                }

                                APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                                    APApplication.installApatch()
                                }

                                else -> {
                                    APApplication.uninstallApatch()
                                }
                            }
                        },
                        contentPadding = ButtonDefaults.MediumContentPadding,
                        content = {
                            when (apState) {
                                APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> {
                                    Text(text = stringResource(id = R.string.home_ap_cando_install))
                                }

                                APApplication.State.ANDROIDPATCH_UNINSTALLING -> {
                                    Icon(Icons.Outlined.Cached, contentDescription = stringResource(CoreR.string.core_state_busy))
                                }

                                APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                                    Text(text = stringResource(id = R.string.home_kp_cando_update))
                                }

                                else -> {
                                    Text(text = stringResource(id = R.string.home_ap_cando_uninstall))
                                }
                            }
                        })
                    }
                }
            }
        }
    }
}


