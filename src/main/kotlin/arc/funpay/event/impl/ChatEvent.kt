package arc.funpay.event.impl

import arc.funpay.event.api.Event

sealed interface ChatEvent : Event {
    data class NewMessage(
        val message: String,
        val author: String
    ) : ChatEvent
}
