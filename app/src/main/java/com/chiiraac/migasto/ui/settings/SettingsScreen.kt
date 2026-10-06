package com.chiiraac.migasto.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chiiraac.migasto.BuildConfig
import com.chiiraac.migasto.R
import com.chiiraac.migasto.data.model.ThemeMode
import com.chiiraac.migasto.ui.components.SectionCard
import com.chiiraac.migasto.ui.components.SettingsRow
import com.chiiraac.migasto.ui.components.label
import com.chiiraac.migasto.ui.components.messageRes
import com.chiiraac.migasto.ui.main.MainUiState

@Composable
fun SettingsScreen(
    state: MainUiState,
    onUpdateName: (String) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onExportCsv: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: (password: String?, onResult: (Throwable?) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val user = state.user
    var editingName by rememberSaveable { mutableStateOf(false) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val privacyUrl = stringResource(R.string.privacy_policy_url)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "profile") {
            SectionCard(title = stringResource(R.string.settings_profile)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel(stringResource(R.string.settings_name))
                        Text(
                            user?.name.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { editingName = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.settings_edit_name))
                    }
                }
                val email = user?.email
                if (!email.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    FieldLabel(stringResource(R.string.settings_email))
                    Text(
                        email,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        item(key = "appearance") {
            SectionCard(title = stringResource(R.string.settings_appearance)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.themeMode == mode,
                            onClick = { onThemeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                        ) { Text(stringResource(mode.label), maxLines = 1) }
                    }
                }
            }
        }
        item(key = "data") {
            SectionCard(title = stringResource(R.string.settings_data)) {
                SettingsRow(
                    icon = Icons.Rounded.FileDownload,
                    title = stringResource(R.string.settings_export_csv),
                    subtitle = state.selectedGroup?.let { stringResource(R.string.settings_export_csv_desc, it.name) },
                    onClick = onExportCsv,
                )
            }
        }
        item(key = "info") {
            SectionCard(title = stringResource(R.string.settings_app_info)) {
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (state.isCloud) Icons.Rounded.CloudDone else Icons.Rounded.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(if (state.isCloud) R.string.settings_mode_cloud else R.string.settings_mode_local),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(if (state.isCloud) R.string.privacy_policy_cloud else R.string.privacy_policy_local),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                SettingsRow(
                    icon = Icons.Rounded.Policy,
                    title = stringResource(R.string.settings_privacy_link),
                    subtitle = null,
                    onClick = { runCatching { uriHandler.openUri(privacyUrl) } },
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        item(key = "account") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.isCloud) {
                    Button(
                        onClick = { confirmSignOut = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.sign_out))
                    }
                }
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                ) {
                    Text(
                        stringResource(if (state.isCloud) R.string.delete_account else R.string.delete_local_data),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    if (editingName) {
        NameDialog(
            initial = user?.name.orEmpty(),
            onDismiss = { editingName = false },
            onConfirm = {
                onUpdateName(it)
                editingName = false
            },
        )
    }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(stringResource(R.string.sign_out_title)) },
            text = { Text(stringResource(R.string.sign_out_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    onSignOut()
                }) { Text(stringResource(R.string.sign_out)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
    if (confirmDelete) {
        DeleteAccountDialog(
            isCloud = state.isCloud,
            onDismiss = { confirmDelete = false },
            onConfirm = onDeleteAccount,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun NameDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_edit_name)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text(stringResource(R.string.settings_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun DeleteAccountDialog(
    isCloud: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (password: String?, onResult: (Throwable?) -> Unit) -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var working by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text(stringResource(if (isCloud) R.string.delete_account_title else R.string.delete_local_title)) },
        text = {
            Column {
                Text(stringResource(if (isCloud) R.string.delete_account_body else R.string.delete_local_body))
                if (isCloud) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            error = null
                        },
                        label = { Text(stringResource(R.string.delete_account_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = error != null,
                        supportingText = error?.let { { Text(stringResource(it)) } },
                        enabled = !working,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    working = true
                    onConfirm(password.takeIf { isCloud }) { failure ->
                        working = false
                        if (failure != null) error = failure.messageRes()
                    }
                },
                enabled = !working && (!isCloud || password.isNotEmpty()),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                if (working) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.action_delete))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !working) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
