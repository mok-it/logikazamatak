package mok.it.tortura.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mok.it.tortura.feature.AuthViewModel
import mok.it.tortura.feature.MainMenu
import mok.it.tortura.feature.SetUpMenu
import mok.it.tortura.feature.SetupViewModel

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    val authViewModel = viewModel { AuthViewModel() }
    val authUiState = authViewModel.uiState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Screen.MainMenu,
    ) {
        composable<Screen.SetUpMenu> {
            val setupViewModel = viewModel { SetupViewModel() }
            val setupUiState = setupViewModel.uiState.collectAsStateWithLifecycle()

            SetUpMenu(
                uiState = setupUiState.value,
                onLoad = setupViewModel::loadSetupData,
                onGameNameChange = setupViewModel::onGameNameChange,
                onBaseTeamCounterChange = setupViewModel::onBaseTeamCounterChange,
                onCompetitionCreation = setupViewModel::createGame,
                onTeamCreation = setupViewModel::createTeamAssignment,
                onClearMessages = setupViewModel::clearMessages,
                onBack = { navController.popBackStack() },
            )
        }
        composable<Screen.MainMenu> {
            MainMenu(
                authUiState = authUiState.value,
                onSignInWithGoogle = authViewModel::signInWithGoogle,
                onSignOut = authViewModel::signOut,
                onClearAuthError = authViewModel::clearError,
                onSetUp = { navController.navigate(Screen.SetUpMenu) },
                onCompetition = { },
            )
        }
    }
}
