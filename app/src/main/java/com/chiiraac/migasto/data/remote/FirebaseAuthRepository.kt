package com.chiiraac.migasto.data.remote

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.chiiraac.migasto.data.AppError
import com.chiiraac.migasto.data.model.UserProfile
import com.chiiraac.migasto.data.repository.AuthRepository
import com.chiiraac.migasto.data.repository.AuthState
import com.chiiraac.migasto.data.repository.Reauth
import com.chiiraac.migasto.data.runCatchingApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Nombres elegidos al registrarse que Firebase aún no ha guardado en el perfil (por ejemplo,
 * porque se cortó la conexión justo después de crear la cuenta). Se guardan en disco para
 * no perderlos al cerrar la app y volver a intentarlo en la siguiente sesión.
 */
interface PendingNames {
    fun get(uid: String): String?
    fun put(uid: String, name: String)
    fun remove(uid: String)

    class InMemory : PendingNames {
        private val names = ConcurrentHashMap<String, String>()
        override fun get(uid: String) = names[uid]
        override fun put(uid: String, name: String) { names[uid] = name }
        override fun remove(uid: String) { names.remove(uid) }
    }

    class Stored(private val prefs: SharedPreferences) : PendingNames {
        override fun get(uid: String) = prefs.getString(key(uid), null)
        override fun put(uid: String, name: String) = prefs.edit { putString(key(uid), name) }
        override fun remove(uid: String) = prefs.edit { remove(key(uid)) }
        private fun key(uid: String) = "pending_name_$uid"
    }
}

/** Modo nube: cuentas con email y contraseña o con Google mediante Firebase Authentication. */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val pendingNames: PendingNames = PendingNames.InMemory(),
) : AuthRepository {

    private val state = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = state.asStateFlow()

    /** Nombre elegido al registrarse, mientras Firebase aún no lo ha guardado en el perfil. */
    @Volatile
    private var pendingName: String? = null

    @Volatile
    private var retryingNameFor: String? = null

    init {
        auth.addAuthStateListener { publish(it.currentUser) }
    }

    private fun publish(user: FirebaseUser?) {
        state.value = if (user == null) AuthState.SignedOut else AuthState.SignedIn(user.toProfile())
        if (user != null) retryPendingName(user)
    }

    private fun FirebaseUser.toProfile(): UserProfile {
        val name = displayName?.takeIf { it.isNotBlank() }
            ?: pendingName
            ?: pendingNames.get(uid)
            ?: email?.substringBefore('@')
            ?: ""
        return UserProfile(
            uid = uid,
            name = name,
            email = email,
            usesGoogle = hasProvider(GoogleAuthProvider.PROVIDER_ID),
            hasPassword = hasProvider(EmailAuthProvider.PROVIDER_ID),
        )
    }

    private fun FirebaseUser.hasProvider(providerId: String): Boolean =
        providerData.any { it.providerId == providerId }

    /** Si al registrarse no se llegó a guardar el nombre, se vuelve a intentar (sin bloquear). */
    private fun retryPendingName(user: FirebaseUser) {
        val saved = pendingNames.get(user.uid) ?: return
        if (!user.displayName.isNullOrBlank()) {
            pendingNames.remove(user.uid)
            return
        }
        if (retryingNameFor == user.uid) return
        retryingNameFor = user.uid
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(saved).build())
            .addOnCompleteListener { task ->
                retryingNameFor = null
                if (task.isSuccessful) {
                    pendingNames.remove(user.uid)
                    auth.currentUser?.takeIf { it.uid == user.uid }?.let(::publish)
                }
            }
    }

    override suspend fun startLocal(name: String): Result<UserProfile> =
        Result.failure(UnsupportedOperationException("Solo disponible en modo local"))

    override suspend fun signIn(email: String, password: String): Result<Unit> = authCall {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = googleCall {
        pendingName = null
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        // Si Google acaba de vincularse a una cuenta que ya existía, el perfil cambia de proveedor.
        publish(auth.currentUser)
    }

    override suspend fun register(name: String, email: String, password: String): Result<Unit> = authCall {
        val trimmed = name.trim()
        pendingName = trimmed
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: throw AppError(AppError.Reason.UNKNOWN)
        pendingNames.put(user.uid, trimmed)
        // Email de verificación (sin esperar): con el email verificado, si un día entra con Google
        // usando ese mismo Gmail, Firebase añade Google a la cuenta en vez de quitarle la contraseña.
        auth.useAppLanguage()
        user.sendEmailVerification().addOnFailureListener { Log.w(TAG, "Verification email not sent", it) }
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(trimmed).build()).await()
        pendingNames.remove(user.uid)
        publish(auth.currentUser)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = authCall {
        // El email y la página para cambiar la contraseña salen en el idioma de la app.
        auth.useAppLanguage()
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    override suspend fun updateName(name: String): Result<UserProfile> = authCall {
        val user = auth.currentUser ?: throw AppError(AppError.Reason.PERMISSION_DENIED)
        pendingName = name.trim()
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
        pendingNames.remove(user.uid)
        val profile = user.toProfile().copy(name = name.trim())
        state.value = AuthState.SignedIn(profile)
        profile
    }

    override suspend fun reauthenticate(proof: Reauth): Result<Unit> = when (proof) {
        is Reauth.Password -> authCall {
            val user = auth.currentUser ?: throw AppError(AppError.Reason.PERMISSION_DENIED)
            val email = user.email ?: throw AppError(AppError.Reason.PERMISSION_DENIED)
            user.reauthenticate(EmailAuthProvider.getCredential(email, proof.password)).await()
        }
        is Reauth.Google -> googleCall {
            val user = auth.currentUser ?: throw AppError(AppError.Reason.PERMISSION_DENIED)
            user.reauthenticate(GoogleAuthProvider.getCredential(proof.idToken, null)).await()
        }
    }

    override suspend fun signOut() {
        pendingName = null
        auth.signOut()
    }

    override suspend fun deleteAccount(): Result<Unit> = authCall {
        val user = auth.currentUser
        user?.delete()?.await()
        user?.uid?.let(pendingNames::remove)
        pendingName = null
    }

    private inline fun <T> authCall(block: () -> T): Result<T> =
        runCatchingApp(block).recoverCatching { throw FirebaseErrors.map(it.logged()) }

    private inline fun <T> googleCall(block: () -> T): Result<T> =
        runCatchingApp(block).recoverCatching { throw FirebaseErrors.mapGoogle(it.logged()) }

    /** Los errores inesperados se registran para poder diagnosticarlos con `adb logcat`. */
    private fun Throwable.logged(): Throwable = also { if (it !is AppError) Log.w(TAG, "Auth error", it) }

    private companion object {
        const val TAG = "MiGastoAuth"
    }
}
