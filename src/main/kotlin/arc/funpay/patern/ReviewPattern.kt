package arc.funpay.patern

import arc.funpay.patern.api.MessagePattern

sealed class ReviewPattern : MessagePattern {
    data object Created : ReviewPattern() {
        override val pattern = Regex(
            """Покупатель\p{Zs}+(\w+)\p{Zs}+написал\p{Zs}+отзыв\p{Zs}+к\p{Zs}+заказу\p{Zs}+#([A-Z0-9]+)\."""
        )

        override fun extractData(message: String): Map<String, String> {
            val matchResult = pattern.find(message) ?: return emptyMap()
            return mapOf(
                "buyer" to (matchResult.groups[1]?.value ?: ""),
                "orderId" to (matchResult.groups[2]?.value ?: "")
            )
        }
    }
}