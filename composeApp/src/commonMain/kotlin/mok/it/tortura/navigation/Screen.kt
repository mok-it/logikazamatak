package mok.it.tortura.navigation

import kotlinx.serialization.Serializable

object Screen {
    @Serializable
    object GameSelection

    @Serializable
    object CreateTeams

    @Serializable
    object CreateTasks

    @Serializable
    object SetUpMenu

    @Serializable
    object MainMenu
}
