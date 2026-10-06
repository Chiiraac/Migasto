package com.chiiraac.migasto.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.chiiraac.migasto.BuildConfig
import com.chiiraac.migasto.data.AppError
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.withTimeoutOrNull

/**
 * "Continuar con Google" mediante Credential Manager. Devuelve el token de Google que luego
 * se cambia por una sesión de Firebase ([com.chiiraac.migasto.data.repository.AuthRepository.signInWithGoogle]).
 */
object GoogleSignIn {
    /** El botón solo aparece si la compilación tiene el ID de cliente web de Firebase. */
    val isConfigured: Boolean
        get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /** Abre el selector de cuentas de Google. Hay que llamarlo con el contexto de la Activity. */
    suspend fun requestIdToken(context: Context): Result<String> {
        if (!isConfigured) return Result.failure(AppError(AppError.Reason.GOOGLE_NOT_CONFIGURED))
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build())
            .build()
        return try {
            val activityContext = context.findActivity() ?: context
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                Result.success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Result.failure(AppError(AppError.Reason.GOOGLE_FAILED))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e !is GetCredentialCancellationException) Log.w(TAG, "Google sign-in failed", e)
            Result.failure(map(e))
        }
    }

    /** Al cerrar sesión, olvida la cuenta elegida para que la próxima vez se pueda elegir otra. */
    suspend fun clearSession(context: Context) {
        if (!isConfigured) return
        try {
            // Con límite de tiempo: cerrar sesión nunca debe quedarse esperando a Google Play Services.
            withTimeoutOrNull(CLEAR_TIMEOUT_MS) {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "clearCredentialState failed", e)
        }
    }

    internal fun map(error: Throwable): AppError {
        val message = error.message.orEmpty()
        val reason = when {
            error is GetCredentialCancellationException -> AppError.Reason.CANCELLED
            // Falta registrar la huella SHA-1 de la firma en Firebase (DEVELOPER_ERROR = 10).
            message.contains("28444") || message.contains("developer console", ignoreCase = true) ||
                DEVELOPER_ERROR.containsMatchIn(message) -> AppError.Reason.GOOGLE_NOT_CONFIGURED
            error is NoCredentialException -> AppError.Reason.GOOGLE_NO_ACCOUNT
            error is GetCredentialProviderConfigurationException ||
                error is GetCredentialUnsupportedException -> AppError.Reason.GOOGLE_UNAVAILABLE
            else -> AppError.Reason.GOOGLE_FAILED
        }
        return AppError(reason, error)
    }

    private fun Context.findActivity(): Activity? {
        var current: Context? = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    private const val TAG = "MiGasto"
    private const val CLEAR_TIMEOUT_MS = 3_000L
    private val DEVELOPER_ERROR = Regex("""(^|\D)10: """)
}
