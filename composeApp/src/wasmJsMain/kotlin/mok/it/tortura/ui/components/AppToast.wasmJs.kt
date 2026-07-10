package mok.it.tortura.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import multiplatform.network.cmptoast.ToastDuration
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.ToastHost
import multiplatform.network.cmptoast.showToast

@Composable
actual fun PlatformToastHost() {
    ToastHost()
}

actual fun platformShowToast(
    message: String,
    backgroundColor: Color,
    textColor: Color,
    isError: Boolean,
) {
    showToast(
        message = message,
        gravity = ToastGravity.Bottom,
        backgroundColor = backgroundColor,
        textColor = textColor,
        duration = if (isError) ToastDuration.Long else ToastDuration.Short,
        bottomPadding = 56,
    )
}
