package mok.it.tortura

import android.os.Build
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.writeString
import mok.it.tortura.model.Game
import mok.it.tortura.model.TeamAssignment

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val excelImportImplemented = false
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual suspend fun saveStringToFile(
    file: PlatformFile,
    string: String,
) {
    file.writeString(string)
}

actual fun loadGameFromExcel(file: PlatformFile): Game? =
    throw Exception("No excel import available for android")

actual fun loadTeamAssignmentFromExcel(file: PlatformFile): TeamAssignment? =
    throw Exception("No excel import available for android")

actual fun goodNightGoodBye() {
    System.exit(0)
}
