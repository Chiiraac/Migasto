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
        PHOTO_TOO_LARGE,
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
