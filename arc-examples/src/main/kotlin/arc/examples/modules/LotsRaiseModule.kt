package arc.examples.modules

import arc.funpay.event.LotsRaiseEvent
import arc.funpay.event.pre.PreLotsRaiseEvent
import arc.funpay.ext.extractNumber
import arc.funpay.ext.now
import arc.funpay.ext.parseTiming
import arc.funpay.models.funpay.Category
import arc.funpay.modules.api.Module

/**
 * Module responsible for raising lots in specified categories.
 *
 * @property categories The list of categories to handle.
 */
class LotsRaiseModule(
    val categories: List<Category>
) : Module() {

    /**
     * Called periodically to handle raising lots for each category.
     */
    override suspend fun onTick() {
        categories.forEach { category ->
            handleRaise(category)
        }
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
            category.nextCheck = System.currentTimeMillis() + 60000
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