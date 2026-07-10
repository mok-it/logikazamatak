package mok.it.tortura.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class AppColors(
    val pageBackground: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceMuted: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentMuted: Color,
    val success: Color,
    val successMuted: Color,
    val warning: Color,
    val warningMuted: Color,
    val danger: Color,
    val dangerMuted: Color,
)

@Immutable
data class AppSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
)

@Immutable
data class AppRadii(
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 18.dp,
    val xl: Dp = 24.dp,
)

private val appColors = AppColors(
    pageBackground = Color(0xFFF4F1EB),
    surface = Color(0xFFFFFCF7),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF0EBE1),
    borderSubtle = Color(0xFFD9D0C3),
    borderStrong = Color(0xFF9A8F80),
    textPrimary = Color(0xFF1F2430),
    textSecondary = Color(0xFF5B6472),
    accent = Color(0xFF1E6A60),
    accentMuted = Color(0xFFDDEEEA),
    success = Color(0xFF1B7F4E),
    successMuted = Color(0xFFDDF2E5),
    warning = Color(0xFF9A6400),
    warningMuted = Color(0xFFF6E9C8),
    danger = Color(0xFFB33B2E),
    dangerMuted = Color(0xFFF8E0DB),
)

private val materialColors = lightColorScheme(
    primary = appColors.accent,
    onPrimary = Color.White,
    primaryContainer = appColors.accentMuted,
    onPrimaryContainer = appColors.textPrimary,
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6EBF3),
    onSecondaryContainer = appColors.textPrimary,
    tertiary = Color(0xFF7A5C3D),
    onTertiary = Color.White,
    background = appColors.pageBackground,
    onBackground = appColors.textPrimary,
    surface = appColors.surface,
    onSurface = appColors.textPrimary,
    surfaceVariant = appColors.surfaceMuted,
    onSurfaceVariant = appColors.textSecondary,
    outline = appColors.borderStrong,
    error = appColors.danger,
    onError = Color.White,
    errorContainer = appColors.dangerMuted,
    onErrorContainer = appColors.textPrimary,
)

private val appTypography = Typography(
    displayLarge = TextStyle(fontSize = 42.sp, lineHeight = 48.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

private val LocalAppColors = staticCompositionLocalOf { appColors }
private val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
private val LocalAppRadii = staticCompositionLocalOf { AppRadii() }

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalAppSpacing provides AppSpacing(),
        LocalAppRadii provides AppRadii(),
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = appTypography,
            content = content,
        )
    }
}

object AppThemeTokens {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val spacing: AppSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalAppSpacing.current

    val radii: AppRadii
        @Composable
        @ReadOnlyComposable
        get() = LocalAppRadii.current
}
