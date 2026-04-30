package mok.it.tortura.feature

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mok.it.tortura.model.Game
import mok.it.tortura.model.Location
import mok.it.tortura.ui.components.ActiveGameLocationTopBarTitle
import mok.it.tortura.ui.components.ChangeLocationTopBarAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenu(
    activeGame: Game,
    activeLocation: Location?,
    authUiState: AuthUiState = AuthUiState(isInitializing = false, isAuthenticated = true),
    onSignInWithGoogle: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onClearAuthError: () -> Unit = {},
    onChangeGame: (() -> Unit),
    onSetUp: (() -> Unit),
    onCompetition: (() -> Unit),
    onChangeLocation: () -> Unit,
) {
    if (authUiState.errorMessage != null) {
        AlertDialog(
            title = { Text("Bejelentkezési hiba") },
            text = { Text(authUiState.errorMessage) },
            onDismissRequest = onClearAuthError,
            confirmButton = {
                TextButton(onClick = onClearAuthError) {
                    Text("Rendben")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ActiveGameLocationTopBarTitle(
                        activeGameName = activeGame.name ?: "#${activeGame.id ?: "-"}",
                    )
                },
                actions = {
                    ChangeLocationTopBarAction(
                        activeLocationName = activeLocation?.name,
                        onChangeLocation = onChangeLocation,
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthSection(
                authUiState = authUiState,
                onSignInWithGoogle = onSignInWithGoogle,
                onSignOut = onSignOut,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    onClick = onChangeGame,
                    enabled = authUiState.isAuthenticated && !authUiState.isBusy,
                ) {
                    Text("Másik játék választása")
                }
            }

            Button(
                onClick = onSetUp,
                enabled = authUiState.isAuthenticated && !authUiState.isBusy,
            ) {
                Text(text = "Előkészítés")
            }
            Button(
                onClick = onCompetition,
                enabled = authUiState.isAuthenticated && !authUiState.isBusy,
            ) {
                Text(text = "Gyógyító feladatok")
            }
        }
    }
}

@Composable
private fun AuthSection(
    authUiState: AuthUiState,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (authUiState.isInitializing) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
                Text("Bejelentkezés ellenőrzése")
            }
            return
        }

        if (authUiState.isAuthenticated) {
            Text(
                text = authUiState.email ?: "Bejelentkezve",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (authUiState.email != "Auth kikapcsolva") {
                Button(
                    onClick = onSignOut,
                    enabled = !authUiState.isBusy,
                ) {
                    Text("Kijelentkezés")
                }
            }
        } else {
            Button(
                onClick = onSignInWithGoogle,
                enabled = !authUiState.isBusy,
            ) {
                Text(if (authUiState.isBusy) "Megnyitás..." else "Bejelentkezés Google-lel")
            }
        }
    }
}
