package me.bmax.apatch.ui.screen.settings

import androidx.biometric.BiometricPrompt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.SegmentedNavigationItem
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSection
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.util.APatchKeyHelper
import androidx.compose.material.icons.outlined.*

@Composable
fun SecuritySettingsContent(
    snackBarHost: SnackbarHostState,
    kPatchReady: Boolean,
    flat: Boolean = false,
    highlightKey: String? = null,
) {
    val prefs = APApplication.sharedPreferences
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var biometricLogin by remember { mutableStateOf(prefs.getBoolean("biometric_login", false)) }
    var showEnableBiometricDialog by remember { mutableStateOf(false) }

    val biometricManager = androidx.biometric.BiometricManager.from(context)
    val canAuthenticate = biometricManager.canAuthenticate(
        androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
    ) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS

    if (canAuthenticate) {
    SegmentedSection(title = stringResource(R.string.settings_section_security_auth)) {
        SegmentedColumn(flat = flat, highlightKey = highlightKey) {
            item(key = "security_biometric_login", visible = canAuthenticate) {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.Fingerprint,
                    title = stringResource(id = R.string.settings_biometric_login),
                    summary = stringResource(id = R.string.settings_biometric_login_summary),
                    checked = biometricLogin,
                    onCheckedChange = { checked ->
                        if (!checked) {
                            if (activity != null) {
                                val executor = ContextCompat.getMainExecutor(context)
                                val biometricPrompt = BiometricPrompt(activity, executor,
                                    object : BiometricPrompt.AuthenticationCallback() {
                                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                            super.onAuthenticationSucceeded(result)
                                            biometricLogin = false
                                            prefs.edit().putBoolean("biometric_login", false).apply()
                                        }

                                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                            super.onAuthenticationError(errorCode, errString)
                                        }
                                    })

                                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                    .setTitle(context.getString(R.string.action_biometric))
                                    .setSubtitle(context.getString(R.string.msg_biometric))
                                    .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                                    .build()

                                biometricPrompt.authenticate(promptInfo)
                            } else {
                                biometricLogin = false
                                prefs.edit().putBoolean("biometric_login", false).apply()
                            }
                        } else {
                            showEnableBiometricDialog = true
                        }
                    }
                )
            }

            item(key = "security_strong_biometric", visible = biometricLogin && canAuthenticate) {
                var strongBiometric by remember { mutableStateOf(prefs.getBoolean("strong_biometric", false)) }
                SegmentedSwitchItem(
                    icon = Icons.Outlined.Shield,
                    title = stringResource(id = R.string.settings_strong_biometric),
                    summary = stringResource(id = R.string.settings_strong_biometric_summary),
                    checked = strongBiometric,
                    onCheckedChange = {
                        strongBiometric = it
                        prefs.edit().putBoolean("strong_biometric", it).apply()
                    },
                )
            }
        }
    }
    }

    if (kPatchReady) {
    SegmentedSection(title = stringResource(R.string.settings_section_security_key)) {
        SegmentedColumn(flat = flat, highlightKey = highlightKey) {
            item(key = "security_clear_superkey", visible = kPatchReady) {
                val title = stringResource(id = R.string.clear_super_key)
                val clearDialog = rememberConfirmDialog {
                    APatchKeyHelper.clearConfigKey()
                    APApplication.setSuperKeyAndRefresh("su")
                }
                SegmentedNavigationItem(
                    icon = Icons.Outlined.DeleteForever,
                    title = title,
                    onClick = {
                        clearDialog.showConfirm(
                            title = title,
                            content = context.getString(R.string.settings_clear_super_key_dialog),
                        )
                    },
                )
            }

            item(key = "security_no_store_superkey", visible = kPatchReady) {
                var noStoreKey by remember {
                    mutableStateOf(APatchKeyHelper.shouldSkipStoreSuperKey())
                }
                SegmentedSwitchItem(
                    icon = Icons.Outlined.Key,
                    title = stringResource(id = R.string.settings_donot_store_superkey),
                    summary = stringResource(id = R.string.settings_donot_store_superkey_summary),
                    checked = noStoreKey,
                    onCheckedChange = {
                        noStoreKey = it
                        APatchKeyHelper.setShouldSkipStoreSuperKey(it)
                    },
                )
            }
        }
    }
    }

    if (showEnableBiometricDialog) {
        AlertDialog(
            onDismissRequest = { showEnableBiometricDialog = false },
            title = { Text(stringResource(id = R.string.settings_biometric_login)) },
            text = { Text(stringResource(id = R.string.msg_enable_biometric)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        biometricLogin = true
                        prefs.edit().putBoolean("biometric_login", true).apply()
                        showEnableBiometricDialog = false
                    }
                ) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEnableBiometricDialog = false }
                ) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}
