package arc.funpay.event

import arc.funpay.event.api.FunpayEvent
import arc.funpay.module.funpay.ReviewEventModule

data class NewReviewEvent(
    val review: ReviewEventModule.Review
) : FunpayEvent