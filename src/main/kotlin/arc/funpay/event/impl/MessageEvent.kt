package arc.funpay.event.impl

import arc.funpay.event.api.Event

sealed interface MessageEvent : Event {
    data class NewMessage(
        val message: String
    ) : MessageEvent
}
