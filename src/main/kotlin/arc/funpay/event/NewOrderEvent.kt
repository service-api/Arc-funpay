package arc.funpay.event

import arc.funpay.event.api.FunpayEvent
import arc.funpay.model.funpay.Order

/**
 * Event triggered when there is a change in the order count.
 *
 * @property order The order that triggered the event.
 */
data class NewOrderEvent(
    val order: Order
) : FunpayEvent