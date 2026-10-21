package mok.it.tortura.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mok.it.tortura.ui.components.*
import mok.it.tortura.ui.theme.AppThemeTokens

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
    LaunchedEffect(Unit) { onLoad() }
    val busy = uiState.isLoading || uiState.isSaving || uiState.isBatkabankLoading
    val spacing = AppThemeTokens.spacing
    var campExpanded by remember { mutableStateOf(false) }

    PageScaffold(modifier = Modifier.verticalScroll(rememberScrollState())) {
        PageHeader(title = "Csapatösszeállítás", description = "Aktív játék: $activeGameName") {
            AppButton("Vissza", onBack, style = AppButtonStyle.Ghost, leadingIcon = { NavigateBackIcon() })
        }
        uiState.errorMessage?.let {
            StatusBanner(it, BannerTone.Error, onDismiss = onClearMessages)
        }
        uiState.message?.let {
            StatusBanner(it, BannerTone.Success, onDismiss = onClearMessages)
        }
        FormSection(title = "Beállítások") {
            AppNumberField(
                value = uiState.baseTeamCounter,
                onValueChange = onBaseTeamCounterChange,
                label = "Alap csapatszám",
                enabled = !busy,
            )
            uiState.persistedSnapshot?.assignment?.let { assignment ->
                Text(
                    "Mentett beosztás: ${uiState.persistedSnapshot.teams.size} csapat, ${assignment.baseTeamCounter} alapcsapat",
                )
            }
        }
        FormSection(
            title = "Import",
            description = "Az import a vázlatot cseréli le. Mentés előtt ellenőrizd a csapatokat.",
        ) {
            AppTextField(
                value = uiState.importText,
                onValueChange = onImportTextChange,
                label = "CSV / TSV tartalom",
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                enabled = !busy,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                AppButton("Szöveg importja", onImportFromText, enabled = !busy)
                AppButton(
                    "Vágólap",
                    onReadClipboard,
                    style = AppButtonStyle.Secondary,
                    enabled =
                        !busy && uiState.clipboardSupported,
                )
                AppButton(
                    "CSV / TSV / XLSX fájl",
                    onImportFromFile,
                    style = AppButtonStyle.Secondary,
                    enabled =
                        !busy && uiState.fileImportSupported,
                )
            }
            if (!uiState.fileImportSupported || !uiState.clipboardSupported) {
                Text(
                    "Fájlimport és közvetlen vágólapolvasás a webes alkalmazásban érhető el. Itt szöveget illeszthetsz be.",
                )
            }
            uiState.importPreview?.let { preview ->
                SectionCard(toned = true) {
                    Text("${preview.sourceLabel}: ${preview.teamCount} csapat, ${preview.studentCount} tanuló")
                    preview.rowErrors.forEach { error ->
                        Text("Sor ${error.rowNumber}: ${error.message}", color = AppThemeTokens.colors.danger)
                    }
                }
            }
        }
        FormSection(title = "Batkabank") {
            if (!uiState.batkabankEnabled) {
                Text("A Batkabank import ehhez az alkalmazáshoz nincs beállítva.")
            } else {
                AppNumberField(
                    value = uiState.batkabankYearInput,
                    onValueChange = onBatkabankYearInputChange,
                    label = "Év (opcionális)",
                    enabled = !busy,
                )
                AppButton(
                    "Táborok betöltése",
                    onRefreshBatkabankCamps,
                    style = AppButtonStyle.Secondary,
                    enabled = !busy,
                    loading = uiState.isBatkabankLoading,
                    leadingIcon = { RefreshIcon() },
                )
                val selectedCamp = uiState.batkabankAvailableCamps.firstOrNull {
                    it.id ==
                        uiState.batkabankSelectedCampId
                }
                AppSelectField(
                    value = selectedCamp?.name.orEmpty(),
                    label = "Tábor",
                    expanded = campExpanded,
                    options = uiState.batkabankAvailableCamps.map { AppSelectOption(it.id, it.name) },
                    onExpandedChange = { campExpanded = it },
                    onSelect = { onBatkabankCampSelected(it.value) },
                    enabled = !busy && uiState.batkabankAvailableCamps.isNotEmpty(),
                )
                selectedCamp?.let { camp ->
                    if (camp.assignments.isEmpty()) Text("Nincs importálható feladat a táborban.")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        camp.assignments.forEach { assignment ->
                            AppButton(
                                "${assignment.name} (${assignment.groupCount})",
                                { onImportFromBatkabank(camp, assignment) },
                                style = AppButtonStyle.Secondary,
                                enabled = !busy,
                            )
                        }
                    }
                }
            }
        }
        FormSection(
            title = "Csapatok",
            headerAction = {
                AppButton("Csapat hozzáadása", onAddTeam, enabled = !busy, leadingIcon = { AddIcon() })
            },
        ) {
            if (uiState.teams.isEmpty()) {
                EmptyState("Még nincs csapat", "Adj hozzá csapatot, vagy importálj egy beosztást.")
            }
            uiState.teams.forEachIndexed { teamIndex, team ->
                SectionCard(toned = true) {
                    Text("${teamIndex + 1}. csapat", style = MaterialTheme.typography.titleMedium)
                    BoxWithConstraints {
                        if (maxWidth >= 600.dp) {
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                                AppTextField(team.name, {
                                    onTeamNameChange(teamIndex, it)
                                }, "Csapatnév", modifier = Modifier.weight(1f), enabled = !busy)
                                AppTextField(team.group, {
                                    onTeamGroupChange(teamIndex, it)
                                }, "Csoport", modifier = Modifier.weight(1f), enabled = !busy)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                AppTextField(team.name, {
                                    onTeamNameChange(teamIndex, it)
                                }, "Csapatnév", modifier = Modifier.fillMaxWidth(), enabled = !busy)
                                AppTextField(team.group, {
                                    onTeamGroupChange(teamIndex, it)
                                }, "Csoport", modifier = Modifier.fillMaxWidth(), enabled = !busy)
                            }
                        }
                    }
                    team.students.forEachIndexed { studentIndex, student ->
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                            AppTextField(
                                student.name,
                                { onStudentNameChange(teamIndex, studentIndex, it) },
                                "Tanuló neve",
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !busy,
                            )
                            AppButton(
                                "Tanuló eltávolítása",
                                { onRemoveStudent(teamIndex, studentIndex) },
                                style = AppButtonStyle.Ghost,
                                enabled = !busy,
                                leadingIcon = { DeleteIcon() },
                            )
                        }
                    }
                    InlineActionRow {
                        AppButton("Tanuló hozzáadása", {
                            onAddStudent(teamIndex)
                        }, style = AppButtonStyle.Secondary, enabled = !busy, leadingIcon = { AddIcon() })
                        AppButton("Csapat törlése", {
                            onRemoveTeam(teamIndex)
                        }, style = AppButtonStyle.Danger, enabled = !busy, leadingIcon = { DeleteIcon() })
                    }
                }
            }
        }
        InlineActionRow {
            AppButton("Mentés", onSave, enabled = !busy, loading = uiState.isSaving, leadingIcon = { SaveIcon() })
        }
    }
}
