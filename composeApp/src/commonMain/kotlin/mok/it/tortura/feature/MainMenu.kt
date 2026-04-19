package mok.it.tortura.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun MainMenu(
    onSetUp: (() -> Unit),
    onCompetition: (() -> Unit),
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = onSetUp) {
            Text(text = "Előkészítés")
        }
        Button(onClick = onCompetition) {
            Text(text = "Tortúra!!! (már nem, csak de)")
        }
    }
}
