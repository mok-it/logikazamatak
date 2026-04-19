package mok.it.tortura.feature

import NavigateBackIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetUpMenu(
    onCompetitionCreation: () -> Unit = {},
    onTeamCreation: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Előkókányolás xd") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        NavigateBackIcon()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround,
        ) {
            Button(onClick = onCompetitionCreation) {
                Text("Feladatsor")
            }
            Button(onClick = onTeamCreation) {
                Text("Csapatok")
            }
        }
    }
}
