package mok.it.tortura.feature

import NavigateBackIcon
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealerTasksScreen(
    activeGameName: String,
    uiState: HealerTasksUiState = HealerTasksUiState(),
    onLoad: () -> Unit = {},
    onSelectHealingTask: (Long) -> Unit = {},
    onCompleteHealing: (Long, Long) -> Unit = { _, _ -> },
    onClearMessages: () -> Unit = {},
    onBack: () -> Unit = {},
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
        AlertDialog(
            onDismissRequest = { isHealDialogVisible = false },
            title = { Text("Melyik elbukott feladatot gyógyítjátok?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.healableFailedTasks.forEach { failedTask ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            RadioButton(
                                selected = selectedFailedLedgerId == failedTask.ledgerId,
                                onClick = { selectedFailedLedgerId = failedTask.ledgerId },
                            )
                            Column(
                                modifier = Modifier.padding(start = 8.dp),
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
                TextButton(
                    onClick = {
                        val healingTaskId = selectedHealingTask.task.id
                        val healedLedgerId = selectedFailedLedgerId
                        if (healingTaskId != null && healedLedgerId != null) {
                            onCompleteHealing(healingTaskId, healedLedgerId)
                            isHealDialogVisible = false
                            selectedFailedLedgerId = null
                        }
                    },
                    enabled = selectedFailedLedgerId != null,
                ) {
                    Text("Rögzítés")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isHealDialogVisible = false
                        selectedFailedLedgerId = null
                    },
                ) {
                    Text("Mégse")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gyógyító feladat") },
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text("Aktív játék: $activeGameName", style = MaterialTheme.typography.titleMedium)
            Text(
                "Csapat: ${uiState.team?.name ?: "Csapat #${uiState.team?.id ?: "-"}"}",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "Gyógyítható elbukott feladatok: ${uiState.healableFailedTasks.size}",
                style = MaterialTheme.typography.bodyLarge,
            )

            if (!hasHealableFailedTask) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Text(
                        text = @Suppress("ktlint:standard:max-line-length")
                        "Ennél a csapatnál jelenleg nincs gyógyítható elbukott feladat, ezért a gyógyítás rögzítése le van tiltva.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Gyógyító feladatok", style = MaterialTheme.typography.titleMedium)
                if (uiState.healingTasks.isEmpty()) {
                    Text(
                        "Ehhez a játékhoz még nincs gyógyító feladat felvéve.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                uiState.healingTasks.forEach { listItem ->
                    val isSelected = listItem.task.id == uiState.selectedHealingTaskId
                    if (isSelected) {
                        Button(
                            onClick = { listItem.task.id?.let(onSelectHealingTask) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = listItem.task.id != null && !uiState.isLoading,
                        ) {
                            HealingTaskButtonContent(
                                text = listItem.task.text,
                                isPreviouslyChosen = listItem.isPreviouslyChosen,
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = { listItem.task.id?.let(onSelectHealingTask) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = listItem.task.id != null && !uiState.isLoading,
                        ) {
                            HealingTaskButtonContent(
                                text = listItem.task.text,
                                isPreviouslyChosen = listItem.isPreviouslyChosen,
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Kiválasztott gyógyító feladat",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (selectedHealingTask == null) {
                        Text("Válassz egy gyógyító feladatot a listából.")
                    } else {
                        if (selectedHealingTask.isPreviouslyChosen) {
                            Text(
                                "Ezt a gyógyító feladatot a csapat már választotta korábban.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            selectedHealingTask.task.text,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        selectedHealingTask.task.solution?.takeIf { it.isNotBlank() }
                            ?.let { solution ->
                                Text(
                                    "Megoldás: $solution",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                    }

                    Button(
                        onClick = {
                            selectedFailedLedgerId = null
                            isHealDialogVisible = true
                        },
                        enabled =
                            !uiState.isLoading && selectedHealingTask?.task?.id != null &&
                                hasHealableFailedTask,
                    ) {
                        Text("Kész, gyógyítás rögzítése")
                    }
                }
            }
        }
    }
}

@Composable
private fun HealingTaskButtonContent(
    text: String,
    isPreviouslyChosen: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text)
        if (isPreviouslyChosen) {
            Text(
                "Korábban már választotta ez a csapat",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview
@Composable
private fun HealerTasksScreenPreview() {
    MaterialTheme {
        HealerTasksScreen(
            activeGameName = "Tortura 2026",
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
