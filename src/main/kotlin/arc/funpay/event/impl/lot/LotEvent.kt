package arc.funpay.event.impl.lot

import arc.funpay.domain.category.Category
import arc.funpay.event.api.CancellableEvent
import arc.funpay.event.api.Event

sealed interface LotEvent : Event {
    data class PreLotsRaise(
        val category: Category
    ) : LotEvent, CancellableEvent {
        override var isCancelled: Boolean = false
    }

    data class LotsRaised(
        val category: Category,
        val message: String? = null
    ) : LotEvent
}
