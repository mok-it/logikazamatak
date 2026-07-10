package mok.it.tortura

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import mok.it.tortura.navigation.NavGraph
import mok.it.tortura.ui.components.PlatformToastHost
import mok.it.tortura.ui.theme.AppTheme

@Composable
@Preview(showBackground = true)
fun App() {
    AppTheme {
        Box {
            NavGraph()
            PlatformToastHost()
        }
    }
}
