package mok.it.tortura

import android.os.Build
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.writeString
import mok.it.tortura.model.ProblemSet
import mok.it.tortura.model.TeamAssignment

actual suspend fun saveStringToFile(
    file: PlatformFile,
    string: String,
) {
    file.writeString(string)
}

actual fun loadProblemSetFromExcel(file: PlatformFile): ProblemSet? =
    throw Exception("No excel import available for android")

actual fun loadTeamAssignmentFromExcel(file: PlatformFile): TeamAssignment? =
    throw Exception("No excel import available for android")

actual fun goodNightGoodBye() {
    System.exit(0)
}
