package arc.funpay.event

import arc.funpay.event.api.FunpayEvent
import arc.funpay.model.funpay.Category

/**
 * Event triggered when lots are raised in a specific category.
 *
 * @property category The category of the lots being raised.
 * @property message A message associated with the event.
 */
data class LotsRaiseEvent(
    val category: Category,
    val message: String
) : FunpayEvent