package com.chiiraac.migasto.data.remote

import com.chiiraac.migasto.data.AppError
import com.chiiraac.migasto.data.model.UserProfile
import com.chiiraac.migasto.data.repository.AuthRepository
import com.chiiraac.migasto.data.repository.AuthState
import com.chiiraac.migasto.data.runCatchingApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/** Modo nube: cuentas con email y contraseña mediante Firebase Authentication. */
class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {

    private val state = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = state.asStateFlow()

    /** Nombre elegido al registrarse, mientras Firebase aún no lo ha guardado en el perfil. */
    @Volatile
    private var pendingName: String? = null

    init {
        auth.addAuthStateListener { publish(it.currentUser) }
    }

    private fun publish(user: FirebaseUser?) {
        state.value = if (user == null) AuthState.SignedOut else AuthState.SignedIn(user.toProfile())
    }

    private fun FirebaseUser.toProfile(): UserProfile {
        val name = displayName?.takeIf { it.isNotBlank() }
            ?: pendingName
            ?: email?.substringBefore('@')
            ?: ""
        return UserProfile(uid = uid, name = name, email = email)
    }

    override suspend fun startLocal(name: String): Result<UserProfile> =
        Result.failure(UnsupportedOperationException("Solo disponible en modo local"))

    override suspend fun signIn(email: String, password: String): Result<Unit> = authCall {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    override suspend fun register(name: String, email: String, password: String): Result<Unit> = authCall {
        pendingName = name.trim()
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: throw AppError(AppError.Reason.UNKNOWN)
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
        publish(auth.currentUser)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = authCall {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    override suspend fun updateName(name: String): Result<UserProfile> = authCall {
        val user = auth.currentUser ?: throw AppError(AppError.Reason.PERMISSION_DENIED)
        pendingName = name.trim()
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
        val profile = user.toProfile().copy(name = name.trim())
        state.value = AuthState.SignedIn(profile)
        profile
    }

    override suspend fun reauthenticate(password: String): Result<Unit> = authCall {
        val user = auth.currentUser ?: throw AppError(AppError.Reason.PERMISSION_DENIED)
        val email = user.email ?: throw AppError(AppError.Reason.PERMISSION_DENIED)
        user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
    }

    override suspend fun signOut() {
        pendingName = null
        auth.signOut()
    }

    override suspend fun deleteAccount(): Result<Unit> = authCall {
        auth.currentUser?.delete()?.await()
        pendingName = null
    }

    private inline fun <T> authCall(block: () -> T): Result<T> =
        runCatchingApp(block).recoverCatching { throw FirebaseErrors.map(it) }
}
