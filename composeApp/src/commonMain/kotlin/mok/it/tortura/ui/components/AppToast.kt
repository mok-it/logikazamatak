package mok.it.tortura.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import mok.it.tortura.ui.theme.AppThemeTokens

@Composable
expect fun PlatformToastHost()

expect fun platformShowToast(
    message: String,
    backgroundColor: Color,
    textColor: Color,
    isError: Boolean,
)

@Composable
fun TransientToastEffect(
    message: String?,
    errorMessage: String?,
    onConsumed: () -> Unit,
) {
    val toastMessage = errorMessage ?: message ?: return
    val isError = errorMessage != null
    val colors = AppThemeTokens.colors

    LaunchedEffect(message, errorMessage) {
        platformShowToast(
            message = toastMessage,
            backgroundColor = if (isError) colors.danger else colors.accent,
            textColor = colors.surface,
            isError = isError,
        )
        onConsumed()
    }
}
