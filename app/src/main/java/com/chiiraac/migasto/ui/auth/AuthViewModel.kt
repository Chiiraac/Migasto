package com.chiiraac.migasto.ui.auth

import android.app.Application
import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chiiraac.migasto.MiGastoApplication
import com.chiiraac.migasto.R
import com.chiiraac.migasto.data.AppError
import com.chiiraac.migasto.data.model.GroupIcon
import com.chiiraac.migasto.data.model.UserProfile
import com.chiiraac.migasto.data.repository.AuthRepository
import com.chiiraac.migasto.data.repository.AuthState
import com.chiiraac.migasto.data.repository.FinanceRepository
import com.chiiraac.migasto.ui.components.messageRes
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val registering: Boolean = false,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val groupName: String = "",
    val loading: Boolean = false,
    /** La carga en curso es la de "Continuar con Google" (el indicador va en ese botón). */
    val googleLoading: Boolean = false,
    @StringRes val error: Int? = null,
    val resetSentTo: String? = null,
) {
    val canSubmit: Boolean
        get() = !loading && email.isNotBlank() && password.length >= 6 && (!registering || name.isNotBlank())
}

class AuthViewModel(
    private val auth: AuthRepository,
    private val finance: FinanceRepository,
    private val defaultGroupName: String,
) : ViewModel() {

    private val state = MutableStateFlow(AuthUiState(groupName = defaultGroupName))
    val uiState: StateFlow<AuthUiState> = state.asStateFlow()

    init {
        // En cuanto hay sesión (aunque el registro terminase con un error parcial, p. ej. al
        // guardar el nombre), se vacía el formulario: al cerrar sesión no deben quedar el email
        // ni la contraseña de la persona anterior.
        viewModelScope.launch {
            auth.authState.collect { if (it is AuthState.SignedIn) state.value = AuthUiState(groupName = defaultGroupName) }
        }
    }

    fun setName(value: String) = state.update { it.copy(name = value.take(40), error = null) }
    fun setEmail(value: String) = state.update { it.copy(email = value.trim().take(120), error = null, resetSentTo = null) }
    fun setPassword(value: String) = state.update { it.copy(password = value.take(128), error = null) }
    fun setGroupName(value: String) = state.update { it.copy(groupName = value.take(30), error = null) }
    fun toggleMode() = state.update { it.copy(registering = !it.registering, error = null, resetSentTo = null) }

    fun submit() {
        val current = state.value
        if (!current.canSubmit) return
        if (!Patterns.EMAIL_ADDRESS.matcher(current.email).matches()) {
            state.update { it.copy(error = R.string.error_invalid_email) }
            return
        }
        state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = if (current.registering) {
                auth.register(current.name, current.email, current.password)
            } else {
                auth.signIn(current.email, current.password)
            }
            // Si va bien, el cambio de sesión lleva a la pantalla principal automáticamente.
            // El formulario se vacía para que, al cerrar sesión, no queden el email ni la contraseña.
            // Si ya hay sesión (p. ej. la cuenta se creó pero falló guardar el nombre), el error
            // no se muestra: el formulario debe quedar vacío para la próxima vez.
            if (result.isSuccess || auth.authState.value is AuthState.SignedIn) {
                state.value = AuthUiState(groupName = defaultGroupName)
            } else {
                state.update { it.copy(loading = false, error = result.exceptionOrNull()?.messageRes()) }
            }
        }
    }

    /**
     * Empieza "Continuar con Google". El selector de cuentas lo abre la pantalla (necesita la
     * Activity) y su resultado llega a [finishGoogle]. Devuelve false si ya hay algo en marcha.
     */
    fun startGoogle(): Boolean {
        if (state.value.loading) return false
        state.update { it.copy(loading = true, googleLoading = true, error = null, resetSentTo = null) }
        return true
    }

    /** Recibe el token de Google (o el motivo por el que no se obtuvo) y abre la sesión. */
    fun finishGoogle(token: Result<String>) {
        val idToken = token.getOrElse { error ->
            val cancelled = (error as? AppError)?.reason == AppError.Reason.CANCELLED
            state.update { it.copy(loading = false, googleLoading = false, error = if (cancelled) null else error.messageRes()) }
            return
        }
        viewModelScope.launch {
            val result = auth.signInWithGoogle(idToken)
            if (result.isSuccess || auth.authState.value is AuthState.SignedIn) {
                state.value = AuthUiState(groupName = defaultGroupName)
            } else {
                state.update { it.copy(loading = false, googleLoading = false, error = result.exceptionOrNull()?.messageRes()) }
            }
        }
    }

    fun sendPasswordReset() {
        val email = state.value.email
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            state.update { it.copy(error = R.string.auth_reset_need_email) }
            return
        }
        state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = auth.sendPasswordReset(email)
            state.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.messageRes(),
                    resetSentTo = if (result.isSuccess) email else null,
                )
            }
        }
    }

    /** Modo local: crea el primer grupo y guarda el perfil. */
    fun startLocal() {
        val current = state.value
        if (current.name.isBlank() || current.loading) return
        state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val groupName = current.groupName.ifBlank { defaultGroupName }
            val provisional = UserProfile(UUID.randomUUID().toString(), current.name.trim(), null)
            val result = finance.createGroup(provisional, groupName, GroupIcon.HOME)
                .mapCatching { auth.startLocal(current.name).getOrThrow() }
            // Si ya hay sesión (p. ej. la cuenta se creó pero falló guardar el nombre), el error
            // no se muestra: el formulario debe quedar vacío para la próxima vez.
            if (result.isSuccess || auth.authState.value is AuthState.SignedIn) {
                state.value = AuthUiState(groupName = defaultGroupName)
            } else {
                state.update { it.copy(loading = false, error = result.exceptionOrNull()?.messageRes()) }
            }
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (application as MiGastoApplication).container
                AuthViewModel(
                    auth = container.authRepository,
                    finance = container.financeRepository,
                    defaultGroupName = application.getString(R.string.default_group_name),
                )
            }
        }
    }
}
