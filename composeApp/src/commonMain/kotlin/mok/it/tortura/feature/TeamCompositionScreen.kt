package mok.it.tortura.feature

import NavigateBackIcon
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onBatkabankYearInputChange: (String) -> Unit = {},
    onRefreshBatkabankCamps: () -> Unit = {},
    onBatkabankCampSelected: (String?) -> Unit = {},
    onImportFromBatkabank: (CampSearchResultDto, CampSearchAssignmentDto) -> Unit = { _, _ -> },
    onAddTeam: () -> Unit = {},
    onRemoveTeam: (Int) -> Unit = {},
    onTeamNameChange: (Int, String) -> Unit = { _, _ -> },
    onTeamGroupChange: (Int, String) -> Unit = { _, _ -> },
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
                onBatkabankYearInputChange = onBatkabankYearInputChange,
                onRefreshBatkabankCamps = onRefreshBatkabankCamps,
                onBatkabankCampSelected = onBatkabankCampSelected,
                onImportFromBatkabank = onImportFromBatkabank,
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
                    Text(
                        "Még nincs csapat a vázlatban.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                uiState.teams
                    .withIndex()
                    .groupBy { it.value.group.trim() }
                    .forEach { (category, teams) ->
                        CategorySection(
                            group = category,
                            teams = teams,
                            onRemoveTeam = onRemoveTeam,
                            onTeamNameChange = onTeamNameChange,
                            onTeamGroupChange = onTeamGroupChange,
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
    onBatkabankYearInputChange: (String) -> Unit,
    onRefreshBatkabankCamps: () -> Unit,
    onBatkabankCampSelected: (String?) -> Unit,
    onImportFromBatkabank: (CampSearchResultDto, CampSearchAssignmentDto) -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Batkabank import", style = MaterialTheme.typography.titleSmall)
            if (uiState.isBatkabankLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        Text(
            "A táborok már tartalmazzák a feladatlehetőségeket. Az év mező üresen hagyva az API alapértelmezett évét használja.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            OutlinedTextField(
                value = uiState.batkabankYearInput,
                onValueChange = onBatkabankYearInputChange,
                label = { Text("Év (opcionális)") },
                placeholder = { Text("Pl. 2026") },
                singleLine = true,
                enabled = !uiState.isLoading && !uiState.isSaving && !uiState.isBatkabankLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = onRefreshBatkabankCamps,
                enabled = !uiState.isLoading && !uiState.isSaving && !uiState.isBatkabankLoading,
            ) {
                Text("Táborok betöltése")
            }
        }
        DropdownField(
                label = "Tábor",
                value = uiState.batkabankAvailableCamps
                    .firstOrNull { it.id == uiState.batkabankSelectedCampId }
                    ?.name
                    .orEmpty(),
                placeholder = "Válassz tábort",
                options = uiState.batkabankAvailableCamps.map { camp ->
                    DropdownOption(
                        id = camp.id,
                        label = buildString {
                            append(camp.name)
                            val dateParts = listOfNotNull(camp.startsAt, camp.endsAt)
                            if (dateParts.isNotEmpty()) {
                                append(" (")
                                append(dateParts.joinToString(" - "))
                                append(")")
                            }
                        },
                    )
                },
                enabled = !uiState.isLoading &&
                    !uiState.isSaving &&
                    !uiState.isBatkabankLoading &&
                    uiState.batkabankAvailableCamps.isNotEmpty(),
                onSelected = { option -> onBatkabankCampSelected(option?.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        uiState.batkabankAvailableCamps
            .firstOrNull { it.id == uiState.batkabankSelectedCampId }
            ?.let { camp ->
                BatkabankCampCard(
                    camp = camp,
                    onImportFromBatkabank = onImportFromBatkabank,
                    isBusy = uiState.isLoading || uiState.isSaving || uiState.isBatkabankLoading,
                )
            }
        if (
            uiState.batkabankYearInput.isNotBlank() &&
            uiState.batkabankAvailableCamps.isEmpty() &&
            !uiState.isLoading && !uiState.isBatkabankLoading
        ) {
            Text(
                "Ebben az évben nincs importálható Batkabank tábor.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        uiState.importPreview?.let { preview ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Import előnézet", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${preview.sourceLabel}: ${preview.teamCount} csapat, ${preview.studentCount} tanuló",
                    )
                    if (preview.categories.isNotEmpty()) {
                        Text(
                            "Kategóriák: ${
                                preview.categories.joinToString {
                                    it.group
                                }
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (preview.rowErrors.isNotEmpty()) {
                        Text("Soros hibák:", style = MaterialTheme.typography.labelLarge)
                        preview.rowErrors.take(6).forEach { error ->
                            Text(
                                "Sor ${error.rowNumber}: ${error.message}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class DropdownOption(
    val id: String,
    val label: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    placeholder: String,
    options: List<DropdownOption>,
    enabled: Boolean,
    onSelected: (DropdownOption?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            enabled = enabled,
            modifier = Modifier
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = enabled,
                )
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun BatkabankCampCard(
    camp: CampSearchResultDto,
    onImportFromBatkabank: (CampSearchResultDto, CampSearchAssignmentDto) -> Unit,
    isBusy: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(camp.name, style = MaterialTheme.typography.titleSmall)
            val dateParts = listOfNotNull(camp.startsAt, camp.endsAt)
            if (dateParts.isNotEmpty()) {
                Text(dateParts.joinToString(" - "), style = MaterialTheme.typography.bodySmall)
            }
            Text("Tábor azonosító: ${camp.id}", style = MaterialTheme.typography.bodySmall)
            if (camp.assignments.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Nincs importálható feladat a táborban.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    camp.assignments.forEach { assignment ->
                        OutlinedButton(
                            onClick = { onImportFromBatkabank(camp, assignment) },
                            enabled = !isBusy,
                        ) {
                            Text("${assignment.name} (${assignment.groupCount})")
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
    teams: List<IndexedValue<TeamDraft>>,
    onRemoveTeam: (Int) -> Unit,
    onTeamNameChange: (Int, String) -> Unit,
    onTeamGroupChange: (Int, String) -> Unit,
    onAddStudent: (Int) -> Unit,
    onRemoveStudent: (Int, Int) -> Unit,
    onStudentNameChange: (Int, Int, String) -> Unit,
    isBusy: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Kategória: ${group.ifBlank { "?" }}",
            style = MaterialTheme.typography.titleSmall,
        )
        teams.forEach { indexedTeam ->
            TeamEditorCard(
                team = indexedTeam.value,
                teamIndex = indexedTeam.index,
                onRemoveTeam = onRemoveTeam,
                onTeamNameChange = onTeamNameChange,
                onTeamGroupChange = onTeamGroupChange,
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
