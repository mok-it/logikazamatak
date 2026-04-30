package mok.it.tortura.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import mok.it.tortura.data.supabase.mapper.toModel
import mok.it.tortura.data.supabase.repository.TorturaSupabaseRepositories
import mok.it.tortura.feature.AuthUiState
import mok.it.tortura.feature.GameSelectionScreen
import mok.it.tortura.feature.GameSelectionViewModel
import mok.it.tortura.feature.HealerTasksScreen
import mok.it.tortura.feature.HealerTasksViewModel
import mok.it.tortura.feature.HealerTeamSelectionScreen
import mok.it.tortura.feature.HealerTeamSelectionViewModel
import mok.it.tortura.feature.LocationSelectionScreen
import mok.it.tortura.feature.LocationSelectionViewModel
import mok.it.tortura.feature.MainMenu
import mok.it.tortura.feature.SetUpMenu
import mok.it.tortura.feature.SetupViewModel
import mok.it.tortura.model.Game
import mok.it.tortura.model.Location
import mok.it.tortura.ui.components.LocationPickerDialog

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    var activeGame by remember { mutableStateOf<Game?>(null) }
    var activeLocation by remember { mutableStateOf<Location?>(null) }
    var isLocationPickerOpen by remember { mutableStateOf(false) }
    var switchableLocations by remember { mutableStateOf<List<Location>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val repositories = remember { TorturaSupabaseRepositories() }
    val authUiState = AuthUiState(
        isInitializing = false,
        isAuthenticated = true,
        email = "Auth kikapcsolva",
    )

    fun resetActiveContext() {
        activeGame = null
        activeLocation = null
    }

    fun openLocationPickerForActiveGame() {
        val gameId = activeGame?.id ?: return
        coroutineScope.launch {
            switchableLocations = repositories.locations.getByGameId(gameId).map { it.toModel() }
            if (switchableLocations.isNotEmpty()) {
                isLocationPickerOpen = true
            }
        }
    }

    if (isLocationPickerOpen) {
        LocationPickerDialog(
            locations = switchableLocations,
            selectedLocationId = activeLocation?.id,
            onSelectLocation = { location ->
                activeLocation = location
                isLocationPickerOpen = false
            },
            onDismiss = { isLocationPickerOpen = false },
        )
    }

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
                onAddLocation = gameSelectionViewModel::addLocation,
                onLocationNameChange = gameSelectionViewModel::updateLocationName,
                onRemoveLocation = gameSelectionViewModel::removeLocation,
                onAddTask = gameSelectionViewModel::addTask,
                onTaskTextChange = gameSelectionViewModel::updateTaskText,
                onTaskSolutionChange = gameSelectionViewModel::updateTaskSolution,
                onTaskMiniBossChange = gameSelectionViewModel::updateTaskMiniBoss,
                onRemoveTask = gameSelectionViewModel::removeTask,
                onAddHealingTask = gameSelectionViewModel::addHealingTask,
                onHealingTaskTextChange = gameSelectionViewModel::updateHealingTaskText,
                onHealingTaskSolutionChange = gameSelectionViewModel::updateHealingTaskSolution,
                onRemoveHealingTask = gameSelectionViewModel::removeHealingTask,
                onAddShopItem = gameSelectionViewModel::addShopItem,
                onShopItemNameChange = gameSelectionViewModel::updateShopItemName,
                onShopItemPriceChange = gameSelectionViewModel::updateShopItemPrice,
                onShopItemMaxPerTeamChange = gameSelectionViewModel::updateShopItemMaxPerTeam,
                onShopItemEffectIdChange = gameSelectionViewModel::updateShopItemEffectId,
                onRemoveShopItem = gameSelectionViewModel::removeShopItem,
                onSelectGame = gameSelectionViewModel::selectGame,
                onGameSelected = { game ->
                    gameSelectionViewModel.clearSelection()
                    activeGame = game
                    activeLocation = null
                    navController.navigate(Screen.MainMenu) {
                        launchSingleTop = true
                    }
                },
                onLocationSelectionRequired = { gameId ->
                    gameSelectionViewModel.clearLocationSelectionRequest()
                    navController.navigate(Screen.LocationSelection(gameId = gameId)) {
                        launchSingleTop = true
                    }
                },
                onClearMessages = gameSelectionViewModel::clearMessages,
            )
        }
        composable<Screen.LocationSelection> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.LocationSelection>()
            val locationSelectionViewModel =
                viewModel(key = "location-selection-${screen.gameId}") {
                    LocationSelectionViewModel(gameId = screen.gameId)
                }
            val locationSelectionUiState = locationSelectionViewModel.uiState.collectAsStateWithLifecycle()

            LocationSelectionScreen(
                uiState = locationSelectionUiState.value,
                onLoad = locationSelectionViewModel::load,
                onSelectLocation = { locationId ->
                    val selectedGame =
                        locationSelectionUiState.value.game ?: return@LocationSelectionScreen
                    val selectedLocation = locationSelectionUiState.value.locations
                        .firstOrNull { it.id == locationId }
                        ?: return@LocationSelectionScreen
                    activeGame = selectedGame
                    activeLocation = selectedLocation
                    navController.navigate(Screen.MainMenu) {
                        popUpTo(Screen.GameSelection) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
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
                activeLocationName = activeLocation?.name,
                uiState = setupUiState.value,
                onLoad = setupViewModel::loadSetupData,
                onBaseTeamCounterChange = setupViewModel::onBaseTeamCounterChange,
                onTeamCreation = setupViewModel::createTeamAssignment,
                onClearMessages = setupViewModel::clearMessages,
                onBack = { navController.popBackStack() },
                onChangeLocation = ::openLocationPickerForActiveGame,
            )
        }
        composable<Screen.HealerTeamSelection> {
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

            val healerTeamSelectionViewModel =
                viewModel(key = "healer-team-selection-$selectedGameId") {
                    HealerTeamSelectionViewModel(activeGameId = selectedGameId)
                }
            val healerTeamSelectionUiState =
                healerTeamSelectionViewModel.uiState.collectAsStateWithLifecycle()

            HealerTeamSelectionScreen(
                activeGameName = selectedGame.name ?: "#$selectedGameId",
                activeLocationName = activeLocation?.name,
                uiState = healerTeamSelectionUiState.value,
                onLoad = healerTeamSelectionViewModel::loadTeams,
                onSelectTeam = { team ->
                    team.id?.let { navController.navigate(Screen.HealerTasks(teamId = it)) }
                },
                onClearMessages = healerTeamSelectionViewModel::clearMessages,
                onBack = { navController.popBackStack() },
                onChangeLocation = ::openLocationPickerForActiveGame,
            )
        }
        composable<Screen.HealerTasks> { backStackEntry ->
            val selectedGame = activeGame
            val selectedGameId = selectedGame?.id
            val screen = backStackEntry.toRoute<Screen.HealerTasks>()

            if (selectedGame == null || selectedGameId == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.GameSelection) {
                        launchSingleTop = true
                    }
                }
                return@composable
            }

            val healerTasksViewModel = viewModel(key = "healer-tasks-${screen.teamId}") {
                HealerTasksViewModel(teamId = screen.teamId)
            }
            val healerTasksUiState = healerTasksViewModel.uiState.collectAsStateWithLifecycle()

            HealerTasksScreen(
                activeGameName = selectedGame.name ?: "#$selectedGameId",
                activeLocationName = activeLocation?.name,
                uiState = healerTasksUiState.value,
                onLoad = healerTasksViewModel::load,
                onSelectHealingTask = healerTasksViewModel::selectHealingTask,
                onCompleteHealing = healerTasksViewModel::completeHealing,
                onClearMessages = healerTasksViewModel::clearMessages,
                onBack = { navController.popBackStack() },
                onChangeLocation = ::openLocationPickerForActiveGame,
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
                activeLocation = activeLocation,
                authUiState = authUiState,
                onChangeGame = {
                    resetActiveContext()
                    navController.navigate(Screen.GameSelection) {
                        launchSingleTop = true
                    }
                },
                onSetUp = { navController.navigate(Screen.SetUpMenu) },
                onCompetition = { navController.navigate(Screen.HealerTeamSelection) },
                onChangeLocation = ::openLocationPickerForActiveGame,
            )
        }
    }
}
