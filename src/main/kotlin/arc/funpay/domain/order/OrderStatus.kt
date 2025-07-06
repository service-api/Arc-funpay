package arc.funpay.domain.order

enum class OrderStatus(val label: String) {
    CLOSED("Закрыт"),
    OPEN("Оплачен"),
    REFUND("Возврат");

    companion object {
        fun from(text: String) = entries.find {
            text.trim().equals(it.label, ignoreCase = true)
        } ?: CLOSED
    }
}
