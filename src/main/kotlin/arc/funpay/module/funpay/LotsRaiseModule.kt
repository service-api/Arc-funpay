package arc.funpay.module.funpay

import arc.funpay.event.LotsRaiseEvent
import arc.funpay.event.pre.PreLotsRaiseEvent
import arc.funpay.ext.extractNumber
import arc.funpay.ext.now
import arc.funpay.ext.parseTiming
import arc.funpay.model.funpay.Category
import arc.funpay.module.api.Module

/**
 * Module responsible for raising lots in automatically detected categories.
 *
 * @property api FunpayAPI instance used to fetch and raise lots.
 */
class LotsRaiseModule : Module() {
    val categories = mutableListOf<Category>()

    override suspend fun onStart() {
        categories.clear()
    }

    /**
     * Called periodically to handle raising lots for each category.
     */
    override suspend fun onTick() {
        for (category in categories) {
            if (category.nextCheck <= now()) {
                handleRaise(category)
            }
        }
    }

    /**
     * Manually adds a category to raise.
     */
    fun addCategory(category: Category): Boolean {
        if (categories.contains(category)) {
            return false
        }
        categories += category
        return true
    }

    /**
     * Manually removes a category by nodeId.
     */
    fun removeCategoryByNodeId(nodeId: String): Boolean {
        return categories.removeIf { it.nodeId == nodeId }
    }

    /**
     * Handles the raising of lots for a specific category.
     *
     * @param category The category to handle.
     */
    suspend fun handleRaise(category: Category) {
        try {
            val event = PreLotsRaiseEvent(category)
            eventBus.post(event)

            if (event.isCancelled) {
                category.nextCheck = calculateNextCheck(null)
                return
            }

            val response = api.raiseLots(category.gameId, category.nodeId)
            if (!response.success) {
                println("Error for category ${category.name}: ${response.msg}")
                category.nextCheck = calculateNextCheck(null)
                return
            }

            category.nextCheck = response.msg.parseTiming()
            if (response.msg.contains("подняты"))
                eventBus.post(LotsRaiseEvent(category, response.msg))
        } catch (e: Exception) {
            println("Error during raise for category ${category.name}: ${e.message}")
            category.nextCheck = now() + 60000
        }
    }

    /**
     * Calculates the next check time based on the response message.
     *
     * @param msg The response message.
     * @return The next check time in milliseconds.
     */
    fun calculateNextCheck(msg: String?): Long {
        if (msg.isNullOrBlank()) return now() + 60000
        return when {
            "час" in msg -> now() + 3600000
            "минут" in msg -> now() + msg.extractNumber() * 60000L
            "секунд" in msg -> now() + msg.extractNumber() * 1000L
            else -> now() + 60000L
        }
    }
}