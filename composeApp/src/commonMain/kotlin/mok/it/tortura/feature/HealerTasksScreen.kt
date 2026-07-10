package mok.it.tortura.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mok.it.tortura.model.HealingTask
import mok.it.tortura.model.Team
import mok.it.tortura.ui.components.*
import mok.it.tortura.ui.theme.AppTheme
import mok.it.tortura.ui.theme.AppThemeTokens

@Composable
fun HealerTasksScreen(
    activeGameName: String,
    activeLocationName: String?,
    uiState: HealerTasksUiState = HealerTasksUiState(),
    onLoad: () -> Unit = {},
    onSelectHealingTask: (Long) -> Unit = {},
    onCompleteHealing: (Long, Long) -> Unit = { _, _ -> },
    onClearMessages: () -> Unit = {},
    onBack: () -> Unit = {},
    onChangeLocation: () -> Unit = {},
) {
    var isHealDialogVisible by rememberSaveable { mutableStateOf(false) }
    var selectedFailedLedgerId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        onLoad()
    }

    val selectedHealingTask = remember(uiState.selectedHealingTaskId, uiState.healingTasks) {
        uiState.healingTasks.firstOrNull { it.task.id == uiState.selectedHealingTaskId }
    }
    val hasHealableFailedTask = uiState.healableFailedTasks.isNotEmpty()

    if (isHealDialogVisible && selectedHealingTask != null) {
        HealFailedTaskDialog(
            failedTasks = uiState.healableFailedTasks,
            selectedFailedLedgerId = selectedFailedLedgerId,
            onSelectFailedLedgerId = { selectedFailedLedgerId = it },
            onDismiss = {
                isHealDialogVisible = false
                selectedFailedLedgerId = null
            },
            onConfirm = {
                val healingTaskId = selectedHealingTask.task.id
                val healedLedgerId = selectedFailedLedgerId
                if (healingTaskId != null && healedLedgerId != null) {
                    onCompleteHealing(healingTaskId, healedLedgerId)
                    isHealDialogVisible = false
                    selectedFailedLedgerId = null
                }
            },
        )
    }

    PageScaffold(modifier = Modifier.verticalScroll(rememberScrollState())) {
        PageHeader(
            title = activeGameName,
            description = buildString {
                append("Csapat: ")
                append(uiState.team?.name ?: "Csapat #${uiState.team?.id ?: "-"}")
                append(" • ")
                append(activeLocationName?.let { "Helyszín: $it" } ?: "Nincs kiválasztott helyszín")
            },
            trailingContent = {
                AppButton(
                    text = "Vissza",
                    onClick = onBack,
                    style = AppButtonStyle.Ghost,
                )
                AppButton(
                    text = "Helyszín váltása",
                    onClick = onChangeLocation,
                    style = AppButtonStyle.Secondary,
                )
            },
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.xl),
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (uiState.errorMessage != null || uiState.message != null) {
                StatusBanner(
                    message = uiState.errorMessage ?: uiState.message.orEmpty(),
                    tone = if (uiState.errorMessage != null) BannerTone.Error else BannerTone.Success,
                    onDismiss = onClearMessages,
                )
            }

            SummarySection(
                teamName = uiState.team?.name ?: "Csapat #${uiState.team?.id ?: "-"}",
                healableCount = uiState.healableFailedTasks.size,
                hasHealableFailedTask = hasHealableFailedTask,
            )

            FormSection(
                title = "Gyógyító feladatok",
                description = "Válassz egy gyógyító feladatot, majd rögzítsd, melyik korábbi hibát javítja.",
            ) {
                if (uiState.healingTasks.isEmpty()) {
                    EmptyState(
                        title = "Nincs gyógyító feladat",
                        description = "Ehhez a játékhoz még nincs gyógyító feladat felvéve.",
                    )
                } else {
                    uiState.healingTasks.forEach { listItem ->
                        val isSelected = listItem.task.id == uiState.selectedHealingTaskId
                        HealingTaskRow(
                            listItem = listItem,
                            isSelected = isSelected,
                            enabled = listItem.task.id != null && !uiState.isLoading,
                            onSelect = { listItem.task.id?.let(onSelectHealingTask) },
                        )
                    }
                }
            }

            FormSection(
                title = "Kiválasztott gyógyító feladat",
                description = "Ellenőrizd a kiválasztott feladatot, majd rögzítsd a gyógyítást.",
            ) {
                if (selectedHealingTask == null) {
                    EmptyState(
                        title = "Még nincs kiválasztott feladat",
                        description = "Válassz egy gyógyító feladatot a fenti listából.",
                    )
                } else {
                    SelectedHealingTaskCard(selectedHealingTask = selectedHealingTask)
                }

                AppButton(
                    text = "Kész, gyógyítás rögzítése",
                    onClick = {
                        selectedFailedLedgerId = null
                        isHealDialogVisible = true
                    },
                    enabled = !uiState.isLoading && selectedHealingTask?.task?.id != null && hasHealableFailedTask,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SummarySection(
    teamName: String,
    healableCount: Int,
    hasHealableFailedTask: Boolean,
) {
    val colors = AppThemeTokens.colors
    val spacing = AppThemeTokens.spacing

    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = teamName,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Gyógyítható elbukott feladatok: $healableCount",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!hasHealableFailedTask) {
                StatusBanner(
                    message = "Ennél a csapatnál jelenleg nincs gyógyítható elbukott feladat," +
                        " ezért a gyógyítás rögzítése le van tiltva.",
                    tone = BannerTone.Warning,
                )
            } else {
                Text(
                    text = "Válassz feladatot, majd rendeld hozzá egy korábbi elbukott próbához.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun HealingTaskRow(
    listItem: HealingTaskListItem,
    isSelected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    SectionCard(toned = isSelected) {
        HealingTaskButtonContent(
            text = listItem.task.text,
            isPreviouslyChosen = listItem.isPreviouslyChosen,
        )
        AppButton(
            text = if (isSelected) "Kiválasztva" else "Feladat kiválasztása",
            onClick = onSelect,
            enabled = enabled,
            style = if (isSelected) AppButtonStyle.Primary else AppButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SelectedHealingTaskCard(selectedHealingTask: HealingTaskListItem) {
    val colors = AppThemeTokens.colors
    val spacing = AppThemeTokens.spacing

    SectionCard(toned = true) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            if (selectedHealingTask.isPreviouslyChosen) {
                Text(
                    text = "Ezt a gyógyító feladatot a csapat már választotta korábban.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.accent,
                )
            }
            Text(
                text = selectedHealingTask.task.text,
                style = MaterialTheme.typography.bodyLarge,
            )
            selectedHealingTask.task.solution?.takeIf { it.isNotBlank() }?.let { solution ->
                Text(
                    text = "Megoldás: $solution",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun HealFailedTaskDialog(
    failedTasks: List<FailedTaskAttempt>,
    selectedFailedLedgerId: Long?,
    onSelectFailedLedgerId: (Long) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Melyik elbukott feladatot gyógyítjátok?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.sm)) {
                failedTasks.forEach { failedTask ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        RadioButton(
                            selected = selectedFailedLedgerId == failedTask.ledgerId,
                            onClick = { onSelectFailedLedgerId(failedTask.ledgerId) },
                        )
                        Column(
                            modifier = Modifier.padding(start = AppThemeTokens.spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(failedTask.taskText)
                            Text(
                                text = "Próba ID ${failedTask.ledgerId}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Rögzítés",
                onClick = onConfirm,
                enabled = selectedFailedLedgerId != null,
            )
        },
        dismissButton = {
            AppButton(
                text = "Mégse",
                onClick = onDismiss,
                style = AppButtonStyle.Ghost,
            )
        },
    )
}

@Composable
private fun HealingTaskButtonContent(
    text: String,
    isPreviouslyChosen: Boolean,
) {
    val colors = AppThemeTokens.colors
    val spacing = AppThemeTokens.spacing

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(text)
        if (isPreviouslyChosen) {
            Text(
                text = "Korábban már választotta ez a csapat",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HealerTasksScreenPreview() {
    AppTheme {
        HealerTasksScreen(
            activeGameName = "Tortura 2026",
            activeLocationName = "Várkapu",
            uiState = HealerTasksUiState(
                team = Team(
                    id = 107,
                    name = "Kecskesajt",
                    teamAssignmentId = 1,
                ),
                healingTasks = listOf(
                    HealingTaskListItem(
                        task = HealingTask(
                            id = 1,
                            text = "Énekeljetek el egy versszakot a kedvenc dalotokból.",
                            solution = null,
                            gameId = 12,
                        ),
                        isPreviouslyChosen = false,
                    ),
                    HealingTaskListItem(
                        task = HealingTask(
                            id = 2,
                            text = "Készítsetek közös csapatfotót egy piros tárggyal.",
                            solution = "Mutassátok meg a fotót.",
                            gameId = 12,
                        ),
                        isPreviouslyChosen = true,
                    ),
                    HealingTaskListItem(
                        task = HealingTask(
                            id = 3,
                            text = "Mondjatok három dolgot, amiben jó a csapatotok.",
                            solution = null,
                            gameId = 12,
                        ),
                        isPreviouslyChosen = false,
                    ),
                ),
                healableFailedTasks = listOf(
                    FailedTaskAttempt(
                        ledgerId = 31,
                        taskId = 2001,
                        taskText = "Rakjátok sorrendbe a megadott történelmi eseményeket.",
                    ),
                    FailedTaskAttempt(
                        ledgerId = 44,
                        taskId = 2005,
                        taskText = "Oldjátok meg a logikai rácsos feladatot.",
                    ),
                ),
                selectedHealingTaskId = 2,
            ),
        )
    }
}
