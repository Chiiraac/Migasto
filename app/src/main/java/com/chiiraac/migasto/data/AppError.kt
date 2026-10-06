package com.chiiraac.migasto.data

/** Errores de negocio que la interfaz sabe traducir a un mensaje para el usuario. */
class AppError(val reason: Reason, cause: Throwable? = null) : Exception(reason.name, cause) {
    enum class Reason {
        INVALID_INVITE_CODE,
        ALREADY_MEMBER,
        NOT_AVAILABLE_OFFLINE_MODE,
        NETWORK,
        PERMISSION_DENIED,
        INVALID_EMAIL,
        WRONG_CREDENTIALS,
        EMAIL_IN_USE,
        WEAK_PASSWORD,
        TOO_MANY_REQUESTS,
        /** El acceso con email no está activado en el proyecto de Firebase. */
        AUTH_NOT_CONFIGURED,
        ACCOUNT_DISABLED,
        /** La conexión se cortó al unirse a un grupo: la solicitud se completará al volver la conexión. */
        JOIN_QUEUED,
        PHOTO_TOO_LARGE,
        /** El usuario cerró la ventana (p. ej. el selector de cuentas de Google): no se muestra nada. */
        CANCELLED,
        GOOGLE_FAILED,
        GOOGLE_NO_ACCOUNT,
        GOOGLE_UNAVAILABLE,
        GOOGLE_NOT_CONFIGURED,
        /** Al confirmar la identidad se eligió otra cuenta de Google distinta de la de la sesión. */
        GOOGLE_ACCOUNT_MISMATCH,
        /** Ese email ya tiene cuenta con contraseña y Google no puede entrar en ella directamente. */
        ACCOUNT_EXISTS_WITH_PASSWORD,
        UNKNOWN,
    }
}

/** Ejecuta [block] convirtiendo cualquier excepción en un [Result] fallido. */
inline fun <T> runCatchingApp(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
