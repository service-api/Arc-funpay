package arc.funpay.event.impl

import arc.funpay.domain.category.Category
import arc.funpay.event.api.Event

sealed interface LotEvent : Event {
    data class LotsRaised(
        val category: Category,
        val message: String? = null
    ) : LotEvent
}