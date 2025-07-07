package arc.funpay.event.impl

import arc.funpay.core.LotsRaiseModule
import arc.funpay.event.api.Event

sealed interface LotEvent : Event {
    data class LotsRaised(
        val category: LotsRaiseModule.CategoryRaiseInfo,
        val message: String? = null
    ) : LotEvent
}