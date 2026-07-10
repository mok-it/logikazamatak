package mok.it.tortura.feature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mok.it.tortura.model.Location
import mok.it.tortura.model.Task
import mok.it.tortura.model.Team
import mok.it.tortura.model.TeamProgressSummary
import mok.it.tortura.ui.components.AppButton
import mok.it.tortura.ui.components.AppButtonStyle
import mok.it.tortura.ui.components.AppSelectOption
import mok.it.tortura.ui.components.AppTextField
import mok.it.tortura.ui.components.ChangeLocationIcon
import mok.it.tortura.ui.components.CorrectIcon
import mok.it.tortura.ui.components.EmptyState
import mok.it.tortura.ui.components.FormSection
import mok.it.tortura.ui.components.IncorrectIcon
import mok.it.tortura.ui.components.NavigateBackIcon
import mok.it.tortura.ui.components.PageHeader
import mok.it.tortura.ui.components.PageScaffold
import mok.it.tortura.ui.components.SaveIcon
import mok.it.tortura.ui.components.SectionCard
import mok.it.tortura.ui.components.TeamInfoDisclosure
import mok.it.tortura.ui.components.TransientToastEffect
import mok.it.tortura.ui.theme.AppTheme
import mok.it.tortura.ui.theme.AppThemeTokens

@Composable
fun LocationTasksScreen(
    activeGameName: String,
    activeLocation: Location,
    uiState: LocationTasksUiState = LocationTasksUiState(),
    onLoad: () -> Unit = {},
    onSelectTeam: (Long) -> Unit = {},
    onAnswerChange: (Long, String) -> Unit = { _, _ -> },
    onSubmitAnswer: (Long) -> Unit = {},
    onClearMessages: () -> Unit = {},
    onBack: () -> Unit = {},
    onChangeLocation: () -> Unit = {},
) {
    val spacing = AppThemeTokens.spacing
    val selectedTeam = remember(uiState.selectedTeamId, uiState.teams) {
        uiState.teams.firstOrNull { it.id == uiState.selectedTeamId }
    }

    LaunchedEffect(activeLocation.id) {
        onLoad()
    }

    PageScaffold(modifier = Modifier.verticalScroll(rememberScrollState())) {
        PageHeader(
            title = activeLocation.name ?: "Állomás",
            description = "$activeGameName • feladatbeküldés",
            trailingContent = {
                AppButton(
                    text = "Vissza",
                    onClick = onBack,
                    style = AppButtonStyle.Ghost,
                    leadingIcon = { NavigateBackIcon() },
                )
                AppButton(
                    text = currentLocationButtonLabel(activeLocation.name),
                    onClick = onChangeLocation,
                    style = AppButtonStyle.Secondary,
                    leadingIcon = { ChangeLocationIcon() },
                )
            },
        )

        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        TransientToastEffect(
            message = uiState.message,
            errorMessage = uiState.errorMessage,
            onConsumed = onClearMessages,
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val splitLayout = maxWidth >= 960.dp

            if (splitLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xl),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(0.95f),
                        verticalArrangement = Arrangement.spacedBy(spacing.xl),
                    ) {
                        TeamSelectionSection(
                            teams = uiState.teams,
                            selectedTeam = selectedTeam,
                            progress = uiState.selectedTeamProgress,
                            isLoading = uiState.isLoading,
                            onSelectTeam = onSelectTeam,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1.35f),
                        verticalArrangement = Arrangement.spacedBy(spacing.xl),
                    ) {
                        TasksSection(
                            tasks = uiState.tasks,
                            selectedTeam = selectedTeam,
                            answerDrafts = uiState.answerDrafts,
                            latestSubmissionByTaskId = uiState.latestSubmissionByTaskId,
                            isLoading = uiState.isLoading,
                            onAnswerChange = onAnswerChange,
                            onSubmitAnswer = onSubmitAnswer,
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.xl),
                ) {
                    TeamSelectionSection(
                        teams = uiState.teams,
                        selectedTeam = selectedTeam,
                        progress = uiState.selectedTeamProgress,
                        isLoading = uiState.isLoading,
                        onSelectTeam = onSelectTeam,
                    )
                    TasksSection(
                        tasks = uiState.tasks,
                        selectedTeam = selectedTeam,
                        answerDrafts = uiState.answerDrafts,
                        latestSubmissionByTaskId = uiState.latestSubmissionByTaskId,
                        isLoading = uiState.isLoading,
                        onAnswerChange = onAnswerChange,
                        onSubmitAnswer = onSubmitAnswer,
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamSelectionSection(
    teams: List<Team>,
    selectedTeam: Team?,
    progress: TeamProgressSummary?,
    isLoading: Boolean,
    onSelectTeam: (Long) -> Unit,
) {
    val teamOptions = remember(teams) {
        teams.mapNotNull { team ->
            val teamId = team.id ?: return@mapNotNull null
            AppSelectOption(
                value = teamId.toString(),
                label = team.name ?: "Csapat #$teamId",
            )
        }
    }
    FormSection(
        title = "Csapat",
        description = "Válassz csapatot, majd rögzítsd a helyszín feladataira adott válaszokat.",
    ) {
        TeamComboBox(
            selectedLabel = selectedTeam?.name ?: "Válassz csapatot",
            options = teamOptions,
            enabled = !isLoading && teamOptions.isNotEmpty(),
            onSelectTeam = onSelectTeam,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = buildString {
                append(selectedTeam?.name ?: "Nincs kiválasztott csapat")
                append(" • ID ")
                append(selectedTeam?.id ?: "-")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = AppThemeTokens.colors.textSecondary,
        )
        TeamInfoDisclosure(team = selectedTeam, progress = progress)
    }
}

@Composable
private fun TasksSection(
    tasks: List<Task>,
    selectedTeam: Team?,
    answerDrafts: Map<Long, String>,
    latestSubmissionByTaskId: Map<Long, TaskSubmissionResult>,
    isLoading: Boolean,
    onAnswerChange: (Long, String) -> Unit,
    onSubmitAnswer: (Long) -> Unit,
) {
    FormSection(
        title = "Elérhető feladatok",
        description = "Csak az aktuális helyszínhez tartozó feladatok jelennek meg itt.",
    ) {
        if (tasks.isEmpty() && !isLoading) {
            EmptyState(
                title = "Ehhez a helyszínhez nincs feladat",
                description = "Vegyél fel legalább egy feladatot ehhez az állomáshoz a setup képernyőn.",
            )
        } else {
            tasks.forEachIndexed { index, task ->
                if (index > 0) {
                    HorizontalDivider()
                }
                LocationTaskRow(
                    task = task,
                    selectedTeam = selectedTeam,
                    answer = answerDrafts[task.id].orEmpty(),
                    latestSubmission = task.id?.let(latestSubmissionByTaskId::get),
                    isLoading = isLoading,
                    onAnswerChange = { value -> task.id?.let { onAnswerChange(it, value) } },
                    onSubmit = { task.id?.let(onSubmitAnswer) },
                )
            }
        }
    }
}

@Composable
private fun LocationTaskRow(
    task: Task,
    selectedTeam: Team?,
    answer: String,
    latestSubmission: TaskSubmissionResult?,
    isLoading: Boolean,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val colors = AppThemeTokens.colors
    val spacing = AppThemeTokens.spacing
    val isMiniBoss = task.isMiniBoss == true
    var isSolutionVisible by rememberSaveable(task.id) { mutableStateOf(false) }

    SectionCard(toned = isMiniBoss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                text = task.text.ifBlank { "Névtelen feladat" },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (isMiniBoss) "Miniboss feladat" else "Normál feladat",
                style = MaterialTheme.typography.bodySmall,
                color = if (isMiniBoss) colors.accent else colors.textSecondary,
            )
            task.solution.takeIf { it.isNotBlank() }?.let { solution ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSolutionVisible = !isSolutionVisible },
                ) {
                    Text(
                        text = if (isSolutionVisible) "Megoldás • kattintással elrejthető" else "Megoldás • kattintással felfedhető",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = solution,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        modifier = if (isSolutionVisible) {
                            Modifier
                        } else {
                            Modifier.blur(12.dp)
                        },
                    )
                }
            }
            AppTextField(
                value = answer,
                onValueChange = onAnswerChange,
                label = "Csapat válasza",
                enabled = !isLoading && selectedTeam?.id != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = if (selectedTeam == null) "Előbb válassz csapatot." else null,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppButton(
                    text = "Beküldés",
                    onClick = onSubmit,
                    enabled = !isLoading && selectedTeam?.id != null && answer.isNotBlank(),
                    leadingIcon = { SaveIcon() },
                )
                latestSubmission?.let { submission ->
                    SubmissionResultLabel(submission = submission)
                }
            }
        }
    }
}

@Composable
private fun SubmissionResultLabel(submission: TaskSubmissionResult) {
    val colors = AppThemeTokens.colors

    Row(
        horizontalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (submission.isSuccess) {
            CorrectIcon(selected = false, iconSize = 18.dp)
        } else {
            IncorrectIcon(selected = false, iconSize = 18.dp)
        }
        Text(
            text = if (submission.isSuccess) "Helyes rögzítés" else "Sikertelen rögzítés",
            style = MaterialTheme.typography.bodyMedium,
            color = if (submission.isSuccess) colors.accent else colors.danger,
        )
    }
}

@Composable
private fun TeamComboBox(
    selectedLabel: String,
    options: List<AppSelectOption>,
    enabled: Boolean,
    onSelectTeam: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Row(modifier = modifier) {
        AppButton(
            onClick = { if (enabled) expanded = true },
            style = AppButtonStyle.Secondary,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = selectedLabel,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Csapat választása",
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    enabled = option.enabled,
                    onClick = {
                        expanded = false
                        option.value.toLongOrNull()?.let(onSelectTeam)
                    },
                )
            }
        }
    }
}

private fun currentLocationButtonLabel(activeLocationName: String?): String =
    "Helyszín: ${activeLocationName ?: "nincs kiválasztva"}"

@Preview(showBackground = true)
@Composable
private fun LocationTasksScreenPreview() {
    AppTheme {
        LocationTasksScreen(
            activeGameName = "Demo Game - Logic Castle",
            activeLocation = Location(id = 1, name = "Library", gameId = 1),
            uiState = LocationTasksUiState(
                tasks = listOf(
                    Task(id = 1, text = "A kódfejtő tekercs megfejtése", solution = "42", isMiniBoss = false),
                    Task(id = 2, text = "A könyvtár miniboss feladata", solution = "LOGIKA", isMiniBoss = true),
                ),
                teams = listOf(
                    Team(id = 1, name = "Blue Cipher"),
                    Team(id = 2, name = "Red Dragon"),
                ),
                selectedTeamId = 1,
                selectedTeamProgress = TeamProgressSummary(
                    solvedTasks = 5,
                    totalTasks = 12,
                    defeatedMiniBosses = 1,
                    totalMiniBosses = 3,
                    points = 7,
                    money = 4,
                    spent = 3,
                ),
                answerDrafts = mapOf(1L to "42"),
                latestSubmissionByTaskId = mapOf(
                    2L to TaskSubmissionResult(
                        isSuccess = false,
                        submittedAnswer = "ROSSZ",
                    ),
                ),
            ),
        )
    }
}
