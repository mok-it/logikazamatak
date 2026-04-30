package mok.it.tortura.feature

import NavigateBackIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamCompositionScreen(
    activeGameName: String,
    uiState: TeamCompositionUiState = TeamCompositionUiState(),
    onLoad: () -> Unit = {},
    onBaseTeamCounterChange: (String) -> Unit = {},
    onImportTextChange: (String) -> Unit = {},
    onImportFromText: () -> Unit = {},
    onReadClipboard: () -> Unit = {},
    onImportFromFile: () -> Unit = {},
    onAddTeam: () -> Unit = {},
    onRemoveTeam: (Int) -> Unit = {},
    onTeamNameChange: (Int, String) -> Unit = { _, _ -> },
    onTeamGroupChange: (Int, String) -> Unit = { _, _ -> },
    onTeamKlassChange: (Int, String) -> Unit = { _, _ -> },
    onAddStudent: (Int) -> Unit = {},
    onRemoveStudent: (Int, Int) -> Unit = { _, _ -> },
    onStudentNameChange: (Int, Int, String) -> Unit = { _, _, _ -> },
    onSave: () -> Unit = {},
    onClearMessages: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        onLoad()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Csapatösszeállítás") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        NavigateBackIcon()
                    }
                },
            )
        },
        snackbarHost = {
            if (uiState.errorMessage != null || uiState.message != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = onClearMessages) {
                            Text("OK")
                        }
                    },
                ) {
                    Text(uiState.errorMessage ?: uiState.message.orEmpty())
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (uiState.isLoading || uiState.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Aktív játék: $activeGameName", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Ez a képernyő a játéklétrehozási folyamat utolsó lépése lesz. Most ideiglenesen az előkészítés menüből érhető el.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            AssignmentSection(
                baseTeamCounter = uiState.baseTeamCounter,
                onBaseTeamCounterChange = onBaseTeamCounterChange,
                onSave = onSave,
                isBusy = uiState.isLoading || uiState.isSaving,
                persistedSnapshot = uiState.persistedSnapshot,
            )

            HorizontalDivider()

            ImportSection(
                uiState = uiState,
                onImportTextChange = onImportTextChange,
                onImportFromText = onImportFromText,
                onReadClipboard = onReadClipboard,
                onImportFromFile = onImportFromFile,
            )

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Csapatok", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = onAddTeam,
                        enabled = !uiState.isLoading && !uiState.isSaving,
                    ) {
                        Text("Csapat hozzáadása")
                    }
                }

                if (uiState.teams.isEmpty()) {
                    Text("Még nincs csapat a vázlatban.", style = MaterialTheme.typography.bodyMedium)
                }

                uiState.teams
                    .withIndex()
                    .groupBy { it.value.group.trim() to it.value.klass.trim() }
                    .forEach { (category, teams) ->
                        CategorySection(
                            group = category.first,
                            klass = category.second,
                            teams = teams,
                            onRemoveTeam = onRemoveTeam,
                            onTeamNameChange = onTeamNameChange,
                            onTeamGroupChange = onTeamGroupChange,
                            onTeamKlassChange = onTeamKlassChange,
                            onAddStudent = onAddStudent,
                            onRemoveStudent = onRemoveStudent,
                            onStudentNameChange = onStudentNameChange,
                            isBusy = uiState.isLoading || uiState.isSaving,
                        )
                    }
            }
        }
    }
}

@Composable
private fun AssignmentSection(
    baseTeamCounter: String,
    onBaseTeamCounterChange: (String) -> Unit,
    onSave: () -> Unit,
    isBusy: Boolean,
    persistedSnapshot: TeamCompositionSnapshot?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Beállítások", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = baseTeamCounter,
                onValueChange = onBaseTeamCounterChange,
                label = { Text("Alap csapatszám") },
                singleLine = true,
                enabled = !isBusy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onSave,
                enabled = !isBusy,
            ) {
                Text("Mentés")
            }
        }
        persistedSnapshot?.assignment?.let { assignment ->
            Text(
                "Legutóbb mentve: ${persistedSnapshot.teams.size} csapat, ${assignment.baseTeamCounter ?: 0} alapcsapat",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ImportSection(
    uiState: TeamCompositionUiState,
    onImportTextChange: (String) -> Unit,
    onImportFromText: () -> Unit,
    onReadClipboard: () -> Unit,
    onImportFromFile: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Import", style = MaterialTheme.typography.titleMedium)
        Text(
            "Elsődleges út a vágólap vagy táblázatból másolt TSV. Fájlimport CSV/TSV/XLSX forrásokat is fogad.",
            style = MaterialTheme.typography.bodyMedium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onImportFromText,
                enabled = !uiState.isLoading && !uiState.isSaving,
            ) {
                Text("Beillesztett szöveg importja")
            }
            OutlinedButton(
                onClick = onReadClipboard,
                enabled = uiState.clipboardSupported && !uiState.isLoading && !uiState.isSaving,
            ) {
                Text("Olvasás vágólapról")
            }
            OutlinedButton(
                onClick = onImportFromFile,
                enabled = uiState.fileImportSupported && !uiState.isLoading && !uiState.isSaving,
            ) {
                Text("Fájl feltöltése")
            }
        }
        OutlinedTextField(
            value = uiState.importText,
            onValueChange = onImportTextChange,
            label = { Text("CSV / TSV / Batkabank export tartalom") },
            enabled = !uiState.isLoading && !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
            minLines = 6,
        )
        Text(uiState.batkabankMessage, style = MaterialTheme.typography.bodySmall)
        uiState.importPreview?.let { preview ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Import előnézet", style = MaterialTheme.typography.titleSmall)
                    Text("${preview.sourceLabel}: ${preview.teamCount} csapat, ${preview.studentCount} tanuló")
                    if (preview.categories.isNotEmpty()) {
                        Text(
                            "Kategóriák: ${preview.categories.joinToString { "${it.klass}/${it.group}" }}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (preview.rowErrors.isNotEmpty()) {
                        Text("Soros hibák:", style = MaterialTheme.typography.labelLarge)
                        preview.rowErrors.take(6).forEach { error ->
                            Text("Sor ${error.rowNumber}: ${error.message}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySection(
    group: String,
    klass: String,
    teams: List<IndexedValue<TeamDraft>>,
    onRemoveTeam: (Int) -> Unit,
    onTeamNameChange: (Int, String) -> Unit,
    onTeamGroupChange: (Int, String) -> Unit,
    onTeamKlassChange: (Int, String) -> Unit,
    onAddStudent: (Int) -> Unit,
    onRemoveStudent: (Int, Int) -> Unit,
    onStudentNameChange: (Int, Int, String) -> Unit,
    isBusy: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Kategória: ${klass.ifBlank { "?" }} / ${group.ifBlank { "?" }}",
            style = MaterialTheme.typography.titleSmall,
        )
        teams.forEach { indexedTeam ->
            TeamEditorCard(
                team = indexedTeam.value,
                teamIndex = indexedTeam.index,
                onRemoveTeam = onRemoveTeam,
                onTeamNameChange = onTeamNameChange,
                onTeamGroupChange = onTeamGroupChange,
                onTeamKlassChange = onTeamKlassChange,
                onAddStudent = onAddStudent,
                onRemoveStudent = onRemoveStudent,
                onStudentNameChange = onStudentNameChange,
                isBusy = isBusy,
            )
        }
    }
}

@Composable
private fun TeamEditorCard(
    team: TeamDraft,
    teamIndex: Int,
    onRemoveTeam: (Int) -> Unit,
    onTeamNameChange: (Int, String) -> Unit,
    onTeamGroupChange: (Int, String) -> Unit,
    onTeamKlassChange: (Int, String) -> Unit,
    onAddStudent: (Int) -> Unit,
    onRemoveStudent: (Int, Int) -> Unit,
    onStudentNameChange: (Int, Int, String) -> Unit,
    isBusy: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Csapat", style = MaterialTheme.typography.titleSmall)
                TextButton(
                    onClick = { onRemoveTeam(teamIndex) },
                    enabled = !isBusy,
                ) {
                    Text("Törlés")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = team.name,
                    onValueChange = { onTeamNameChange(teamIndex, it) },
                    label = { Text("Csapatnév") },
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = team.group,
                    onValueChange = { onTeamGroupChange(teamIndex, it) },
                    label = { Text("Csoport") },
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = team.klass,
                    onValueChange = { onTeamKlassChange(teamIndex, it) },
                    label = { Text("Osztály") },
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                team.students.forEachIndexed { studentIndex, student ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = student.name,
                            onValueChange = { onStudentNameChange(teamIndex, studentIndex, it) },
                            label = { Text("Tanuló neve") },
                            enabled = !isBusy,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = { onRemoveStudent(teamIndex, studentIndex) },
                            enabled = !isBusy,
                        ) {
                            Text("Eltávolítás")
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { onAddStudent(teamIndex) },
                enabled = !isBusy,
            ) {
                Text("Tanuló hozzáadása")
            }
        }
    }
}
