package com.chiiraac.migasto.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.chiiraac.migasto.data.model.ThemeMode

// Paleta inspirada en el diseño original (tonos Tailwind).
object Palette {
    val Indigo = Color(0xFF6366F1)
    val IndigoDark = Color(0xFF4F46E5)
    val IndigoDeep = Color(0xFF4338CA)
    val IndigoLight = Color(0xFFA5B4FC)
    val Emerald = Color(0xFF10B981)
    val EmeraldDark = Color(0xFF059669)
    val Red = Color(0xFFEF4444)
    val RedDark = Color(0xFFDC2626)
    val Amber = Color(0xFFF59E0B)
    val AmberDark = Color(0xFFD97706)
    val Sky = Color(0xFF60A5FA)
    val SkyDark = Color(0xFF2563EB)

    val Gray50 = Color(0xFFF9FAFB)
    val Gray100 = Color(0xFFF3F4F6)
    val Gray200 = Color(0xFFE5E7EB)
    val Gray300 = Color(0xFFD1D5DB)
    val Gray400 = Color(0xFF9CA3AF)
    val Gray500 = Color(0xFF6B7280)
    val Gray600 = Color(0xFF4B5563)
    val Gray700 = Color(0xFF374151)
    val Gray750 = Color(0xFF2B3544)
    val Gray800 = Color(0xFF1F2937)
    val Gray850 = Color(0xFF18212F)
    val Gray900 = Color(0xFF111827)
    val Gray950 = Color(0xFF0B1120)
}

/** Colores con significado propio de la app (ingreso, gasto, factura, traspaso…). */
@Immutable
data class MoneyColors(
    val income: Color,
    val expense: Color,
    val bill: Color,
    val transfer: Color,
    val balanceCard: Color,
    val balanceCardEnd: Color,
    val onBalanceCard: Color,
    val bankDot: Color,
    val cashDot: Color,
    val fab: Color,
    val navSelected: Color,
)

private val DarkMoneyColors = MoneyColors(
    income = Palette.Emerald,
    expense = Palette.Red,
    bill = Palette.Amber,
    transfer = Palette.Sky,
    balanceCard = Palette.Indigo,
    balanceCardEnd = Color(0xFF5458E8),
    onBalanceCard = Color.White,
    bankDot = Palette.IndigoLight,
    cashDot = Color(0xFF34D399),
    fab = Palette.Emerald,
    navSelected = Palette.Emerald,
)

private val LightMoneyColors = MoneyColors(
    income = Palette.EmeraldDark,
    expense = Palette.RedDark,
    bill = Palette.AmberDark,
    transfer = Palette.SkyDark,
    balanceCard = Palette.Indigo,
    balanceCardEnd = Palette.IndigoDark,
    onBalanceCard = Color.White,
    bankDot = Color(0xFFC7D2FE),
    cashDot = Color(0xFF6EE7B7),
    fab = Palette.Emerald,
    navSelected = Palette.EmeraldDark,
)

private val DarkColors = darkColorScheme(
    primary = Palette.Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Palette.Emerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Palette.Amber,
    onTertiary = Color.Black,
    error = Color(0xFFF87171),
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    background = Palette.Gray900,
    onBackground = Palette.Gray50,
    surface = Palette.Gray900,
    onSurface = Palette.Gray50,
    surfaceVariant = Palette.Gray700,
    onSurfaceVariant = Palette.Gray400,
    surfaceContainerLowest = Palette.Gray950,
    surfaceContainerLow = Palette.Gray850,
    surfaceContainer = Palette.Gray800,
    surfaceContainerHigh = Palette.Gray800,
    surfaceContainerHighest = Palette.Gray750,
    surfaceBright = Palette.Gray700,
    surfaceDim = Palette.Gray950,
    inverseSurface = Palette.Gray100,
    inverseOnSurface = Palette.Gray900,
    inversePrimary = Palette.IndigoDark,
    outline = Palette.Gray600,
    outlineVariant = Palette.Gray700,
    scrim = Color.Black,
)

private val LightColors = lightColorScheme(
    primary = Palette.IndigoDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Palette.EmeraldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Palette.AmberDark,
    onTertiary = Color.White,
    error = Palette.RedDark,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Palette.Gray100,
    onBackground = Palette.Gray900,
    surface = Palette.Gray100,
    onSurface = Palette.Gray900,
    surfaceVariant = Palette.Gray200,
    onSurfaceVariant = Palette.Gray500,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Palette.Gray50,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Palette.Gray200,
    surfaceBright = Color.White,
    surfaceDim = Palette.Gray200,
    inverseSurface = Palette.Gray800,
    inverseOnSurface = Palette.Gray50,
    inversePrimary = Palette.IndigoLight,
    outline = Palette.Gray300,
    outlineVariant = Palette.Gray200,
    scrim = Color.Black,
)

val LocalMoneyColors = staticCompositionLocalOf { DarkMoneyColors }

private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Estilo del importe grande de la tarjeta de saldo. */
val BalanceTextStyle = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 50.sp)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun isAppInDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun MiGastoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = isAppInDarkTheme(themeMode)
    val colors: ColorScheme = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(LocalMoneyColors provides if (dark) DarkMoneyColors else LightMoneyColors) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object AppTheme {
    val money: MoneyColors
        @Composable get() = LocalMoneyColors.current
}
