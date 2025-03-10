package arc.funpay.models.other

import kotlinx.serialization.Serializable

/**
 * Data class representing orders with buyer and seller information.
 *
 * @property buyer The count of orders where the user is the buyer.
 * @property seller The count of orders where the user is the seller.
 */
@Serializable
data class Orders(
    val buyer: Int,
    val seller: Int
)