package mok.it.tortura.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import mok.it.tortura.ui.theme.AppThemeTokens

enum class BannerTone {
    Success,
    Warning,
    Error,
}

@Composable
fun StatusBanner(
    message: String,
    tone: BannerTone,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val colors = AppThemeTokens.colors
    val spacing = AppThemeTokens.spacing
    val radii = AppThemeTokens.radii
    val background = when (tone) {
        BannerTone.Success -> colors.successMuted
        BannerTone.Warning -> colors.warningMuted
        BannerTone.Error -> colors.dangerMuted
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radii.md))
            .background(background)
            .padding(spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
        )
        onDismiss?.let {
            AppButton(
                text = "Bezárás",
                onClick = it,
                style = AppButtonStyle.Ghost,
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val spacing = AppThemeTokens.spacing

    SectionCard(modifier = modifier, toned = true) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}
