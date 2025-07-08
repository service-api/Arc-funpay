package arc.funpay.event.impl

import arc.funpay.domain.chat.ChatMessage
import arc.funpay.event.api.Event

sealed interface ChatEvent : Event {
    data class NewMessage(
        val message: ChatMessage
    ) : ChatEvent
}
