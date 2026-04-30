package mok.it.tortura.feature

import NavigateBackIcon
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mok.it.tortura.model.Team
import mok.it.tortura.ui.components.ActiveGameLocationTopBarTitle
import mok.it.tortura.ui.components.ChangeLocationTopBarAction

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ActiveGameLocationTopBarTitle(
                        activeGameName = activeGameName,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        NavigateBackIcon()
                    }
                },
                actions = {
                    ChangeLocationTopBarAction(
                        activeLocationName = activeLocationName,
                        onChangeLocation = onChangeLocation,
                    )
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

            Text(
                "Válassz csapatot a gyógyító feladatokhoz.",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (uiState.teams.isEmpty() && !uiState.isLoading) {
                Text(
                    "Ehhez a játékhoz még nincs csapat.",
                    style = MaterialTheme.typography.bodyLarge,
                )
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
