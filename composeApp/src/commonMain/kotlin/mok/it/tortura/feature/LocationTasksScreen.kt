package mok.it.tortura.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import mok.it.tortura.model.Location
import mok.it.tortura.ui.components.AppButton
import mok.it.tortura.ui.components.AppButtonStyle
import mok.it.tortura.ui.components.ChangeLocationIcon
import mok.it.tortura.ui.components.EmptyState
import mok.it.tortura.ui.components.NavigateBackIcon
import mok.it.tortura.ui.components.PageHeader
import mok.it.tortura.ui.components.PageScaffold
import mok.it.tortura.ui.components.SectionCard
import mok.it.tortura.ui.theme.AppTheme
import mok.it.tortura.ui.theme.AppThemeTokens

@Composable
fun LocationTasksScreen(
    activeGameName: String,
    activeLocation: Location,
    onBack: () -> Unit = {},
    onChangeLocation: () -> Unit = {},
) {
    PageScaffold(modifier = Modifier.verticalScroll(rememberScrollState())) {
        PageHeader(
            title = activeLocation.name ?: "Állomás",
            description = "$activeGameName • feladatbeküldés",
            trailingContent = {
                AppButton(
                    text = "Vissza",
                    onClick = onBack,
                    style = AppButtonStyle.Ghost,
                    leadingIcon = { NavigateBackIcon() },
                )
                AppButton(
                    text = currentLocationButtonLabel(activeLocation.name),
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
            SectionCard(toned = true) {
                Text(
                    text = "Mockolt állomásnézet",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Ide kerül majd a csapatválasztás, a helyszínhez tartozó feladatok listája, " +
                        "és a válaszbeküldés.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppThemeTokens.colors.textSecondary,
                )
            }

            EmptyState(
                title = "Feladatbeküldés még nincs implementálva",
                description = "A routing most már külön kezeli a shopot és a normál állomásokat. " +
                    "A tényleges gameplay űrlap ide jön majd.",
            )
        }
    }
}

private fun currentLocationButtonLabel(activeLocationName: String?): String =
    "Helyszín: ${activeLocationName ?: "nincs kiválasztva"}"

@Preview(showBackground = true)
@Composable
private fun LocationTasksScreenPreview() {
    AppTheme {
        LocationTasksScreen(
            activeGameName = "Demo Game - Logic Castle",
            activeLocation = Location(id = 1, name = "Library", gameId = 1),
        )
    }
}
