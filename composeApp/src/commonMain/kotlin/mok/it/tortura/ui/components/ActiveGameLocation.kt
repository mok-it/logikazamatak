package mok.it.tortura.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mok.it.tortura.model.Location

@Composable
fun ActiveGameLocationHeader(
    activeGameName: String,
    activeLocationName: String?,
    onChangeLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Aktív játék: $activeGameName",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Állomás: ${activeLocationName ?: "Nincs helyszín kiválasztva"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            AssistChip(
                onClick = onChangeLocation,
                label = { Text("Állomás váltása") },
            )
        }
    }
}

@Composable
fun LocationPickerDialog(
    locations: List<Location>,
    selectedLocationId: Long?,
    onSelectLocation: (Location) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Állomás váltása") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                locations.forEach { location ->
                    val isSelected = location.id == selectedLocationId
                    TextButton(
                        onClick = { onSelectLocation(location) },
                        enabled = location.id != null,
                    ) {
                        val label = location.name ?: "Állomás #${location.id ?: "-"}"
                        Text(if (isSelected) "$label (jelenlegi)" else label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bezárás")
            }
        },
    )
}
