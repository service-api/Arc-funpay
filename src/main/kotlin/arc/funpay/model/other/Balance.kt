package arc.funpay.model.other

/**
 * Data class representing a balance with an amount and currency.
 *
 * @property amount The amount of the balance.
 * @property currency The currency of the balance.
 */
data class Balance(
    val amount: Double,
    val currency: Currency
)