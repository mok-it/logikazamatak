package mok.it.tortura.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mok.it.tortura.model.Game

@Composable
fun GameSelectionScreen(
    uiState: GameSelectionUiState = GameSelectionUiState(),
    onLoad: () -> Unit = {},
    onGameNameChange: (String) -> Unit = {},
    onCreateGame: () -> Unit = {},
    onSelectGame: (Game) -> Unit = {},
    onGameSelected: (Game) -> Unit = {},
    onClearMessages: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        onLoad()
    }

    LaunchedEffect(uiState.selectedGame) {
        uiState.selectedGame?.let(onGameSelected)
    }

    Scaffold(
        snackbarHost = {
            if (uiState.errorMessage != null || uiState.message != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = onClearMessages) {
                            Text("OK")
                        }
                    },
                ) {
                    Text(uiState.errorMessage ?: uiState.message.orEmpty())
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Válassz játékot", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Hozz létre egy új játékot, vagy csatlakozz egy meglévőhöz.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = uiState.gameName,
                    onValueChange = onGameNameChange,
                    label = { Text("Új játék neve") },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = onCreateGame,
                    enabled = !uiState.isLoading,
                ) {
                    Text("Létrehozás")
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Meglévő játékok", style = MaterialTheme.typography.titleMedium)
                if (uiState.games.isEmpty() && !uiState.isLoading) {
                    Text("Nincs még mentett játék", style = MaterialTheme.typography.bodyMedium)
                }
                uiState.games.forEach { game ->
                    GameRow(
                        game = game,
                        isLoading = uiState.isLoading,
                        onSelectGame = onSelectGame,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameRow(
    game: Game,
    isLoading: Boolean,
    onSelectGame: (Game) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(game.name ?: "Névtelen játék", style = MaterialTheme.typography.titleSmall)
                Text("ID ${game.id ?: "-"}", style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { onSelectGame(game) },
                enabled = !isLoading && game.id != null,
            ) {
                Text("Csatlakozás")
            }
        }
    }
}
