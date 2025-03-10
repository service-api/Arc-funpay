package arc.funpay.event

import arc.funpay.event.api.FunpayEvent

/**
 * Event triggered when there is a change in the order count.
 *
 * @property oldCount The previous count of orders.
 * @property newCount The new count of orders.
 */
data class NewOrderEvent(
    val oldCount: Int,
    val newCount: Int
) : FunpayEvent