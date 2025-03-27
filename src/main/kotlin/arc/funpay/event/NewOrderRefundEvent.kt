package arc.funpay.event

import arc.funpay.event.api.FunpayEvent
import arc.funpay.model.funpay.Order

/**
 * Event triggered when an order refund occurs.
 *
 * This event represents the refund of an order.
 *
 * @property order The order that is being refunded.
 */
data class NewOrderRefundEvent(
    val order: Order
) : FunpayEvent