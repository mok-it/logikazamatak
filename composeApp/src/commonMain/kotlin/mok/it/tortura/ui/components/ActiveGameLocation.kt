package mok.it.tortura.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import mok.it.tortura.model.Location

@Composable
fun ActiveGameLocationTopBarTitle(activeGameName: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = activeGameName,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun ChangeLocationTopBarAction(
    activeLocationName: String?,
    onChangeLocation: () -> Unit,
) {
    AppButton(
        text = activeLocationName?.let { "Helyszín: $it" } ?: "Nincs helyszín kiválasztva",
        onClick = onChangeLocation,
        style = AppButtonStyle.Ghost,
    )
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
                    AppButton(
                        text = run {
                            val label = location.name ?: "Állomás #${location.id ?: "-"}"
                            if (isSelected) "$label (jelenlegi)" else label
                        },
                        onClick = { onSelectLocation(location) },
                        enabled = location.id != null,
                        style = if (isSelected) AppButtonStyle.Primary else AppButtonStyle.Ghost,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = "Bezárás",
                onClick = onDismiss,
                style = AppButtonStyle.Ghost,
            )
        },
    )
}
