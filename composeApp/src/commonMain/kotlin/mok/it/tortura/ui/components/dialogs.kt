package mok.it.tortura.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun HelpDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text("Ha idáig eljutottál inkább kérj segítséget!")
        },
        confirmButton = {
            AppButton(
                text = "Megyek segítséget kérni",
                onClick = onDismiss,
                style = AppButtonStyle.Ghost,
            )
        },
    )
}

@Composable
fun ParseErrorPopup(onDismiss: () -> Unit) {
    GenericErrorPopup(
        "Formátumhiba",
        "A fájl formátuma helytelen, biztosan jót választottál?" +
                "Excel fájlok beolvasása még nem lehetséges, a beosztást az appban hozd létre!",
        onDismiss
    )
}

@Composable
fun TypeErrorPopup(onDismiss: () -> Unit) {
    GenericErrorPopup(
        "Tartalomhiba",
        "A fájl formátuma helyes ,de tartalma nem értelmezhető. Biztos jót választottál?" +
                "Baloldat csapatbeosztást (.csp), jobboldalt feladatsort (.fes) válassz ki!",
        onDismiss
    )
}

@Composable
fun SaveErrorPopup(onDismiss: () -> Unit) {
    GenericErrorPopup(
        "Mentési hiba",
        "A fájl mentése nem sikerült, mert egy vagy több helyen" +
                "nincs csapat vagy feladatsor kiválasztva.",
        onDismiss
    )
}

@Composable
fun GenericErrorPopup(
    title: String,
    text: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        title = { Text(title) },
        text = { Text(text) },
        onDismissRequest = onDismiss,
        confirmButton = {
            AppButton(
                text = "Rendben",
                onClick = onDismiss,
            )
        },
    )
}

@Composable
fun ExcelErrorPopup(
    onDismiss: () -> Unit,
) {
    GenericErrorPopup(
        "Excel hiba",
        "Az Excel fájl beolvasása nem sikerült, mert a fájl formátuma nem megfelelő." +
                "Kérlek ellenőrizd a fájlt és próbáld újra.",
        onDismiss
    )
}

@Composable
fun CsvErrorPopup(onDismiss: () -> Unit) {
    GenericErrorPopup(
        "CSV hiba",
        "A CSV fájl beolvasása nem sikerült, mert a fájl formátuma nem megfelelő." +
                "Kérlek ellenőrizd a fájlt és próbáld újra." +
                "Jelenleg csak a Batkabankból exportált CSV fájlok támogatottak.",
        onDismiss
    )
}
