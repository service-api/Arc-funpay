package arc.funpay.domain.common

enum class Currency {
    USD, EUR, RUB;

    companion object {
        fun fromString(value: String) = when (value) {
            "₽" -> RUB
            "$" -> USD
            "€" -> EUR
            else -> throw IllegalArgumentException("Unknown currency: $value")
        }
    }
}
