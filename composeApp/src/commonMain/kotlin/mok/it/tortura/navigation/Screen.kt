package mok.it.tortura.navigation

sealed class Screen(val route: String) {
    object CreateTeams : Screen("create_teams")
    object CreateTasks : Screen("create_tasks")
    object SetUpMenu : Screen("setup_menu")
    object MainMenu : Screen("main_menu")
}
