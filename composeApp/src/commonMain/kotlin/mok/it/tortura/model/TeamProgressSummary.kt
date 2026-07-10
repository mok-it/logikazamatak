package mok.it.tortura.model

data class TeamProgressSummary(
    val solvedTasks: Int = 0,
    val totalTasks: Int = 0,
    val defeatedMiniBosses: Int = 0,
    val totalMiniBosses: Int = 0,
    val points: Int = 0,
    val money: Int = 0,
    val spent: Int = 0,
)

object TeamProgressSummaryCalculator {
    fun calculate(
        team: Team,
        allGameTasks: List<Task>,
        allItems: List<Item>,
        taskEvents: List<TaskEvent>,
        purchases: List<ShopEntry>,
    ): TeamProgressSummary {
        val baseSuccessEvents = taskEvents.filter { it.isSuccess == true && it.bonusSourceShopId == null }
        val solvedTaskIds = baseSuccessEvents.map { it.taskId }.toSet()
        val miniBossTaskIds = allGameTasks.filter { it.isMiniBoss == true }.mapNotNull { it.id }.toSet()

        return TeamProgressSummary(
            solvedTasks = solvedTaskIds.size,
            totalTasks = allGameTasks.size,
            defeatedMiniBosses = solvedTaskIds.count { it in miniBossTaskIds },
            totalMiniBosses = miniBossTaskIds.size,
            points = TeamScoreCalculator.calculateFinalPoints(
                taskEvents = taskEvents,
                additionalScoreAwarded = team.additionalScoreAwarded,
            ),
            money = TeamScoreCalculator.calculateCurrentMoney(
                taskEvents = taskEvents,
                additionalScoreAwarded = team.additionalScoreAwarded,
                purchases = purchases,
                items = allItems,
            ),
            spent = TeamScoreCalculator.calculateSpentPoints(
                purchases = purchases,
                items = allItems,
            ),
        )
    }
}
