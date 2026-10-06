package com.chiiraac.migasto.ui.groups

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import com.chiiraac.migasto.data.model.Member
import android.provider.Settings
import com.chiiraac.migasto.R
import com.chiiraac.migasto.notifications.Notifications
import com.chiiraac.migasto.data.InviteCodes
import com.chiiraac.migasto.data.model.Group
import com.chiiraac.migasto.data.model.GroupIcon
import com.chiiraac.migasto.ui.components.MemberAvatar
import com.chiiraac.migasto.ui.components.label
import com.chiiraac.migasto.ui.components.messageRes
import com.chiiraac.migasto.ui.components.vector
import kotlinx.coroutines.launch

/** Hoja "Tus Grupos": cambiar de grupo, crear uno nuevo o unirse a otro. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsSheet(
    groups: List<Group>,
    selectedId: String?,
    onSelect: (Group) -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(stringResource(R.string.groups_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            groups.forEach { group ->
                val selected = group.id == selectedId
                Surface(
                    onClick = { onSelect(group) },
                    shape = MaterialTheme.shapes.large,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .semantics { this.selected = selected },
                ) {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(group.icon.vector, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                group.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (group.members.size > 1) {
                                Text(
                                    group.members.joinToString { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (selected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = stringResource(R.string.cd_selected),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            SheetButton(Icons.Rounded.Add, stringResource(R.string.group_create), onCreate)
            Spacer(Modifier.height(12.dp))
            SheetButton(Icons.Rounded.PersonAdd, stringResource(R.string.group_join), onJoin)
        }
    }
}

@Composable
private fun SheetButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Normal)
    }
}

/** Crear un grupo nuevo o editar el nombre/icono de uno existente. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupEditorDialog(
    initialName: String,
    initialIcon: GroupIcon,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, GroupIcon) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var icon by rememberSaveable { mutableStateOf(initialIcon) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.group_create_title else R.string.group_edit_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = { Text(stringResource(R.string.group_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.group_icon), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GroupIcon.entries.forEach { option ->
                        val selected = option == icon
                        val description = stringResource(option.label)
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                                )
                                .clickable { icon = option }
                                .semantics {
                                    contentDescription = description
                                    this.selected = selected
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                option.vector,
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), icon) }, enabled = name.isNotBlank()) {
                Text(stringResource(if (isNew) R.string.action_create else R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Unirse a un grupo con su código de invitación. */
@Composable
fun JoinGroupDialog(
    isCloud: Boolean,
    onDismiss: () -> Unit,
    onJoin: (code: String, onResult: (Throwable?) -> Unit) -> Unit,
) {
    if (!isCloud) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.group_join_title)) },
            text = { Text(stringResource(R.string.group_join_local)) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
        )
        return
    }
    var code by rememberSaveable { mutableStateOf("") }
    // `remember` (no saveable): si se recrea la actividad a mitad de la petición, el diálogo
    // vuelve a quedar utilizable en lugar de mostrar un indicador de carga para siempre.
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    val submit = {
        working = true
        error = null
        onJoin(code) { failure ->
            working = false
            if (failure == null) onDismiss() else error = failure.messageRes()
        }
    }
    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text(stringResource(R.string.group_join_title)) },
        text = {
            Column {
                Text(stringResource(R.string.group_join_body))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = InviteCodes.normalize(it)
                        error = null
                    },
                    label = { Text(stringResource(R.string.group_code)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (code.length == InviteCodes.LENGTH && !working) submit() }),
                    isError = error != null,
                    supportingText = error?.let { { Text(stringResource(it)) } },
                    enabled = !working,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = code.length == InviteCodes.LENGTH && !working) {
                if (working) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.action_join))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !working) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Diálogo "Ajustes: Casa" con el código de invitación y los participantes. */
@Composable
fun GroupSettingsDialog(
    group: Group,
    currentUserId: String?,
    isCloud: Boolean,
    onEdit: () -> Unit,
    onLeave: () -> Unit,
    onDismiss: () -> Unit,
    /** Movimientos de cada miembro (uid → número), para avisar antes de borrarlos. */
    movementCounts: Map<String, Int> = emptyMap(),
    onRemoveMember: (member: Member, deleteMovements: Boolean) -> Unit = { _, _ -> },
    onSetJoinLocked: (locked: Boolean) -> Unit = {},
    /** Avisos de movimientos nuevos para mí en este grupo (null = no aplica, modo local). */
    notificationsEnabled: Boolean? = null,
    onSetNotifications: (enabled: Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val shareText = stringResource(R.string.group_share_text, group.name, group.inviteCode)
    val copiedText = stringResource(R.string.group_code_copied)
    var copied by rememberSaveable { mutableStateOf(false) }
    // Solo el creador del grupo gestiona a los miembros y si se admiten nuevos.
    val isOwner = isCloud && group.ownerId == currentUserId
    var removingUid by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.group_settings_title, group.name),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.group_edit))
                }
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (isCloud) {
                    Text(
                        stringResource(R.string.group_invite_code),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (group.joinLocked && !isOwner) {
                        Text(
                            stringResource(R.string.group_closed_member),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            group.inviteCode,
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 4.sp,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(enabled = !group.joinLocked, onClick = {
                            scope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(copiedText, group.inviteCode)))
                            }
                            copied = true
                        }) {
                            Icon(
                                if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                contentDescription = stringResource(R.string.action_copy),
                            )
                        }
                        IconButton(enabled = !group.joinLocked, onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }) {
                            Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.action_share))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(if (group.joinLocked) R.string.group_allow_join_off else R.string.group_invite_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    }
                    if (isOwner) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.group_allow_join), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(if (group.joinLocked) R.string.group_allow_join_off else R.string.group_allow_join_on),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(checked = !group.joinLocked, onCheckedChange = { onSetJoinLocked(!it) })
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                if (notificationsEnabled != null) {
                    val systemAllows = Notifications.enabled(context)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.group_notifications), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(
                                    if (notificationsEnabled) R.string.group_notifications_on else R.string.group_notifications_off,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(checked = notificationsEnabled, onCheckedChange = onSetNotifications)
                    }
                    if (notificationsEnabled && !systemAllows) {
                        TextButton(onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            runCatching { context.startActivity(intent) }
                        }) {
                            Text(stringResource(R.string.group_notifications_blocked), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                Text(
                    stringResource(R.string.group_participants),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                group.members.forEach { member ->
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        MemberAvatar(member.name)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            val you = if (member.uid == currentUserId) " " + stringResource(R.string.group_you) else ""
                            val owner = if (isCloud && member.uid == group.ownerId) " " + stringResource(R.string.group_owner) else ""
                            Text(
                                member.name + you + owner,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!member.email.isNullOrBlank()) {
                                Text(
                                    member.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (isOwner && member.uid != currentUserId) {
                            IconButton(onClick = { removingUid = member.uid }) {
                                Icon(
                                    Icons.Rounded.PersonRemove,
                                    contentDescription = stringResource(R.string.group_remove_member, member.name),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onLeave,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                ) {
                    Text(stringResource(if (isCloud) R.string.group_leave else R.string.group_delete))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )

    group.members.firstOrNull { it.uid == removingUid }?.let { member ->
        RemoveMemberDialog(
            member = member,
            movementCount = movementCounts[member.uid] ?: 0,
            groupOpen = !group.joinLocked,
            onDismiss = { removingUid = null },
            onConfirm = { deleteMovements ->
                removingUid = null
                onRemoveMember(member, deleteMovements)
            },
        )
    }
}

/** El creador quita a un miembro: elige si borrar también los movimientos que añadió. */
@Composable
fun RemoveMemberDialog(
    member: Member,
    movementCount: Int,
    groupOpen: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (deleteMovements: Boolean) -> Unit,
) {
    var deleteMovements by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.group_remove_title, member.name)) },
        text = {
            Column(Modifier.selectableGroup()) {
                RemoveOption(
                    selected = !deleteMovements,
                    title = stringResource(R.string.group_remove_keep),
                    body = stringResource(R.string.group_remove_keep_desc),
                    onClick = { deleteMovements = false },
                )
                RemoveOption(
                    selected = deleteMovements,
                    title = stringResource(R.string.group_remove_delete),
                    body = if (movementCount > 0) {
                        pluralStringResource(R.plurals.group_remove_delete_count, movementCount, movementCount)
                    } else {
                        stringResource(R.string.group_remove_delete_desc)
                    },
                    onClick = { deleteMovements = true },
                )
                if (groupOpen) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.group_remove_rejoin_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(deleteMovements) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.group_remove_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun RemoveOption(selected: Boolean, title: String, body: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Confirmación antes de salir de un grupo / eliminarlo. */
@Composable
fun LeaveGroupDialog(
    group: Group,
    isCloud: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val isLastMember = group.members.size <= 1
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(if (isCloud) R.string.group_leave_title else R.string.group_delete_title, group.name),
            )
        },
        text = {
            Text(
                stringResource(
                    when {
                        !isCloud -> R.string.group_delete_body
                        isLastMember -> R.string.group_leave_last_body
                        else -> R.string.group_leave_body
                    },
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(if (isCloud) R.string.group_leave else R.string.action_delete))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
