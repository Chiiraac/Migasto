package com.chiiraac.migasto.ui.movement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.chiiraac.migasto.R
import com.chiiraac.migasto.data.model.Movement
import com.chiiraac.migasto.data.model.MovementType
import com.chiiraac.migasto.domain.Dates
import com.chiiraac.migasto.ui.components.Categories
import com.chiiraac.migasto.ui.components.CategoryBadge
import com.chiiraac.migasto.ui.components.color
import com.chiiraac.migasto.ui.components.label
import com.chiiraac.migasto.ui.components.movementAmountText
import com.chiiraac.migasto.ui.components.transferLabel

/** Detalle de un movimiento con su foto y las acciones de editar/eliminar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementDetailSheet(
    movement: Movement,
    authorName: String?,
    loadPhoto: suspend (Movement) -> ByteArray?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var photo by remember(movement.id) { mutableStateOf<ByteArray?>(null) }
    var photoLoading by remember(movement.id) { mutableStateOf(movement.hasPhoto) }
    var showViewer by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(movement.id, movement.hasPhoto) {
        if (movement.hasPhoto) {
            photoLoading = true
            photo = loadPhoto(movement)
            photoLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CategoryBadge(movement.type, movement.categoryId, size = 64.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                movement.description.ifBlank { stringResource(R.string.movement_no_description) },
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                movementAmountText(movement),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = movement.type.color(),
            )
            Spacer(Modifier.height(20.dp))

            DetailRow(stringResource(R.string.movement_type), stringResource(movement.type.label))
            if (movement.type != MovementType.TRANSFER) {
                DetailRow(stringResource(R.string.movement_category), stringResource(Categories.byId(movement.categoryId).label))
            }
            DetailRow(
                stringResource(R.string.movement_account),
                stringResource(if (movement.type == MovementType.TRANSFER) movement.method.transferLabel else movement.method.label),
            )
            DetailRow(stringResource(R.string.movement_date), Dates.longDay(movement.date))
            if (!authorName.isNullOrBlank()) {
                DetailRow(stringResource(R.string.movement_created_by), authorName)
            }

            if (movement.hasPhoto) {
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.movement_photo),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable(enabled = photo != null) { showViewer = true },
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        photo != null -> AsyncImage(
                            model = photo,
                            contentDescription = stringResource(R.string.movement_photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        photoLoading -> CircularProgressIndicator()
                        else -> Text(
                            stringResource(R.string.error_photo_load),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_edit))
                }
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.movement_delete_title)) },
            text = { Text(stringResource(R.string.movement_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    val bytes = photo
    if (showViewer && bytes != null) {
        PhotoViewer(bytes, onDismiss = { showViewer = false })
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(16.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.5f),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Visor a pantalla completa con zoom (pellizcar) para la foto del ticket. */
@Composable
fun PhotoViewer(bytes: ByteArray, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformState = rememberTransformableState { _, zoom, pan, _ ->
            scale = (scale * zoom).coerceIn(1f, 5f)
            offset = if (scale == 1f) Offset.Zero else offset + pan
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            AsyncImage(
                model = bytes,
                contentDescription = stringResource(R.string.movement_photo),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(transformState)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close), tint = Color.White)
            }
        }
    }
}
