package mok.it.tortura.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mok.it.tortura.feature.MainMenu
import mok.it.tortura.feature.SetUpMenu

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.MainMenu,
    ) {
        composable<Screen.SetUpMenu> {
            SetUpMenu(
                onCompetitionCreation = { },
                onTeamCreation = { },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Screen.MainMenu> {
            MainMenu(
                onSetUp = { navController.navigate(Screen.SetUpMenu) },
                onCompetition = { },
            )
        }
    }
}
