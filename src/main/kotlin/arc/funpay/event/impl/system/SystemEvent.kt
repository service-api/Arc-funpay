package arc.funpay.event.impl.system

import arc.funpay.event.api.CancellableEvent
import arc.funpay.event.api.Event

sealed interface SystemEvent : Event {
    data class ApplicationReady(
        val accountId: Long
    ) : SystemEvent

    data class ApplicationStopping(
        val reason: String? = null
    ) : SystemEvent, CancellableEvent {
        override var isCancelled: Boolean = false
    }
}
