package mok.it.tortura.model

data class ProblemSet(
    val locations: Set<Location>,
    val healers: Set<Healer>,
    val mainBoss: Task,
    val taskValue: Int,
    val miniBossValue: Int,
    val maxBossValue: Int,
    val id: Int = -1,
)
