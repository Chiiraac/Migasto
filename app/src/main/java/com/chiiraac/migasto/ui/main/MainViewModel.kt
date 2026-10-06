package com.chiiraac.migasto.ui.main

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chiiraac.migasto.MiGastoApplication
import com.chiiraac.migasto.R
import com.chiiraac.migasto.data.AppError
import com.chiiraac.migasto.data.model.Group
import com.chiiraac.migasto.data.model.GroupIcon
import com.chiiraac.migasto.data.model.Movement
import com.chiiraac.migasto.data.model.MovementDraft
import com.chiiraac.migasto.data.model.PhotoChange
import com.chiiraac.migasto.data.model.ThemeMode
import com.chiiraac.migasto.data.model.UserProfile
import com.chiiraac.migasto.data.prefs.UserPreferences
import com.chiiraac.migasto.data.repository.AuthRepository
import com.chiiraac.migasto.data.repository.AuthState
import com.chiiraac.migasto.data.repository.FinanceRepository
import com.chiiraac.migasto.ui.components.messageRes
import com.chiiraac.migasto.util.PhotoProcessor
import com.chiiraac.migasto.util.TempFiles
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val user: UserProfile? = null,
    val isCloud: Boolean = false,
    val groups: List<Group> = emptyList(),
    val groupsLoaded: Boolean = false,
    val selectedGroup: Group? = null,
    val movements: List<Movement> = emptyList(),
    val movementsLoaded: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Último editor (por su identificador) cuyo movimiento se guardó: la pantalla lo cierra. */
    val savedEditorToken: Long? = null,
    /** Editores cuyo guardado sigue en curso (sobrevive a la recreación de la actividad). */
    val savingEditorTokens: Set<Long> = emptySet(),
) {
    /** Nombre a mostrar del autor de un movimiento (actualizado si cambió su nombre). */
    fun authorName(movement: Movement): String =
        selectedGroup?.memberName(movement.createdById)?.takeIf { it.isNotBlank() }
            ?: movement.createdByName
}

/** Mensaje puntual para mostrar en un snackbar. */
data class UiMessage(@StringRes val text: Int, val args: List<Any> = emptyList())

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val application: Application,
    private val auth: AuthRepository,
    private val finance: FinanceRepository,
    private val preferences: UserPreferences,
    private val isCloud: Boolean,
) : ViewModel() {

    private val messageChannel = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = messageChannel.receiveAsFlow()

    private val user: StateFlow<UserProfile?> = auth.authState
        .map { (it as? AuthState.SignedIn)?.user }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, (auth.authState.value as? AuthState.SignedIn)?.user)

    // Todos los flujos son "calientes" mientras viva el ViewModel: al volver de segundo plano
    // no se pierde el estado cargado (antes se reiniciaba y podía cerrar el editor abierto).
    private val groups: StateFlow<List<Group>?> = user.flatMapLatest { profile ->
        if (profile == null) {
            flowOf(null)
        } else {
            finance.observeGroups(profile)
                .retryWithBackoff()
                .map<List<Group>, List<Group>?> { it }
                .onStart { emit(null) }
                .catch { emit(emptyList()) }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val selectedGroupId: Flow<String?> = user.flatMapLatest { profile ->
        if (profile == null) flowOf(null) else preferences.selectedGroupId(profile.uid)
    }

    private val selectedGroup: StateFlow<Group?> = combine(groups, selectedGroupId) { list, id ->
        list?.firstOrNull { it.id == id } ?: list?.firstOrNull()
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val movements: StateFlow<List<Movement>?> = selectedGroup
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { groupId ->
            if (groupId == null) {
                flowOf(emptyList())
            } else {
                // En modo nube, un grupo recién creado puede no existir aún en el servidor cuando
                // empieza la escucha (las reglas la rechazan): se reintenta en lugar de quedarse vacía.
                finance.observeMovements(groupId)
                    .retryWithBackoff()
                    .map<List<Movement>, List<Movement>?> { it }
                    .onStart { emit(null) }
                    .catch { emit(emptyList()) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val savedEditorToken = MutableStateFlow<Long?>(null)
    private val savingEditorTokens = MutableStateFlow<Set<Long>>(emptySet())

    private data class EditorState(val theme: ThemeMode, val saved: Long?, val saving: Set<Long>)

    private val themeAndEditor = combine(preferences.themeMode, savedEditorToken, savingEditorTokens) { theme, saved, saving ->
        EditorState(theme, saved, saving)
    }

    val uiState: StateFlow<MainUiState> = combine(
        user,
        groups,
        selectedGroup,
        movements,
        themeAndEditor,
    ) { profile, groupList, selected, movementList, editor ->
        MainUiState(
            user = profile,
            isCloud = isCloud,
            groups = groupList.orEmpty(),
            groupsLoaded = groupList != null,
            selectedGroup = selected,
            movements = movementList.orEmpty(),
            movementsLoaded = movementList != null,
            themeMode = editor.theme,
            savedEditorToken = editor.saved,
            savingEditorTokens = editor.saving,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState(isCloud = isCloud))

    private fun currentUser(): UserProfile? = user.value

    private fun post(@StringRes text: Int, vararg args: Any) {
        messageChannel.trySend(UiMessage(text, args.toList()))
    }

    private fun postError(error: Throwable) = post(error.messageRes())

    // ---------- Grupos ----------

    fun selectGroup(groupId: String) {
        val profile = currentUser() ?: return
        viewModelScope.launch { preferences.setSelectedGroupId(profile.uid, groupId) }
    }

    fun createGroup(name: String, icon: GroupIcon, onDone: () -> Unit = {}) {
        val profile = currentUser() ?: return
        viewModelScope.launch {
            finance.createGroup(profile, name, icon)
                .onSuccess { group ->
                    preferences.setSelectedGroupId(profile.uid, group.id)
                    post(R.string.group_created, group.name)
                }
                .onFailure(::postError)
            onDone()
        }
    }

    fun joinGroup(code: String, onResult: (Throwable?) -> Unit) {
        val profile = currentUser() ?: return
        viewModelScope.launch {
            finance.joinGroup(profile, code)
                .onSuccess { group ->
                    preferences.setSelectedGroupId(profile.uid, group.id)
                    post(R.string.group_joined, group.name)
                    onResult(null)
                }
                .onFailure { onResult(it) }
        }
    }

    fun leaveGroup(group: Group) {
        val profile = currentUser() ?: return
        viewModelScope.launch {
            finance.leaveGroup(profile, group)
                .onSuccess { post(if (isCloud) R.string.group_left else R.string.group_deleted, group.name) }
                .onFailure(::postError)
        }
    }

    fun updateGroup(group: Group, name: String, icon: GroupIcon) {
        viewModelScope.launch {
            finance.updateGroup(group, name, icon).onFailure(::postError)
        }
    }

    // ---------- Movimientos ----------

    /**
     * Guarda un movimiento. [newPhoto] es la imagen elegida por el usuario (se comprime aquí);
     * [removePhoto] indica que se quiere quitar la foto existente.
     */
    fun saveMovement(
        editorToken: Long,
        movementId: String?,
        draft: MovementDraft,
        newPhoto: Uri?,
        removePhoto: Boolean,
        onDone: (Boolean) -> Unit,
    ) {
        val profile = currentUser() ?: return onDone(false)
        val groupId = uiState.value.selectedGroup?.id ?: return onDone(false)
        // Un mismo editor no puede guardar dos veces a la vez (p. ej. tras girar la pantalla).
        if (editorToken in savingEditorTokens.value) return onDone(false)
        savingEditorTokens.update { it + editorToken }
        viewModelScope.launch {
            try {
                val photoChange: PhotoChange = when {
                    newPhoto != null -> {
                        val bytes = runCatching { PhotoProcessor.compress(application, newPhoto) }.getOrNull()
                        if (bytes == null) {
                            post(R.string.error_photo_load)
                            onDone(false)
                            return@launch
                        }
                        PhotoChange.Replace(bytes)
                    }
                    removePhoto -> PhotoChange.Remove
                    else -> PhotoChange.Keep
                }
                finance.saveMovement(profile, groupId, movementId, draft, photoChange)
                    .onSuccess {
                        // La foto original de la cámara solo se borra cuando ya está guardada.
                        newPhoto?.let { TempFiles.deleteCameraCapture(application, it) }
                        post(if (movementId == null) R.string.movement_saved else R.string.movement_updated)
                        // Se guarda en el estado del ViewModel para cerrar el editor aunque la
                        // actividad se haya recreado (p. ej. al girar la pantalla) mientras se guardaba.
                        savedEditorToken.value = editorToken
                        onDone(true)
                    }
                    .onFailure {
                        postError(it)
                        onDone(false)
                    }
            } finally {
                savingEditorTokens.update { it - editorToken }
            }
        }
    }

    fun deleteMovement(movement: Movement) {
        viewModelScope.launch {
            finance.deleteMovement(movement)
                .onSuccess { post(R.string.movement_deleted) }
                .onFailure(::postError)
        }
    }

    suspend fun loadPhoto(movement: Movement): ByteArray? = finance.loadPhoto(movement)

    // ---------- Ajustes ----------

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun updateName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            auth.updateName(trimmed)
                .onSuccess { profile ->
                    finance.updateMemberProfile(profile)
                    post(R.string.settings_name_updated)
                }
                .onFailure(::postError)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            TempFiles.clearAll(application)
            auth.signOut()
        }
    }

    /** Borra la cuenta (modo nube) o todos los datos del dispositivo (modo local). */
    fun deleteAccount(password: String?, onResult: (Throwable?) -> Unit) {
        val profile = currentUser() ?: return
        viewModelScope.launch {
            val result = runCatching {
                if (isCloud) {
                    if (password.isNullOrEmpty()) throw AppError(AppError.Reason.WRONG_CREDENTIALS)
                    auth.reauthenticate(password).getOrThrow()
                }
                finance.purgeUser(profile).getOrThrow()
                TempFiles.clearAll(application)
                auth.deleteAccount().getOrThrow()
                preferences.clearAccountData()
            }
            onResult(result.exceptionOrNull())
        }
    }

    companion object {
        private const val MAX_LISTEN_RETRIES = 6

        /** Reintenta una escucha fallida (500 ms, 1 s, 2 s… ~30 s en total) antes de rendirse. */
        private fun <T> Flow<T>.retryWithBackoff(): Flow<T> = retryWhen { _, attempt ->
            if (attempt < MAX_LISTEN_RETRIES) {
                delay(500L shl attempt.toInt())
                true
            } else {
                false
            }
        }

        fun factory(application: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (application as MiGastoApplication).container
                MainViewModel(
                    application = application,
                    auth = container.authRepository,
                    finance = container.financeRepository,
                    preferences = container.preferences,
                    isCloud = container.isCloud,
                )
            }
        }
    }
}
