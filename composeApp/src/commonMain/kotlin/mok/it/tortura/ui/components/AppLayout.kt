package mok.it.tortura.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mok.it.tortura.ui.theme.AppThemeTokens

@Composable
fun PageScaffold(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppThemeTokens.colors
    val spacing = AppThemeTokens.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.pageBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1120.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = spacing.xl, vertical = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
            content = content,
        )
    }
}

@Composable
fun PageHeader(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = AppThemeTokens.colors
    val spacing = AppThemeTokens.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
            description?.takeIf { it.isNotEmpty() }?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
            }
        }
        trailingContent?.let {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                content = it,
            )
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    toned: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppThemeTokens.colors
    val spacing = AppThemeTokens.spacing
    val radii = AppThemeTokens.radii

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(radii.lg),
        border = BorderStroke(1.dp, colors.borderSubtle),
        colors = CardDefaults.cardColors(
            containerColor = if (toned) colors.surfaceMuted else colors.surfaceRaised,
            contentColor = colors.textPrimary,
        ),
    ) {
        Column(
            modifier = Modifier.padding(spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
            content = content,
        )
    }
}

@Composable
fun FormSection(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    headerAction: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppThemeTokens.colors
    val spacing = AppThemeTokens.spacing

    SectionCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                description?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
            }
            headerAction?.let {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    content = it,
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
            content = content,
        )
    }
}

@Composable
fun InlineActionRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val spacing = AppThemeTokens.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
