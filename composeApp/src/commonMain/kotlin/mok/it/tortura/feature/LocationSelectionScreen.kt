package mok.it.tortura.feature

import NavigateBackIcon
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mok.it.tortura.model.Game
import mok.it.tortura.model.Location

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionScreen(
    game: Game,
    locations: List<Location>,
    onSelectLocation: (Location) -> Unit = {},
    onBack: () -> Unit = {},
) {
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
            Text(
                text = "Játék: ${game.name ?: "#${game.id ?: "-"}"}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Válaszd ki, melyik állomáson vagytok.",
                style = MaterialTheme.typography.bodyLarge,
            )

            locations.forEach { location ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
//                    Column(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(16.dp),
//                        verticalArrangement = Arrangement.spacedBy(12.dp),
//                    ) {
                    Text(
                        text = location.name ?: "Állomás #${location.id ?: "-"}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Button(
                        onClick = { onSelectLocation(location) },
                        enabled = location.id != null,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Kiválasztás")
                    }
//                    }
                }
            }
        }
    }
}
