package arc.funpay.event

import arc.funpay.event.api.FunpayEvent

data class NewChatEvent(
    val userName: String,
    val nodeId: String
) : FunpayEvent