package arc.funpay.patern

import arc.funpay.patern.api.MessagePattern

sealed class OrderPattern : MessagePattern {
    data object Confirmed : OrderPattern() {
        override val pattern = Regex(
            """Покупатель\p{Zs}+(\w+)\p{Zs}+подтвердил\p{Zs}+успешное\p{Zs}+выполнение\p{Zs}+заказа\p{Zs}+#([A-Z0-9]+)\p{Zs}+и\p{Zs}+отправил\p{Zs}+деньги\p{Zs}+продавцу\p{Zs}+(\w+)\."""
        )

        override fun extractData(message: String): Map<String, String> {
            val matchResult = pattern.find(message) ?: return emptyMap()
            return mapOf(
                "buyer" to (matchResult.groups[1]?.value ?: ""),
                "orderId" to (matchResult.groups[2]?.value ?: ""),
                "seller" to (matchResult.groups[3]?.value ?: "")
            )
        }
    }

    data object Opened : OrderPattern() {
        override val pattern = Regex(
            """Покупатель\p{Zs}+(\w+)\p{Zs}+оплатил\p{Zs}+заказ\p{Zs}+#([A-Z0-9]+)\."""
        )

        override fun extractData(message: String): Map<String, String> {
            val matchResult = pattern.find(message) ?: return emptyMap()
            return mapOf(
                "buyer" to (matchResult.groups[1]?.value ?: ""),
                "orderId" to (matchResult.groups[2]?.value ?: "")
            )
        }
    }

    data object Refunded : OrderPattern() {
        override val pattern = Regex(
            """Продавец\p{Zs}+(\w+)\p{Zs}+вернул\p{Zs}+деньги\p{Zs}+покупателю\p{Zs}+(\w+)\p{Zs}+по\p{Zs}+заказу\p{Zs}+#([A-Z0-9]+)\."""
        )

        override fun extractData(message: String): Map<String, String> {
            val matchResult = pattern.find(message) ?: return emptyMap()
            return mapOf(
                "seller" to (matchResult.groups[1]?.value ?: ""),
                "buyer" to (matchResult.groups[2]?.value ?: ""),
                "orderId" to (matchResult.groups[3]?.value ?: "")
            )
        }
    }
}