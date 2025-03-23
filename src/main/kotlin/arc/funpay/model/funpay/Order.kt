package arc.funpay.model.funpay

import java.time.LocalDate

data class Order(
    val date: LocalDate,
    val orderId: String,
    val description: String,
    val category: String,
    val buyer: String,
    val buyerId: String,
    val status: OrderStatus,
    val amount: Double
)