package mok.it.tortura.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mok.it.tortura.ui.components.AppButton
import mok.it.tortura.ui.components.AppButtonStyle
import mok.it.tortura.ui.components.SectionCard
import mok.it.tortura.ui.theme.AppThemeTokens

@Composable
fun SelectionRow(
    title: String,
    subtitle: String? = null,
    actionLabel: String,
    enabled: Boolean = true,
    onAction: () -> Unit,
) {
    val spacing = AppThemeTokens.spacing

    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                modifier = Modifier.weight(1f),
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
            AppButton(
                text = actionLabel,
                onClick = onAction,
                enabled = enabled,
                style = AppButtonStyle.Secondary,
            )
        }
    }
}
