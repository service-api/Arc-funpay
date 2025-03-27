package arc.funpay.patern

object ChatPattern {
    val ORDER_CONFIRMED: Regex = Regex(
        """Покупатель\p{Zs}+(\w+)\p{Zs}+подтвердил\p{Zs}+успешное\p{Zs}+выполнение\p{Zs}+заказа\p{Zs}+#([A-Z0-9]+)\p{Zs}+и\p{Zs}+отправил\p{Zs}+деньги\p{Zs}+продавцу\p{Zs}+(\w+)\."""
    )

    val ORDER_REVIEW: Regex = Regex(
        """Покупатель\p{Zs}+(\w+)\p{Zs}+написал\p{Zs}+отзыв\p{Zs}+к\p{Zs}+заказу\p{Zs}+#([A-Z0-9]+)\."""
    )

    val SELLER_REPLIED_TO_REVIEW: Regex = Regex(
        """Продавец\p{Zs}+(\w+)\p{Zs}+ответил\p{Zs}+на\p{Zs}+отзыв\p{Zs}+к\p{Zs}+заказу\p{Zs}+#([A-Z0-9]+)\."""
    )

    val ORDER_REFUNDED: Regex = Regex(
        """Продавец\p{Zs}+(\w+)\p{Zs}+вернул\p{Zs}+деньги\p{Zs}+покупателю\p{Zs}+(\w+)\p{Zs}+по\p{Zs}+заказу\p{Zs}+#([A-Z0-9]+)\."""
    )

    val ORDER_REVIEW_DELETED: Regex = Regex(
        """Покупатель\p{Zs}+(\w+)\p{Zs}+удалил\p{Zs}+отзыв\p{Zs}+к\p{Zs}+заказу\p{Zs}+#([A-Z0-9]+)\."""
    )

    val ORDER_REVIEW_EDITED: Regex = Regex(
        """Покупатель\p{Zs}+(\w+)\p{Zs}+изменил\p{Zs}+отзыв\p{Zs}+к\p{Zs}+заказу\p{Zs}+#([A-Z0-9]+)\."""
    )

    val SUPPORT_MESSAGE: Regex = Regex(
        """Приглашаем\p{Zs}+вас\p{Zs}+в\p{Zs}+наш\p{Zs}+закрытый\p{Zs}+Telegram.*"""
    )

    val EXTERNAL_COMMUNICATION_WARNING: Regex = Regex(
        """Вы\p{Zs}+можете\p{Zs}+перейти\p{Zs}+в\p{Zs}+Discord\.?\p{Zs}+Внимание:\p{Zs}+общение\p{Zs}+за\p{Zs}+пределами\p{Zs}+сервера\p{Zs}+FunPay\p{Zs}+считается\p{Zs}+нарушением\p{Zs}+правил\."""
    )

    val ORDER_OPENED: Regex = Regex(
        """Покупатель\p{Zs}+(\w+)\p{Zs}+оплатил\p{Zs}+заказ\p{Zs}+#([A-Z0-9]+)\."""
    )
}