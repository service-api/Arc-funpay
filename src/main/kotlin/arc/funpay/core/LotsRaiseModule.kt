package arc.funpay.core

import arc.funpay.core.api.FunpayModule
import arc.funpay.di.api.DependencyContainer
import arc.funpay.domain.category.Category
import arc.funpay.event.api.EventBus
import arc.funpay.event.impl.lot.LotEvent
import arc.funpay.ext.StringExtensions
import arc.funpay.system.FunPayAPI
import java.time.Instant

class LotsRaiseModule(
    override val container: DependencyContainer
) : FunpayModule() {
    val api: FunPayAPI by lazy { inject() }
    val eventBus: EventBus by lazy { inject() }
    val string: StringExtensions by lazy { inject() }

    private val categories = mutableListOf<Category>()

    override suspend fun onStart() {
        super.onStart()
        categories.clear()
    }

    override suspend fun onTick() {
        val currentTime = Instant.now().toEpochMilli()
        categories.filter { it.nextCheck <= currentTime }
            .forEach { handleRaise(it) }
    }

    fun addCategory(category: Category): Boolean {
        return if (categories.none { it.nodeId == category.nodeId }) {
            categories.add(category)
            true
        } else false
    }

    fun removeCategory(nodeId: String): Boolean {
        return categories.removeIf { it.nodeId == nodeId }
    }

    suspend fun handleRaise(category: Category) {
        try {
            val preRaiseEvent = LotEvent.PreLotsRaise(category)
            eventBus.publish(preRaiseEvent)

            if (preRaiseEvent.isCancelled) {
                category.nextCheck = calculateNextCheck(null)
                return
            }

            val response = api.raiseLots(category.gameId, category.nodeId)

            category.nextCheck = if (response.isSuccess) {
                eventBus.publish(LotEvent.LotsRaised(category, response.message))
                calculateNextCheck(response.message)
            } else {
                calculateNextCheck(null)
            }
        } catch (e: Exception) {
            category.nextCheck = Instant.now().toEpochMilli() + 60 * 1000L
        }
    }

    fun calculateNextCheck(message: String?): Long {
        if (message.isNullOrBlank()) return Instant.now().toEpochMilli() + 60 * 1000L

        val currentTime = Instant.now().toEpochMilli()
        with(string) {
            return currentTime + when {
                message.contains("час") -> 60 * 60 * 1000L
                message.contains("минут") -> {
                    val minutes = message.extractNumber()
                    minutes * 60 * 1000L
                }
                message.contains("секунд") -> {
                    val seconds = message.extractNumber()
                    seconds * 1000L
                }
                else -> 60 * 1000L
            }
        }
    }
}
