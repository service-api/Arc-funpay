package arc.funpay.event

import arc.funpay.event.api.FunpayEvent
import arc.funpay.model.funpay.Order

/**
 * Represents an event that is triggered when an order is closed.
 *
 * This event contains information about the order that was closed.
 *
 * @property order The order that was closed.
 */
data class OrderCloseEvent(
    val order: Order
) : FunpayEvent