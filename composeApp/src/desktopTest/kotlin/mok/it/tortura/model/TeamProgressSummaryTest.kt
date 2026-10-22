package mok.it.tortura.model

import kotlin.test.Test
import kotlin.test.assertEquals

class TeamProgressSummaryTest {
    @Test
    fun bonusesIncreasePointsWithoutInflatingSolvedTasksOrMinibosses() {
        val summary = TeamProgressSummaryCalculator.calculate(
            team = Team(additionalScoreAwarded = -1),
            allGameTasks = listOf(Task(id = 10), Task(id = 11, isMiniBoss = true), Task(id = 12)),
            allItems = listOf(Item(id = 1, price = 2)),
            taskEvents = listOf(
                TaskEvent(taskId = 10, isSuccess = true),
                TaskEvent(taskId = 10, isSuccess = true),
                TaskEvent(taskId = 11, isSuccess = true, bonusSourceShopId = 1),
                TaskEvent(taskId = 12, isSuccess = false),
            ),
            purchases = listOf(ShopEntry(itemId = 1)),
        )
        assertEquals(
            TeamProgressSummary(
                solvedTasks = 1,
                totalTasks = 3,
                defeatedMiniBosses = 0,
                totalMiniBosses = 1,
                points = 2,
                spent = 2,
                money = 0,
            ),
            summary,
        )
    }

    @Test
    fun repeatedMinibossSuccessCountsOnceForProgressButEachSuccessAwardsAPoint() {
        val summary = TeamProgressSummaryCalculator.calculate(
            team = Team(),
            allGameTasks = listOf(Task(id = 11, isMiniBoss = true)),
            allItems = emptyList(),
            taskEvents = listOf(TaskEvent(taskId = 11, isSuccess = true), TaskEvent(taskId = 11, isSuccess = true)),
            purchases = emptyList(),
        )
        assertEquals(1, summary.solvedTasks)
        assertEquals(1, summary.defeatedMiniBosses)
        assertEquals(2, summary.points)
    }

    @Test
    fun signedManualCorrectionsAreNotClampedWhenBudgetIsNegative() {
        val summary = TeamProgressSummaryCalculator.calculate(
            team = Team(additionalScoreAwarded = -5),
            allGameTasks = emptyList(),
            allItems = listOf(Item(id = 1, price = 2)),
            taskEvents = emptyList(),
            purchases = listOf(ShopEntry(itemId = 1)),
        )
        assertEquals(-5, summary.points)
        assertEquals(-7, summary.money)
        assertEquals(2, summary.spent)
    }
}
