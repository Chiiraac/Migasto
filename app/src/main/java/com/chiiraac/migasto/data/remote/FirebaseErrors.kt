package com.chiiraac.migasto.data.remote

import com.chiiraac.migasto.data.AppError
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException

/** Traduce las excepciones de Firebase a [AppError] para mostrar mensajes claros. */
object FirebaseErrors {
    fun map(error: Throwable): Throwable = when (error) {
        is AppError -> error
        is FirebaseAuthWeakPasswordException -> AppError(AppError.Reason.WEAK_PASSWORD, error)
        is FirebaseAuthUserCollisionException -> AppError(AppError.Reason.EMAIL_IN_USE, error)
        is FirebaseAuthInvalidUserException -> AppError(AppError.Reason.WRONG_CREDENTIALS, error)
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
        else -> AppError(AppError.Reason.UNKNOWN, error)
    }
}
