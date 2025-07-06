package arc.funpay.event.dsl

import arc.funpay.event.api.Event
import arc.funpay.event.api.EventBus

inline fun <reified T : Event> EventBus.on(noinline handler: suspend (T) -> Unit) {
    subscribe(T::class.java, handler)
}

inline fun <reified T : Event> EventBus.off(noinline handler: suspend (T) -> Unit) {
    unsubscribe(T::class.java, handler)
}
