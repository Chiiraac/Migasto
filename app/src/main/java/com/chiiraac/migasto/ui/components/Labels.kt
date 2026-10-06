package com.chiiraac.migasto.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Work
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.chiiraac.migasto.R
import com.chiiraac.migasto.data.AppError
import com.chiiraac.migasto.data.model.GroupIcon
import com.chiiraac.migasto.data.model.MovementType
import com.chiiraac.migasto.data.model.PaymentMethod
import com.chiiraac.migasto.data.model.ThemeMode
import com.chiiraac.migasto.ui.theme.AppTheme

@get:StringRes
val MovementType.label: Int
    get() = when (this) {
        MovementType.EXPENSE -> R.string.type_expense
        MovementType.INCOME -> R.string.type_income
        MovementType.BILL -> R.string.type_bill
        MovementType.TRANSFER -> R.string.type_transfer
    }

@get:StringRes
val PaymentMethod.label: Int
    get() = when (this) {
        PaymentMethod.BANK -> R.string.method_bank
        PaymentMethod.CASH -> R.string.method_cash
    }

@get:StringRes
val PaymentMethod.transferLabel: Int
    get() = when (this) {
        PaymentMethod.BANK -> R.string.transfer_bank_to_cash
        PaymentMethod.CASH -> R.string.transfer_cash_to_bank
    }

val PaymentMethod.icon: ImageVector
    get() = when (this) {
        PaymentMethod.BANK -> Icons.Rounded.AccountBalance
        PaymentMethod.CASH -> Icons.Rounded.Payments
    }

@get:StringRes
val ThemeMode.label: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }

val GroupIcon.vector: ImageVector
    get() = when (this) {
        GroupIcon.HOME -> Icons.Rounded.Home
        GroupIcon.PERSON -> Icons.Rounded.Person
        GroupIcon.COUPLE -> Icons.Rounded.Favorite
        GroupIcon.FAMILY -> Icons.Rounded.FamilyRestroom
        GroupIcon.FRIENDS -> Icons.Rounded.Groups
        GroupIcon.TRAVEL -> Icons.Rounded.Luggage
        GroupIcon.WORK -> Icons.Rounded.Work
        GroupIcon.SAVINGS -> Icons.Rounded.Savings
    }

@get:StringRes
val GroupIcon.label: Int
    get() = when (this) {
        GroupIcon.HOME -> R.string.group_icon_home
        GroupIcon.PERSON -> R.string.group_icon_person
        GroupIcon.COUPLE -> R.string.group_icon_couple
        GroupIcon.FAMILY -> R.string.group_icon_family
        GroupIcon.FRIENDS -> R.string.group_icon_friends
        GroupIcon.TRAVEL -> R.string.group_icon_travel
        GroupIcon.WORK -> R.string.group_icon_work
        GroupIcon.SAVINGS -> R.string.group_icon_savings
    }

@Composable
fun MovementType.color(): Color = when (this) {
    MovementType.INCOME -> AppTheme.money.income
    MovementType.EXPENSE -> AppTheme.money.expense
    MovementType.BILL -> AppTheme.money.bill
    MovementType.TRANSFER -> AppTheme.money.transfer
}

/** Mensaje para el usuario a partir de un error. */
@StringRes
fun Throwable.messageRes(): Int {
    val reason = (this as? AppError)?.reason ?: return R.string.error_unknown
    return when (reason) {
        AppError.Reason.INVALID_INVITE_CODE -> R.string.error_invalid_code
        AppError.Reason.ALREADY_MEMBER -> R.string.error_already_member
        AppError.Reason.NOT_AVAILABLE_OFFLINE_MODE -> R.string.error_local_mode
        AppError.Reason.NETWORK -> R.string.error_network
        AppError.Reason.PERMISSION_DENIED -> R.string.error_permission
        AppError.Reason.INVALID_EMAIL -> R.string.error_invalid_email
        AppError.Reason.WRONG_CREDENTIALS -> R.string.error_wrong_credentials
        AppError.Reason.EMAIL_IN_USE -> R.string.error_email_in_use
        AppError.Reason.WEAK_PASSWORD -> R.string.error_weak_password
        AppError.Reason.TOO_MANY_REQUESTS -> R.string.error_too_many
        AppError.Reason.AUTH_NOT_CONFIGURED -> R.string.error_auth_not_configured
        AppError.Reason.ACCOUNT_DISABLED -> R.string.error_account_disabled
        AppError.Reason.PHOTO_TOO_LARGE -> R.string.error_photo_too_large
        AppError.Reason.CANCELLED -> R.string.error_cancelled
        AppError.Reason.GOOGLE_FAILED -> R.string.error_google_failed
        AppError.Reason.GOOGLE_NO_ACCOUNT -> R.string.error_google_no_account
        AppError.Reason.GOOGLE_UNAVAILABLE -> R.string.error_google_unavailable
        AppError.Reason.GOOGLE_NOT_CONFIGURED -> R.string.error_google_not_configured
        AppError.Reason.GOOGLE_ACCOUNT_MISMATCH -> R.string.error_google_mismatch
        AppError.Reason.ACCOUNT_EXISTS_WITH_PASSWORD -> R.string.error_account_exists_password
        AppError.Reason.UNKNOWN -> R.string.error_unknown
    }
}
