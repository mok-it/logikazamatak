package mok.it.tortura.navigation

import kotlinx.serialization.Serializable

object Screen {
    @Serializable
    object GameSelection

    @Serializable
    data class LocationSelection(
        val gameId: Long,
    )

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

    @Serializable
    data class Ledger(val gameId: Long, val teamId: Long? = null)

    @Serializable
    object Shop

    @Serializable
    object LocationTasks
}
