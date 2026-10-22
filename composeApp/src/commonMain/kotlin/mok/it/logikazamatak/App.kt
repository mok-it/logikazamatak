package mok.it.logikazamatak

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import mok.it.logikazamatak.navigation.NavGraph
import mok.it.logikazamatak.ui.components.PlatformToastHost
import mok.it.logikazamatak.ui.theme.AppTheme

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
