package arc.funpay.event.impl

import arc.funpay.domain.account.Account
import arc.funpay.event.api.Event

sealed interface SystemEvent : Event {
    data class ApplicationReady(
        val account: Account
    ) : SystemEvent

    data class ApplicationStopping(
        val reason: String? = null
    ) : SystemEvent
}