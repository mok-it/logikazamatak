package mok.it.tortura

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import mok.it.tortura.navigation.NavGraph
import mok.it.tortura.ui.theme.AppTheme

@Composable
@Preview
fun App() {
    AppTheme {
        NavGraph()
    }
}
