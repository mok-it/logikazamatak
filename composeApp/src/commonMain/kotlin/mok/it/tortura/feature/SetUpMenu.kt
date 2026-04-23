package mok.it.tortura.feature

import NavigateBackIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetUpMenu(
    activeGameName: String,
    uiState: SetupUiState = SetupUiState(),
    onLoad: () -> Unit = {},
    onBaseTeamCounterChange: (String) -> Unit = {},
    onTeamCreation: () -> Unit = {},
    onClearMessages: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        onLoad()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Előkókányolás") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        NavigateBackIcon()
                    }
                }
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
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text("Aktív játék: $activeGameName", style = MaterialTheme.typography.titleMedium)

            HorizontalDivider()

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Csapatok", style = MaterialTheme.typography.titleMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = uiState.baseTeamCounter,
                        onValueChange = onBaseTeamCounterChange,
                        label = { Text("Alap csapatszám") },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = onTeamCreation,
                        enabled = !uiState.isLoading,
                    ) {
                        Text("Mentés")
                    }
                }
                ExistingRows(
                    title = "Mentett csapatbeosztások",
                    rows = uiState.teamAssignments.map { assignment ->
                        "ID ${assignment.id ?: "-"}: ${assignment.baseTeamCounter ?: 0} csapat"
                    },
                )
            }

            OutlinedButton(
                onClick = onLoad,
                enabled = !uiState.isLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Frissítés")
            }
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
        Text("Nincs mentett adat", style = MaterialTheme.typography.bodyMedium)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Text(row, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
