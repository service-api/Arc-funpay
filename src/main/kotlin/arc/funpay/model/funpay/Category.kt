package arc.funpay.model.funpay

/**
 * Data class representing a category with game and node information.
 *
 * @property gameId The ID of the game associated with the category.
 * @property nodeId The ID of the node associated with the category.
 * @property name The name of the category.
 * @property nextCheck The timestamp for the next check (default is 0L).
 */
data class Category(
    val gameId: String,
    val nodeId: String,
    val name: String,
    var nextCheck: Long = 0L
)