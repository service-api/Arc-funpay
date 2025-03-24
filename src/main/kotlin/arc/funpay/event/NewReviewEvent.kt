package arc.funpay.event

import arc.funpay.event.api.FunpayEvent

data class NewReviewEvent(
    val userId: Int,
    val text: String,
    val orderLink: String
) : FunpayEvent