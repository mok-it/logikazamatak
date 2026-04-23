package mok.it.tortura.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mok.it.tortura.feature.AuthUiState
import mok.it.tortura.feature.GameSelectionScreen
import mok.it.tortura.feature.GameSelectionViewModel
import mok.it.tortura.feature.MainMenu
import mok.it.tortura.feature.SetUpMenu
import mok.it.tortura.feature.SetupViewModel
import mok.it.tortura.model.Game

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    var activeGame by remember { mutableStateOf<Game?>(null) }
    val authUiState = AuthUiState(
        isInitializing = false,
        isAuthenticated = true,
        email = "Auth kikapcsolva",
    )

    NavHost(
        navController = navController,
        startDestination = if (activeGame == null) Screen.GameSelection else Screen.MainMenu,
    ) {
        composable<Screen.GameSelection> {
            val gameSelectionViewModel = viewModel { GameSelectionViewModel() }
            val gameSelectionUiState = gameSelectionViewModel.uiState.collectAsStateWithLifecycle()

            GameSelectionScreen(
                uiState = gameSelectionUiState.value,
                onLoad = gameSelectionViewModel::loadGames,
                onGameNameChange = gameSelectionViewModel::onGameNameChange,
                onCreateGame = gameSelectionViewModel::createGame,
                onSelectGame = gameSelectionViewModel::selectGame,
                onGameSelected = { game ->
                    gameSelectionViewModel.clearSelection()
                    activeGame = game
                    navController.navigate(Screen.MainMenu) {
                        launchSingleTop = true
                    }
                },
                onClearMessages = gameSelectionViewModel::clearMessages,
            )
        }

        composable<Screen.SetUpMenu> {
            val selectedGame = activeGame
            val selectedGameId = selectedGame?.id

            if (selectedGame == null || selectedGameId == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.GameSelection) {
                        launchSingleTop = true
                    }
                }
                return@composable
            }

            val setupViewModel = viewModel(key = "setup-$selectedGameId") {
                SetupViewModel(activeGameId = selectedGameId)
            }
            val setupUiState = setupViewModel.uiState.collectAsStateWithLifecycle()

            SetUpMenu(
                activeGameName = selectedGame.name ?: "#$selectedGameId",
                uiState = setupUiState.value,
                onLoad = setupViewModel::loadSetupData,
                onBaseTeamCounterChange = setupViewModel::onBaseTeamCounterChange,
                onTeamCreation = setupViewModel::createTeamAssignment,
                onClearMessages = setupViewModel::clearMessages,
                onBack = { navController.popBackStack() },
            )
        }
        composable<Screen.MainMenu> {
            val selectedGame = activeGame
            if (selectedGame?.id == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.GameSelection) {
                        launchSingleTop = true
                    }
                }
                return@composable
            }

            MainMenu(
                activeGame = selectedGame,
                authUiState = authUiState,
                onChangeGame = {
                    activeGame = null
                    navController.navigate(Screen.GameSelection) {
                        launchSingleTop = true
                    }
                },
                onSetUp = { navController.navigate(Screen.SetUpMenu) },
                onCompetition = { },
            )
        }
    }
}
