package arc.funpay.event

import arc.funpay.event.api.FunpayEvent

data class NewMessageEvent(
    val userName: String,
    val nodeId: String,
    val message: String
) : FunpayEvent