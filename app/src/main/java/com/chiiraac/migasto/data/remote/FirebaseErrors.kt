package com.chiiraac.migasto.data.remote

import com.chiiraac.migasto.data.AppError
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException

/** Traduce las excepciones de Firebase a [AppError] para mostrar mensajes claros. */
object FirebaseErrors {
    fun map(error: Throwable): Throwable = when (error) {
        is AppError -> error
        // Al confirmar la identidad con Google se eligió una cuenta distinta de la de la sesión.
        is FirebaseAuthException if error.errorCode == "ERROR_USER_MISMATCH" ->
            AppError(AppError.Reason.GOOGLE_ACCOUNT_MISMATCH, error)
        is FirebaseAuthWeakPasswordException -> AppError(AppError.Reason.WEAK_PASSWORD, error)
        is FirebaseAuthUserCollisionException ->
            if (error.errorCode == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL") {
                AppError(AppError.Reason.ACCOUNT_EXISTS_WITH_PASSWORD, error)
            } else {
                AppError(AppError.Reason.EMAIL_IN_USE, error)
            }
        is FirebaseAuthInvalidUserException ->
            if (error.errorCode == "ERROR_USER_DISABLED") {
                AppError(AppError.Reason.ACCOUNT_DISABLED, error)
            } else {
                AppError(AppError.Reason.WRONG_CREDENTIALS, error)
            }
        is FirebaseAuthInvalidCredentialsException ->
            if (error.errorCode == "ERROR_INVALID_EMAIL") {
                AppError(AppError.Reason.INVALID_EMAIL, error)
            } else {
                AppError(AppError.Reason.WRONG_CREDENTIALS, error)
            }
        is FirebaseNetworkException -> AppError(AppError.Reason.NETWORK, error)
        is FirebaseTooManyRequestsException -> AppError(AppError.Reason.TOO_MANY_REQUESTS, error)
        is FirebaseFirestoreException -> when (error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError(AppError.Reason.PERMISSION_DENIED, error)
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError(AppError.Reason.NETWORK, error)
            else -> AppError(AppError.Reason.UNKNOWN, error)
        }
        // Proveedor desactivado en la consola de Firebase (Authentication → Sign-in method).
        is FirebaseAuthException if error.errorCode == "ERROR_OPERATION_NOT_ALLOWED" ->
            AppError(AppError.Reason.AUTH_NOT_CONFIGURED, error)
        // Authentication aún no se ha iniciado en el proyecto: el SDK lo da como error interno.
        is FirebaseException if error.message?.contains("CONFIGURATION_NOT_FOUND") == true ->
            AppError(AppError.Reason.AUTH_NOT_CONFIGURED, error)
        else -> AppError(AppError.Reason.UNKNOWN, error)
    }

    /** Errores al entrar con Google: un token de Google rechazado no es "contraseña incorrecta". */
    fun mapGoogle(error: Throwable): Throwable {
        val mapped = map(error)
        if (mapped !is AppError) return mapped
        return when (mapped.reason) {
            AppError.Reason.AUTH_NOT_CONFIGURED -> AppError(AppError.Reason.GOOGLE_NOT_CONFIGURED, error)
            AppError.Reason.WRONG_CREDENTIALS,
            AppError.Reason.UNKNOWN -> AppError(AppError.Reason.GOOGLE_FAILED, error)
            else -> mapped
        }
    }
}
