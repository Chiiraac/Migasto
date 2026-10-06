package com.chiiraac.migasto.ui.movement

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.chiiraac.migasto.R
import com.chiiraac.migasto.data.model.Movement
import com.chiiraac.migasto.data.model.MovementDraft
import com.chiiraac.migasto.data.model.MovementType
import com.chiiraac.migasto.data.model.PaymentMethod
import com.chiiraac.migasto.domain.Dates
import com.chiiraac.migasto.domain.Money
import com.chiiraac.migasto.ui.components.Categories
import com.chiiraac.migasto.ui.components.CategoryInfo
import com.chiiraac.migasto.ui.components.color
import com.chiiraac.migasto.ui.components.label
import com.chiiraac.migasto.ui.components.today
import com.chiiraac.migasto.ui.components.transferLabel
import com.chiiraac.migasto.util.TempFiles
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch

/**
 * Hoja "Añadir Movimiento" / "Editar Movimiento".
 *
 * [onSave] recibe el borrador, la nueva foto (si se eligió) y si hay que quitar la existente;
 * debe llamar al callback con `true` cuando se haya guardado para cerrar la hoja.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementEditorSheet(
    existing: Movement?,
    initialDate: LocalDate,
    loadPhoto: suspend (Movement) -> ByteArray?,
    onSave: (draft: MovementDraft, newPhoto: Uri?, removePhoto: Boolean, onDone: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        MovementEditorContent(existing, initialDate, loadPhoto, onSave, onDismiss)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementEditorContent(
    existing: Movement?,
    initialDate: LocalDate,
    loadPhoto: suspend (Movement) -> ByteArray?,
    onSave: (draft: MovementDraft, newPhoto: Uri?, removePhoto: Boolean, onDone: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val todayDate = today()

    var type by rememberSaveable { mutableStateOf(existing?.type ?: MovementType.EXPENSE) }
    var method by rememberSaveable { mutableStateOf(existing?.method ?: PaymentMethod.BANK) }
    var amountText by rememberSaveable { mutableStateOf(existing?.let { Money.toInput(it.amountCents) } ?: "") }
    var description by rememberSaveable { mutableStateOf(existing?.description ?: "") }
    var categoryId by rememberSaveable {
        mutableStateOf(existing?.categoryId?.takeIf { Categories.isValidFor(it, existing.type) })
    }
    var epochDay by rememberSaveable { mutableLongStateOf((existing?.date ?: initialDate).toEpochDay()) }
    var newPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var removeExisting by rememberSaveable { mutableStateOf(false) }
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    // No se guarda en el Bundle: tras rotar la pantalla el formulario debe quedar utilizable.
    var saving by remember { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showCategoryPicker by rememberSaveable { mutableStateOf(false) }
    var showPhotoMenu by remember { mutableStateOf(false) }
    var existingPhoto by remember { mutableStateOf<ByteArray?>(null) }
    var existingPhotoLoading by remember { mutableStateOf(existing?.hasPhoto == true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(existing?.id) {
        if (existing?.hasPhoto == true) {
            existingPhotoLoading = true
            existingPhoto = loadPhoto(existing)
            existingPhotoLoading = false
        }
    }

    /** Cambia la foto nueva y borra la captura de cámara que se descarta. */
    fun replaceNewPhoto(value: String?) {
        val previous = newPhoto
        newPhoto = value
        if (previous != null && previous != value) {
            scope.launch { TempFiles.deleteCameraCapture(context, previous.toUri()) }
        }
    }

    val date = LocalDate.ofEpochDay(epochDay)
    val amountCents = Money.parseToCents(amountText)
    val effectiveCategory = if (type == MovementType.TRANSFER) Categories.TRANSFER_ID else categoryId
    val canSave = !saving && amountCents != null && amountCents > 0 && effectiveCategory != null

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val captured = pendingCameraUri
        if (success && captured != null) {
            replaceNewPhoto(captured)
            removeExisting = false
        } else if (captured != null) {
            scope.launch { TempFiles.deleteCameraCapture(context, captured.toUri()) }
        }
        pendingCameraUri = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            replaceNewPhoto(uri.toString())
            removeExisting = false
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(if (existing == null) R.string.movement_add_title else R.string.movement_edit_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        // Tipo de movimiento
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MovementType.entries.forEach { option ->
                ToggleButton(
                    text = stringResource(option.label),
                    selected = type == option,
                    selectedColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        type = option
                        if (categoryId != null && !Categories.isValidFor(categoryId!!, option)) categoryId = null
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Cuenta
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PaymentMethod.entries.forEach { option ->
                ToggleButton(
                    text = stringResource(if (type == MovementType.TRANSFER) option.transferLabel else option.label),
                    selected = method == option,
                    selectedColor = MaterialTheme.colorScheme.secondary,
                    onClick = { method = option },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Cantidad + foto
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            val amountInvalid = amountText.isNotBlank() && (amountCents == null || amountCents == 0L)
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = sanitizeAmount(it) },
                label = { Text(stringResource(R.string.movement_amount)) },
                // Muestra cómo se ha interpretado el importe (p. ej. "1.500" → 1.500,00 €).
                supportingText = when {
                    amountInvalid -> {
                        { Text(stringResource(R.string.error_amount_invalid)) }
                    }
                    amountCents != null && amountCents > 0 -> {
                        { Text(Money.format(amountCents), maxLines = 1) }
                    }
                    else -> null
                },
                isError = amountInvalid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                textStyle = MaterialTheme.typography.titleLarge,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.weight(1f)) {
                val photoModel: Any? = newPhoto?.let(Uri::parse) ?: existingPhoto.takeIf { !removeExisting }
                val hasPhoto = newPhoto != null || (existing?.hasPhoto == true && !removeExisting)
                if (hasPhoto) {
                    PhotoThumbnail(
                        model = photoModel,
                        loading = newPhoto == null && existingPhotoLoading,
                        onClick = { showPhotoMenu = true },
                        onRemove = {
                            replaceNewPhoto(null)
                            removeExisting = existing?.hasPhoto == true
                        },
                    )
                } else {
                    Surface(
                        onClick = { showPhotoMenu = true },
                        shape = MaterialTheme.shapes.large,
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.movement_add_photo),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                DropdownMenu(expanded = showPhotoMenu, onDismissRequest = { showPhotoMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.movement_take_photo)) },
                        leadingIcon = { Icon(Icons.Rounded.PhotoCamera, contentDescription = null) },
                        onClick = {
                            showPhotoMenu = false
                            val uri = createCameraUri(context)
                            pendingCameraUri = uri.toString()
                            try {
                                cameraLauncher.launch(uri)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, R.string.error_camera_unavailable, Toast.LENGTH_LONG).show()
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.movement_pick_photo)) },
                        leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null) },
                        onClick = {
                            showPhotoMenu = false
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it.take(80) },
            label = { Text(stringResource(R.string.movement_description)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        )

        if (type != MovementType.TRANSFER) {
            val selectedCategory = categoryId?.let(Categories::byId)
            PickerField(
                label = stringResource(R.string.movement_category),
                value = selectedCategory?.let { stringResource(it.label) },
                leading = selectedCategory?.let { { Icon(it.icon, contentDescription = null, tint = it.color) } },
                onClick = { showCategoryPicker = true },
            )
        }

        PickerField(
            label = stringResource(R.string.movement_date),
            value = when (date) {
                todayDate -> stringResource(R.string.movement_today) + " · " + Dates.mediumDay(date)
                todayDate.minusDays(1) -> stringResource(R.string.movement_yesterday) + " · " + Dates.mediumDay(date)
                else -> Dates.longDay(date)
            },
            leading = { Icon(Icons.Rounded.CalendarToday, contentDescription = null) },
            onClick = { showDatePicker = true },
        )

        Button(
            onClick = {
                val cents = amountCents ?: return@Button
                val category = effectiveCategory ?: return@Button
                saving = true
                onSave(
                    MovementDraft(
                        type = type,
                        method = method,
                        amountCents = cents,
                        description = description.trim(),
                        categoryId = category,
                        date = date,
                    ),
                    newPhoto?.let(Uri::parse),
                    removeExisting,
                ) { success ->
                    saving = false
                    if (success) onDismiss()
                }
            },
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        ) {
            if (saving) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondary)
            } else {
                Text(stringResource(R.string.movement_save), style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        epochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showCategoryPicker) {
        CategoryPickerDialog(
            categories = Categories.forType(type),
            selectedId = categoryId,
            type = type,
            onSelect = {
                categoryId = it.id
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
        )
    }
}

/**
 * Deja solo dígitos y separadores ("," o "."), sin reinterpretarlos mientras se escribe:
 * así "1.500" llega entero a [Money.parseToCents], que distingue miles de decimales.
 */
internal fun sanitizeAmount(input: String): String =
    input.filter { it.isDigit() || it == ',' || it == '.' }.take(MAX_AMOUNT_LENGTH)

private const val MAX_AMOUNT_LENGTH = 14

private fun createCameraUri(context: Context): Uri {
    val directory = TempFiles.cameraDir(context).apply { mkdirs() }
    val file = File(directory, "ticket_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
private fun ToggleButton(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) selectedColor else Color.Transparent,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .height(52.dp)
            .semantics {
                this.selected = selected
                role = Role.RadioButton
            },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Campo de solo lectura que abre un selector al tocarlo. */
@Composable
private fun PickerField(
    label: String,
    value: String?,
    leading: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                if (value == null) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PhotoThumbnail(model: Any?, loading: Boolean, onClick: () -> Unit, onRemove: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(MaterialTheme.shapes.large)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = stringResource(R.string.movement_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center).size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                Icons.Rounded.BrokenImage,
                contentDescription = stringResource(R.string.error_photo_load),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.movement_remove_photo),
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun CategoryPickerDialog(
    categories: List<CategoryInfo>,
    selectedId: String?,
    type: MovementType,
    onSelect: (CategoryInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.movement_choose_category)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 84.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.heightIn(max = 420.dp),
            ) {
                items(categories, key = { it.id }) { category ->
                    val selected = category.id == selectedId
                    val tint = if (selected) type.color() else category.color
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) tint.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { onSelect(category) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(category.color.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(category.icon, contentDescription = null, tint = category.color)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(category.label),
                            style = MaterialTheme.typography.labelMedium.copy(hyphens = Hyphens.Auto),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
