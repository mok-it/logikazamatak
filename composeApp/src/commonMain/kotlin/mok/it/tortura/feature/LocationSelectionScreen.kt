package mok.it.tortura.feature

import NavigateBackIcon
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mok.it.tortura.model.Game
import mok.it.tortura.model.Location

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionScreen(
    uiState: LocationSelectionUiState = LocationSelectionUiState(),
    onLoad: () -> Unit = {},
    onSelectLocation: (Long) -> Unit = {},
    onBack: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        onLoad()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Állomás kiválasztása") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        NavigateBackIcon()
                    }
                },
            )
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
                text = "Játék: ${uiState.game?.name ?: "#${uiState.game?.id ?: "-"}"}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Válaszd ki, melyik állomáson vagytok.",
                style = MaterialTheme.typography.bodyLarge,
            )

            uiState.errorMessage?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }

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

@Preview
@Composable
private fun LocationSelectionScreenPreview() {
    MaterialTheme {
        LocationSelectionScreen(
            uiState = LocationSelectionUiState(
                game = Game(
                    id = 12,
                    name = "Tortura 2026",
                ),
                locations = listOf(
                    Location(
                        id = 101,
                        name = "Várkapu",
                        gameId = 12,
                    ),
                    Location(
                        id = 102,
                        name = "Könyvtár",
                        gameId = 12,
                    ),
                    Location(
                        id = 103,
                        name = "Boszorkánytorony",
                        gameId = 12,
                    ),
                ),
            ),
        )
    }
}
