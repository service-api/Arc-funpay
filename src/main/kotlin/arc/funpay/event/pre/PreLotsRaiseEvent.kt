package arc.funpay.event.pre

import arc.funpay.event.api.Cancelable
import arc.funpay.event.api.FunpayEvent
import arc.funpay.models.funpay.Category

/**
 * Event triggered before raising lots in a specific category.
 *
 * @property category The category of the lots to be raised.
 */
class PreLotsRaiseEvent(
    val category: Category
) : FunpayEvent, Cancelable {
    /**
     * Indicates whether the event is cancelled.
     */
    override var isCancelled: Boolean = false
}