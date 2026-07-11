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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.pageBackground),
    ) {
        val compact = maxWidth < 600.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1120.dp)
                .align(Alignment.TopCenter)
                .padding(
                    horizontal = if (compact) spacing.md else spacing.xl,
                    vertical = if (compact) spacing.lg else spacing.xxl,
                ),
            verticalArrangement = Arrangement.spacedBy(if (compact) spacing.lg else spacing.xl),
            content = content,
        )
    }
}

@Composable
fun PageHeader(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val colors = AppThemeTokens.colors
    val spacing = AppThemeTokens.spacing

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compact = maxWidth < 600.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                PageHeaderText(title = title, description = description)
                trailingContent?.let {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) { it() }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                verticalAlignment = Alignment.Top,
            ) {
                PageHeaderText(
                    title = title,
                    description = description,
                    modifier = Modifier.weight(1f),
                )
                trailingContent?.let {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { it() }
                }
            }
        }
    }
}

@Composable
private fun PageHeaderText(
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val spacing = AppThemeTokens.spacing

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
        description?.takeIf { it.isNotEmpty() }?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
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
        BoxWithConstraints {
            val compact = maxWidth < 600.dp
            Column(
                modifier = Modifier.padding(if (compact) spacing.lg else spacing.xl),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
                content = content,
            )
        }
    }
}

@Composable
fun FormSection(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppThemeTokens.colors
    val spacing = AppThemeTokens.spacing

    SectionCard(modifier = modifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 480.dp
            val heading: @Composable (Modifier) -> Unit = { headingModifier ->
                Column(modifier = headingModifier, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                description?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
            }
            }
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    heading(Modifier.fillMaxWidth())
                    headerAction?.let {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(spacing.sm),
                        ) { it() }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalAlignment = Alignment.Top,
                ) {
                    heading(Modifier.weight(1f))
                    headerAction?.let {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) { it() }
                    }
                }
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
    content: @Composable () -> Unit,
) {
    val spacing = AppThemeTokens.spacing

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 480.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) { content() }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) { content() }
        }
    }
}
