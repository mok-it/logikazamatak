package mok.it.tortura.model

object TeamScoreCalculator {

    fun calculateFinalPoints(
        taskEvents: List<TaskEvent>,
        additionalScoreAwarded: Int = 0,
    ): Int = taskEvents.count { it.isSuccess == true } + additionalScoreAwarded

    fun calculateSpentPoints(
        purchases: List<ShopEntry>,
        items: List<Item>,
    ): Int {
        val itemsById = items.associateBy { it.id }
        return purchases.sumOf { purchase ->
            val itemId = purchase.itemId ?: return@sumOf 0
            itemsById[itemId]?.price ?: 0
        }
    }

    fun calculateCurrentMoney(
        taskEvents: List<TaskEvent>,
        additionalScoreAwarded: Int = 0,
        purchases: List<ShopEntry>,
        items: List<Item>,
    ): Int = calculateFinalPoints(
        taskEvents = taskEvents,
        additionalScoreAwarded = additionalScoreAwarded,
    ) - calculateSpentPoints(
        purchases = purchases,
        items = items,
    )
}
