package mok.it.tortura.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import mok.it.tortura.model.Team
import mok.it.tortura.ui.components.AppButton
import mok.it.tortura.ui.components.AppButtonStyle
import mok.it.tortura.ui.components.BannerTone
import mok.it.tortura.ui.components.ChangeLocationIcon
import mok.it.tortura.ui.components.EmptyState
import mok.it.tortura.ui.components.FormSection
import mok.it.tortura.ui.components.NavigateBackIcon
import mok.it.tortura.ui.components.PageHeader
import mok.it.tortura.ui.components.PageScaffold
import mok.it.tortura.ui.components.NavigateForwardIcon
import mok.it.tortura.ui.components.SectionCard
import mok.it.tortura.ui.components.TransientToastEffect
import mok.it.tortura.ui.theme.AppThemeTokens

@Composable
fun HealerTeamSelectionScreen(
    activeGameName: String,
    activeLocationName: String?,
    uiState: HealerTeamSelectionUiState = HealerTeamSelectionUiState(),
    onLoad: () -> Unit = {},
    onSelectTeam: (Team) -> Unit = {},
    onClearMessages: () -> Unit = {},
    onBack: () -> Unit = {},
    onChangeLocation: () -> Unit = {},
) {
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
                    text = currentLocationButtonLabel(activeLocationName),
                    onClick = onChangeLocation,
                    style = AppButtonStyle.Secondary,
                    leadingIcon = { ChangeLocationIcon() },
                )
            },
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.xl),
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            TransientToastEffect(
                message = uiState.message,
                errorMessage = uiState.errorMessage,
                onConsumed = onClearMessages,
            )

            FormSection(
                title = "Gyógyító csapatok",
                description = "Válassz csapatot a gyógyító feladatokhoz.",
            ) {
                if (uiState.teams.isEmpty() && !uiState.isLoading) {
                    EmptyState(
                        title = "Ehhez a játékhoz még nincs csapat",
                        description = "Előbb hozd létre a csapatokat az előkészítés képernyőn.",
                    )
                } else {
                    uiState.teams.forEach { team ->
                        TeamSelectionRow(
                            team = team,
                            isLoading = uiState.isLoading,
                            onSelectTeam = onSelectTeam,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamSelectionRow(
    team: Team,
    isLoading: Boolean,
    onSelectTeam: (Team) -> Unit,
) {
    SectionCard(modifier = Modifier.fillMaxWidth(), toned = true) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.md),
        ) {
            Text(
                text = team.name ?: "Csapat #${team.id ?: "-"}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "ID ${team.id ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
            )
            AppButton(
                text = "Megnyitás",
                onClick = { onSelectTeam(team) },
                enabled = !isLoading && team.id != null,
                leadingIcon = { NavigateForwardIcon() },
            )
        }
    }
}

private fun currentLocationButtonLabel(activeLocationName: String?): String =
    "Helyszín: ${activeLocationName ?: "nincs kiválasztva"}"
