package mok.it.tortura.feature

import NavigateBackIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mok.it.tortura.model.Team

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealerTeamSelectionScreen(
    activeGameName: String,
    uiState: HealerTeamSelectionUiState = HealerTeamSelectionUiState(),
    onLoad: () -> Unit = {},
    onSelectTeam: (Team) -> Unit = {},
    onClearMessages: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        onLoad()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gyógyító csapat választása") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        NavigateBackIcon()
                    }
                },
            )
        },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text("Aktív játék: $activeGameName", style = MaterialTheme.typography.titleMedium)
            Text(
                "Válassz csapatot a gyógyító feladatokhoz.",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (uiState.teams.isEmpty() && !uiState.isLoading) {
                Text("Ehhez a játékhoz még nincs csapat.", style = MaterialTheme.typography.bodyLarge)
            }

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

@Composable
private fun TeamSelectionRow(
    team: Team,
    isLoading: Boolean,
    onSelectTeam: (Team) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = team.name ?: "Csapat #${team.id ?: "-"}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "ID ${team.id ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = { onSelectTeam(team) },
                enabled = !isLoading && team.id != null,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Megnyitás")
            }
        }
    }
}
