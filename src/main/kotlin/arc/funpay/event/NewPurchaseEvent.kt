package arc.funpay.event

import arc.funpay.event.api.FunpayEvent

/**
 * Event triggered when there is a new purchase.
 *
 * @property oldCount The previous count of purchases.
 * @property newCount The new count of purchases.
 */
data class NewPurchaseEvent(
    val oldCount: Int,
    val newCount: Int
) : FunpayEvent