package arc.funpay.model.funpay

enum class OrderStatus(val label: String) {
    CLOSED("Закрыт"),
    OPEN("Оплачен"),
    REFUND("Возврат");

    companion object {
        fun from(text: String): OrderStatus = entries.find {
            text.trim().equals(it.label, ignoreCase = true)
        } ?: CLOSED
    }
}