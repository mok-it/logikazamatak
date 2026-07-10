package mok.it.tortura.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mok.it.tortura.ui.components.AppButton
import mok.it.tortura.ui.components.AppButtonStyle
import mok.it.tortura.ui.components.AppNumberField
import mok.it.tortura.ui.components.BannerTone
import mok.it.tortura.ui.components.ChangeLocationIcon
import mok.it.tortura.ui.components.EmptyState
import mok.it.tortura.ui.components.FormSection
import mok.it.tortura.ui.components.NavigateBackIcon
import mok.it.tortura.ui.components.PageHeader
import mok.it.tortura.ui.components.PageScaffold
import mok.it.tortura.ui.components.RefreshIcon
import mok.it.tortura.ui.components.SaveIcon
import mok.it.tortura.ui.components.StatusBanner
import mok.it.tortura.ui.theme.AppThemeTokens

@Composable
fun SetUpMenu(
    activeGameName: String,
    activeLocationName: String?,
    uiState: SetupUiState = SetupUiState(),
    onLoad: () -> Unit = {},
    onBaseTeamCounterChange: (String) -> Unit = {},
    onTeamCreation: () -> Unit = {},
    onClearMessages: () -> Unit = {},
    onBack: () -> Unit = {},
    onChangeLocation: () -> Unit = {},
) {
    val spacing = AppThemeTokens.spacing

    LaunchedEffect(Unit) {
        onLoad()
    }

    PageScaffold(modifier = Modifier.verticalScroll(rememberScrollState())) {
        PageHeader(
            title = activeGameName,
            description = activeLocationName?.let { "Aktív helyszín: $it" } ?: "Még nincs kiválasztott helyszín.",
            trailingContent = {
                AppButton(
                    text = "Vissza",
                    onClick = onBack,
                    style = AppButtonStyle.Ghost,
                    leadingIcon = { NavigateBackIcon() },
                )
                AppButton(
                    text = "Helyszín váltása",
                    onClick = onChangeLocation,
                    style = AppButtonStyle.Secondary,
                    leadingIcon = { ChangeLocationIcon() },
                )
            },
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (uiState.errorMessage != null || uiState.message != null) {
                StatusBanner(
                    message = uiState.errorMessage ?: uiState.message.orEmpty(),
                    tone = if (uiState.errorMessage != null) BannerTone.Error else BannerTone.Success,
                    onDismiss = onClearMessages,
                )
            }

            FormSection(
                title = "Csapatok",
                description = "Állítsd be az alap csapatszámot, majd mentsd el a jelenlegi játékhoz.",
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth >= 680.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.md),
                            verticalAlignment = Alignment.Top,
                        ) {
                            AppNumberField(
                                value = uiState.baseTeamCounter,
                                onValueChange = onBaseTeamCounterChange,
                                label = "Alap csapatszám",
                                enabled = !uiState.isLoading,
                                modifier = Modifier.weight(1f),
                            )
                            AppButton(
                                text = "Mentés",
                                onClick = onTeamCreation,
                                enabled = !uiState.isLoading,
                                leadingIcon = { SaveIcon() },
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                            AppNumberField(
                                value = uiState.baseTeamCounter,
                                onValueChange = onBaseTeamCounterChange,
                                label = "Alap csapatszám",
                                enabled = !uiState.isLoading,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            AppButton(
                                text = "Mentés",
                                onClick = onTeamCreation,
                                enabled = !uiState.isLoading,
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { SaveIcon() },
                            )
                        }
                    }
                }

                ExistingRows(
                    title = "Mentett csapatbeosztások",
                    rows = uiState.teamAssignments.map { assignment ->
                        "ID ${assignment.id ?: "-"}: ${assignment.baseTeamCounter ?: 0} csapat"
                    },
                )
            }

            AppButton(
                text = "Frissítés",
                onClick = onLoad,
                enabled = !uiState.isLoading,
                style = AppButtonStyle.Secondary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                leadingIcon = { RefreshIcon() },
            )
        }
    }
}

@Composable
private fun ExistingRows(
    title: String,
    rows: List<String>,
) {
    Text(title, style = MaterialTheme.typography.labelLarge)
    if (rows.isEmpty()) {
        EmptyState(
            title = "Nincs mentett adat",
            description = "A mentés után itt jelennek meg a csapatbeosztások.",
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.sm)) {
        rows.forEach { row ->
            Text(row, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
