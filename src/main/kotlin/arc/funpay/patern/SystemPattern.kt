package arc.funpay.patern

import arc.funpay.patern.api.MessagePattern

sealed class SystemPattern : MessagePattern {
    data object TelegramJoin : SystemPattern() {
        override val pattern = Regex(
            """Приглашаем\p{Zs}+вас\p{Zs}+в\p{Zs}+наш\p{Zs}+закрытый\p{Zs}+Telegram.*"""
        )

        override fun extractData(message: String): Map<String, String> {
            return mapOf("type" to "telegram")
        }
    }

    data object DiscordCommunication : SystemPattern() {
        override val pattern = Regex(
            """Вы\p{Zs}+можете\p{Zs}+перейти\p{Zs}+в\p{Zs}+Discord\.?\p{Zs}+Внимание:\p{Zs}+общение\p{Zs}+за\p{Zs}+пределами\p{Zs}+сервера\p{Zs}+FunPay\p{Zs}+считается\p{Zs}+нарушением\p{Zs}+правил\."""
        )

        override fun extractData(message: String): Map<String, String> {
            return mapOf("type" to "discord")
        }
    }
}