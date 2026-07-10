package mok.it.tortura.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mok.it.tortura.model.Game
import mok.it.tortura.model.Location
import mok.it.tortura.ui.components.AppButton
import mok.it.tortura.ui.components.AppButtonStyle
import mok.it.tortura.ui.components.BannerTone
import mok.it.tortura.ui.components.ChangeLocationIcon
import mok.it.tortura.ui.components.FormSection
import mok.it.tortura.ui.components.GamesIcon
import mok.it.tortura.ui.components.HealerIcon
import mok.it.tortura.ui.components.LoginIcon
import mok.it.tortura.ui.components.LogoutIcon
import mok.it.tortura.ui.components.NavigateBackIcon
import mok.it.tortura.ui.components.PageHeader
import mok.it.tortura.ui.components.PageScaffold
import mok.it.tortura.ui.components.SectionCard
import mok.it.tortura.ui.components.SetupIcon
import mok.it.tortura.ui.components.ShopIcon
import mok.it.tortura.ui.components.StatusBanner
import mok.it.tortura.ui.components.TransientToastEffect
import mok.it.tortura.ui.theme.AppThemeTokens

@Composable
fun MainMenu(
    activeGame: Game,
    activeLocation: Location?,
    authUiState: AuthUiState = AuthUiState(isInitializing = false, isAuthenticated = true),
    onSignInWithGoogle: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onClearAuthError: () -> Unit = {},
    onBack: () -> Unit,
    onChangeGame: (() -> Unit),
    onSetUp: (() -> Unit),
    onCompetition: (() -> Unit),
    onShop: (() -> Unit),
    onChangeLocation: () -> Unit,
) {
    val canUseActions = authUiState.isAuthenticated && !authUiState.isBusy
    val spacing = AppThemeTokens.spacing

    PageScaffold {
        PageHeader(
            title = activeGame.name ?: "#${activeGame.id ?: "-"}",
            description = activeLocation?.name?.let { "Aktív helyszín: $it" } ?: "Még nincs kiválasztott helyszín.",
            trailingContent = {
                AppButton(
                    text = "Vissza",
                    onClick = onBack,
                    style = AppButtonStyle.Ghost,
                    leadingIcon = { NavigateBackIcon() },
                )
                AppButton(
                    text = currentLocationButtonLabel(activeLocation?.name),
                    onClick = onChangeLocation,
                    style = AppButtonStyle.Secondary,
                    enabled = canUseActions,
                    leadingIcon = { ChangeLocationIcon() },
                )
            },
        )

        TransientToastEffect(
            message = null,
            errorMessage = authUiState.errorMessage,
            onConsumed = onClearAuthError,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthSection(
                authUiState = authUiState,
                onSignInWithGoogle = onSignInWithGoogle,
                onSignOut = onSignOut,
            )

            FormSection(
                title = "Műveletek",
                description = "Válassz egy fő munkafolyamatot az aktív játékhoz.",
                modifier = Modifier.fillMaxWidth(),
            ) {
                AppButton(
                    text = "Másik játék választása",
                    onClick = onChangeGame,
                    enabled = canUseActions,
                    style = AppButtonStyle.Ghost,
                    modifier = Modifier.align(Alignment.Start),
                    leadingIcon = { GamesIcon() },
                )
                AppButton(
                    text = "Előkészítés",
                    onClick = onSetUp,
                    enabled = canUseActions,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { SetupIcon() },
                )
                AppButton(
                    text = "Gyógyító feladatok",
                    onClick = onCompetition,
                    enabled = canUseActions,
                    style = AppButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { HealerIcon() },
                )
            }
            AppButton(
                text = "Bolt",
                onClick = onShop,
                enabled = canUseActions,
                style = AppButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { ShopIcon() },
            )
        }
    }
}

private fun currentLocationButtonLabel(activeLocationName: String?): String =
    "Helyszín: ${activeLocationName ?: "nincs kiválasztva"}"

@Composable
private fun AuthSection(
    authUiState: AuthUiState,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
) {
    val spacing = AppThemeTokens.spacing

    SectionCard(modifier = Modifier.fillMaxWidth()) {
        if (authUiState.isInitializing) {
            StatusBanner(
                message = "Bejelentkezés ellenőrzése",
                tone = BannerTone.Warning,
            )
            return@SectionCard
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            androidx.compose.material3.Text(
                text = authUiState.email ?: "Bejelentkezve",
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            )

            if (authUiState.isAuthenticated) {
                if (authUiState.email != "Auth kikapcsolva") {
                    AppButton(
                        text = "Kijelentkezés",
                        onClick = onSignOut,
                        enabled = !authUiState.isBusy,
                        style = AppButtonStyle.Ghost,
                        leadingIcon = { LogoutIcon() },
                    )
                }
            } else {
                AppButton(
                    text = if (authUiState.isBusy) "Megnyitás..." else "Bejelentkezés Google-lel",
                    onClick = onSignInWithGoogle,
                    enabled = !authUiState.isBusy,
                    leadingIcon = { LoginIcon() },
                )
            }
        }
    }
}
