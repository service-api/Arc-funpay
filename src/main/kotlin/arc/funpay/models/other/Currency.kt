package arc.funpay.models.other

/**
 * Enum class representing different currencies.
 */
enum class Currency {
    USD, EUR, RUB;

    companion object {
        /**
         * Converts a string representation of a currency to a Currency enum.
         *
         * @param value The string representation of the currency.
         * @return The corresponding Currency enum.
         * @throws IllegalArgumentException if the currency is unknown.
         */
        fun fromString(value: String): Currency = when (value) {
            "₽" -> RUB
            "$" -> USD
            "€" -> EUR
            else -> throw IllegalArgumentException("Unknown currency: $value")
        }
    }
}