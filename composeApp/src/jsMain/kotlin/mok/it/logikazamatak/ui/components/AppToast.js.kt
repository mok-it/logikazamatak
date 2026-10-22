package mok.it.logikazamatak.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun PlatformToastHost() = Unit

actual fun platformShowToast(
    message: String,
    backgroundColor: Color,
    textColor: Color,
    isError: Boolean,
) = Unit
