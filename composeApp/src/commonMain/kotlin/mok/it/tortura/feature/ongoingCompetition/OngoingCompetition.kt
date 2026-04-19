package mok.it.tortura.feature.ongoingCompetition

import NavigateBackIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import mok.it.tortura.model.Competition
import mok.it.tortura.ui.components.AnswerBlock
import mok.it.tortura.ui.components.FinishedContent
import mok.it.tortura.ui.components.HelpDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OngoingCompetition(
    onBack: () -> Unit,
    onEvaluate: () -> Unit,
) {

    val viewModel = viewModel { OngoingCompetitionViewModel() }

    val competitions by remember { viewModel.competitions }

    val save = rememberFileSaverLauncher { file ->
        if (file != null) {
            viewModel.onEvent(
                OnGoingCompetitionEvent.SaveToFile(file)
            )
        }
    }

    fun teamsInPreviousCompetitions(actCompetition: Competition): Int {
        var counter = 0
        for (competition in competitions) {
            if (competition == actCompetition) return counter
            counter += competition.teamAssignment.teams.size
        }
        return counter
    }

    val tabIndex by remember { viewModel.tabIndex }

    val helpDialogShown = remember { mutableStateOf(false) }
    if (helpDialogShown.value) {
        HelpDialog(
            onDismiss = { helpDialogShown.value = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verseny") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        NavigateBackIcon()
                    }
                },
                actions = {
                    Button(
                        onClick = { save.launch(suggestedName = "tortura", extension = "ttr") }
                    ) {
                        Text("Export")
                    }
                    Button(
                        onClick = onEvaluate
                    ) {
                        Text("Kiértékelés")
                    }

//                    CustomizableSearchBar(
//                        query = searchText,
//                        onQueryChange = { viewModel.onEvent(OnGoingCompetitionEvent.SearchTextChange( it ) ) },
//                        onSearch = {  },
//                        searchResults = viewModel.getAllStudentsList(),
//                        onResultClick = { viewModel.onEvent(OnGoingCompetitionEvent.SearchForStudent(it)) },
//                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(innerPadding),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            competitions.forEach { competition ->
                val competitionTeams = competition.answers
                competitionTeams.forEachIndexed { index, competitionTeam ->
                    if (tabIndex == teamsInPreviousCompetitions(competition) + index) {
                        if (competitionTeam.answer.finished) {
                            FinishedContent(
                                textColor = competition.colors.textColor,
                                backgroundColor = competition.colors.backgroundColor,
                                points = competitionTeam.answer.points().toString(),
                                answer = competitionTeam.answer,
                                onNavigateBack = {
                                    viewModel.onEvent(
                                        OnGoingCompetitionEvent.NavigateBackwards(
                                            competition,
                                            competitionTeam
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxSize().weight(1f)
                            )


                        } else {

                            val showConfirmDialog = remember { mutableStateOf(false) }

                            AnswerBlock(
                                teamName = competitionTeam.team.name ?: "${competition.teamAssignment.baseTeamId + index}",
                                answer = competitionTeam.answer,
                            )

                        }
                    }
                }
            }

            ScrollableTabRow(
                selectedTabIndex = tabIndex,
                containerColor = Color.Transparent,
                modifier = Modifier
            ) {
                for (competition in competitions) {
                    val teams = competition.teamAssignment.teams
                    teams.forEachIndexed { index, team ->
                        Tab(
                            selected = tabIndex == teamsInPreviousCompetitions(competition) + index,
                            onClick = {
                                viewModel.onEvent(
                                    OnGoingCompetitionEvent.ChangeTabIndex(teamsInPreviousCompetitions(competition) + index)
                                )
                            },
                            text = {
                                Text(
                                    text = team.name ?: "${competition.teamAssignment.baseTeamId + index}",
                                    color = competition.colors.textColor,
                                )
                            },
                            modifier = Modifier.background(competition.colors.backgroundColor),
                        )
                    }
                }
            }
        }
    }
}

