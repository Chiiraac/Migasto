package com.chiiraac.migasto.data.repository

import com.chiiraac.migasto.data.AppError
import com.chiiraac.migasto.data.model.Group
import com.chiiraac.migasto.data.model.GroupIcon
import com.chiiraac.migasto.data.model.Movement
import com.chiiraac.migasto.data.model.MovementDraft
import com.chiiraac.migasto.data.model.PhotoChange
import com.chiiraac.migasto.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: UserProfile) : AuthState
}

/** Prueba de identidad para operaciones sensibles (borrar la cuenta). */
sealed interface Reauth {
    data class Password(val password: String) : Reauth

    /** Token de Google recién obtenido con Credential Manager. */
    data class Google(val idToken: String) : Reauth
}

/**
 * Sesión del usuario. En modo nube usa Firebase Authentication; en modo local
 * simplemente guarda un perfil en el dispositivo.
 */
interface AuthRepository {
    val authState: StateFlow<AuthState>

    /** Crea el perfil local (solo modo local). */
    suspend fun startLocal(name: String): Result<UserProfile>

    suspend fun signIn(email: String, password: String): Result<Unit>

    /** Entra (o crea la cuenta) con el token de Google obtenido con Credential Manager. */
    suspend fun signInWithGoogle(idToken: String): Result<Unit>

    suspend fun register(name: String, email: String, password: String): Result<Unit>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    suspend fun updateName(name: String): Result<UserProfile>

    /** Confirma la identidad antes de operaciones sensibles (borrar cuenta). */
    suspend fun reauthenticate(proof: Reauth): Result<Unit>

    suspend fun signOut()

    /** Elimina las credenciales/perfil. Los datos de grupos se borran antes con [FinanceRepository.purgeUser]. */
    suspend fun deleteAccount(): Result<Unit>
}

/** Grupos y movimientos. */
interface FinanceRepository {
    fun observeGroups(user: UserProfile): Flow<List<Group>>

    fun observeMovements(groupId: String): Flow<List<Movement>>

    suspend fun createGroup(user: UserProfile, name: String, icon: GroupIcon): Result<Group>

    suspend fun joinGroup(user: UserProfile, inviteCode: String): Result<Group>

    /** Sale del grupo. Si era el último miembro, el grupo se elimina con todos sus datos. */
    suspend fun leaveGroup(user: UserProfile, group: Group): Result<Unit>

    suspend fun updateGroup(group: Group, name: String, icon: GroupIcon): Result<Unit>

    /**
     * Solo el creador (modo nube): quita a un miembro del grupo y, si [deleteMovements], borra
     * también los movimientos que añadió.
     */
    suspend fun removeMember(user: UserProfile, group: Group, memberUid: String, deleteMovements: Boolean): Result<Unit> =
        Result.failure(AppError(AppError.Reason.NOT_AVAILABLE_OFFLINE_MODE))

    /** Solo el creador (modo nube): cierra o vuelve a abrir el grupo a nuevos miembros. */
    suspend fun setJoinLocked(group: Group, locked: Boolean): Result<Unit> =
        Result.failure(AppError(AppError.Reason.NOT_AVAILABLE_OFFLINE_MODE))

    suspend fun saveMovement(
        user: UserProfile,
        groupId: String,
        movementId: String?,
        draft: MovementDraft,
        photo: PhotoChange,
    ): Result<Unit>

    suspend fun deleteMovement(movement: Movement): Result<Unit>

    suspend fun loadPhoto(movement: Movement): ByteArray?

    /** Actualiza el nombre/email visible del usuario en todos sus grupos. */
    suspend fun updateMemberProfile(user: UserProfile): Result<Unit>

    /** Sale de todos los grupos (borrando los que se queden vacíos). */
    suspend fun purgeUser(user: UserProfile): Result<Unit>

    /**
     * Espera (como mucho [timeoutMs]) a que los cambios guardados sin conexión lleguen al servidor.
     * Devuelve false si aún queda algo pendiente. En modo local no hay nada que enviar.
     */
    suspend fun flushPendingWrites(timeoutMs: Long): Boolean = true
}
