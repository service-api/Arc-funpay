package arc.funpay.model.response

/**
 * Data class representing the response from raising lots.
 *
 * @property success Indicates whether the operation was successful.
 * @property msg The message returned from the server.
 */
data class RaiseResponse(
    val success: Boolean,
    val msg: String
)