package mok.it.tortura.feature

import NavigateBackIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
