package mok.it.tortura.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainMenu(
    authUiState: AuthUiState = AuthUiState(isInitializing = false),
    onSignInWithGoogle: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onClearAuthError: () -> Unit = {},
    onSetUp: (() -> Unit),
    onCompetition: (() -> Unit),
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthSection(
            authUiState = authUiState,
            onSignInWithGoogle = onSignInWithGoogle,
            onSignOut = onSignOut,
        )

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
            Text(text = "Tortúra!!! (már nem, csak de)")
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
            Button(
                onClick = onSignOut,
                enabled = !authUiState.isBusy,
            ) {
                Text("Kijelentkezés")
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
