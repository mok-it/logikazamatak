package mok.it.logikazamatak

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    val icon = painterResource("icon.png")

    Window(
        onCloseRequest = ::exitApplication,
        title = "Logikazamatak",
        icon = icon,
    ) {
        App()
    }
}
