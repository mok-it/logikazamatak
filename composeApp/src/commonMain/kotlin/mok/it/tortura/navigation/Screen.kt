package mok.it.tortura.navigation

import kotlinx.serialization.Serializable

object Screen {
    @Serializable
    object GameSelection

    @Serializable
    object HealerTeamSelection

    @Serializable
    data class HealerTasks(
        val teamId: Long,
    )

    @Serializable
    object CreateTeams

    @Serializable
    object CreateTasks

    @Serializable
    object SetUpMenu

    @Serializable
    object MainMenu
}
