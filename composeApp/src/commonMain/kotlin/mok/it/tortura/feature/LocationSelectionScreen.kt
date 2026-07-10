package mok.it.tortura.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import mok.it.tortura.model.Game
import mok.it.tortura.model.Location
import mok.it.tortura.ui.components.AppButton
import mok.it.tortura.ui.components.AppButtonStyle
import mok.it.tortura.ui.components.EmptyState
import mok.it.tortura.ui.components.FormSection
import mok.it.tortura.ui.components.NavigateBackIcon
import mok.it.tortura.ui.components.PageHeader
import mok.it.tortura.ui.components.PageScaffold
import mok.it.tortura.ui.components.TransientToastEffect
import mok.it.tortura.ui.theme.AppTheme
import mok.it.tortura.ui.theme.AppThemeTokens

@Composable
fun LocationSelectionScreen(
    uiState: LocationSelectionUiState = LocationSelectionUiState(),
    onLoad: () -> Unit = {},
    onSelectLocation: (Long) -> Unit = {},
    onClearError: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        onLoad()
    }

    PageScaffold(modifier = Modifier.verticalScroll(rememberScrollState())) {
        PageHeader(
            title = "Állomás kiválasztása",
            description = "Játék: ${uiState.game?.name ?: "#${uiState.game?.id ?: "-"}"}",
            trailingContent = {
                AppButton(
                    text = "Vissza",
                    onClick = onBack,
                    style = AppButtonStyle.Ghost,
                    leadingIcon = { NavigateBackIcon() },
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
                message = null,
                errorMessage = uiState.errorMessage,
                onConsumed = onClearError,
            )

            FormSection(
                title = "Választható állomások",
                description = "Válaszd ki, melyik állomáson vagytok.",
            ) {
                if (uiState.locations.isEmpty() && !uiState.isLoading) {
                    EmptyState(
                        title = "Nincs választható állomás",
                        description = "Ehhez a játékhoz még nincs elérhető helyszín.",
                    )
                } else {
                    uiState.locations.forEach { location ->
                        SelectionRow(
                            title = location.name ?: "Állomás #${location.id ?: "-"}",
                            subtitle = if (location.id != null) "ID ${location.id}" else null,
                            actionLabel = "Kiválasztás",
                            enabled = location.id != null,
                            onAction = { location.id?.let(onSelectLocation) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationSelectionScreenPreview() {
    AppTheme {
        LocationSelectionScreen(
            uiState = LocationSelectionUiState(
                game = Game(id = 12, name = "Tortura 2026"),
                locations = listOf(
                    Location(id = 101, name = "Várkapu", gameId = 12),
                    Location(id = 102, name = "Könyvtár", gameId = 12),
                    Location(id = 103, name = "Boszorkánytorony", gameId = 12),
                ),
            ),
        )
    }
}
