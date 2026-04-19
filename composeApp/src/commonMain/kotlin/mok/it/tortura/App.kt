package mok.it.tortura

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import mok.it.tortura.navigation.NavGraph

@Composable
@Preview
fun App() {
    MaterialTheme {
        NavGraph()
    }
}
