package mok.it.tortura.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import mok.it.tortura.ui.theme.AppTheme
import mok.it.tortura.ui.theme.AppThemeTokens

@Preview(showBackground = true)
@Composable
private fun AppButtonsPreview() {
    AppTheme {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.md),
        ) {
            AppButton(text = "Primary action", onClick = {}, leadingIcon = { SaveIcon() })
            AppButton(text = "Secondary action", onClick = {
            }, style = AppButtonStyle.Secondary, leadingIcon = { AddIcon() })
            AppButton(text = "Danger action", onClick = {
            }, style = AppButtonStyle.Danger, leadingIcon = { DeleteIcon() })
            AppButton(text = "Ghost action", onClick = {
            }, style = AppButtonStyle.Ghost, leadingIcon = { NavigateBackIcon() })
            AppButton(text = "Loading action", onClick = {}, loading = true)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppFieldsPreview() {
    AppTheme {
        var selectExpanded by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.md),
        ) {
            AppTextField(
                value = "Tortura 2026",
                onValueChange = {},
                label = "Játék neve",
                modifier = Modifier.fillMaxWidth(),
            )
            AppTextField(
                value = "",
                onValueChange = {},
                label = "Helyszín neve",
                modifier = Modifier.fillMaxWidth(),
                isError = true,
                supportingText = "Kötelező",
            )
            AppNumberField(
                value = "12",
                onValueChange = {},
                label = "Alap csapatszám",
                modifier = Modifier.fillMaxWidth(),
            )
            AppSelectField(
                value = "1: Extra pont",
                label = "Effekt",
                expanded = selectExpanded,
                options = listOf(
                    AppSelectOption(value = "", label = "Nincs effekt"),
                    AppSelectOption(value = "1", label = "1: Extra pont"),
                    AppSelectOption(value = "2", label = "2: Ingyen tipp"),
                ),
                onExpandedChange = { selectExpanded = it },
                onSelect = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppFeedbackPreview() {
    AppTheme {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.md),
        ) {
            StatusBanner(
                message = "A játék sikeresen mentve lett.",
                tone = BannerTone.Success,
                onDismiss = {},
            )
            StatusBanner(
                message = "Ehhez a játékhoz még nincs választható állomás.",
                tone = BannerTone.Warning,
            )
            StatusBanner(
                message = "A mentés nem sikerült.",
                tone = BannerTone.Error,
                onDismiss = {},
            )
            EmptyState(
                title = "Nincs még mentett játék",
                description = "Hozz létre egy újat a szerkesztőben.",
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppLayoutPreview() {
    AppTheme {
        PageScaffold {
            PageHeader(
                title = "Design system preview",
                description = "A közös layout és a szekcióblokkok mintája.",
                trailingContent = {
                    AppButton(text = "Művelet", onClick = {
                    }, style = AppButtonStyle.Secondary, leadingIcon = { RefreshIcon() })
                },
            )
            FormSection(
                title = "Űrlapszekció",
                description = "A jellemző admin felületi blokk.",
                headerAction = {
                    AppButton(text = "Hozzáadás", onClick = {
                    }, style = AppButtonStyle.Secondary, leadingIcon = { AddIcon() })
                },
            ) {
                AppTextField(
                    value = "Mintamező",
                    onValueChange = {},
                    label = "Mező",
                    modifier = Modifier.fillMaxWidth(),
                )
                InlineActionRow {
                    AppButton(text = "Mentés", onClick = {}, leadingIcon = { SaveIcon() })
                }
            }
        }
    }
}
