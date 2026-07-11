package mok.it.tortura.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mok.it.tortura.ui.theme.AppThemeTokens

enum class AppButtonStyle {
    Primary,
    Secondary,
    Danger,
    Ghost,
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    AppButton(
        onClick = onClick,
        modifier = modifier,
        style = style,
        enabled = enabled,
        loading = loading,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text)
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingIcon()
        }
    }
}

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = AppThemeTokens.colors
    val isEnabled = enabled && !loading
    val indicatorColor = when (style) {
        AppButtonStyle.Primary, AppButtonStyle.Danger -> MaterialTheme.colorScheme.onPrimary
        AppButtonStyle.Secondary -> colors.accent
        AppButtonStyle.Ghost -> colors.textSecondary
    }
    val buttonContent: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = indicatorColor,
            )
        } else {
            content()
        }
    }

    val touchTargetModifier = modifier.heightIn(min = 48.dp)

    when (style) {
        AppButtonStyle.Primary -> Button(
            onClick = onClick,
            modifier = touchTargetModifier,
            enabled = isEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            content = buttonContent,
        )

        AppButtonStyle.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = touchTargetModifier,
            enabled = isEnabled,
            border = BorderStroke(1.dp, colors.borderStrong),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
            content = buttonContent,
        )

        AppButtonStyle.Danger -> Button(
            onClick = onClick,
            modifier = touchTargetModifier,
            enabled = isEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.danger,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            content = buttonContent,
        )

        AppButtonStyle.Ghost -> TextButton(
            onClick = onClick,
            modifier = touchTargetModifier,
            enabled = isEnabled,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary),
            content = buttonContent,
        )
    }
}
