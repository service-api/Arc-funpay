package arc.funpay.event.core

import arc.funpay.event.api.CancellableEvent
import arc.funpay.event.api.Event
import arc.funpay.event.api.EventBus
import arc.funpay.event.api.EventDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.ConcurrentHashMap

class DefaultEventBus(
    val dispatcher: EventDispatcher,
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : EventBus {
    val handlers = ConcurrentHashMap<Class<*>, MutableSet<suspend (Event) -> Unit>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Event> subscribe(eventType: Class<T>, handler: suspend (T) -> Unit) {
        handlers.computeIfAbsent(eventType) { ConcurrentHashMap.newKeySet() }
            .add(handler as suspend (Event) -> Unit)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Event> unsubscribe(eventType: Class<T>, handler: suspend (T) -> Unit) {
        handlers[eventType]?.remove(handler as suspend (Event) -> Unit)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Event> publish(event: T) {
        if (event is CancellableEvent && event.isCancelled) return

        val eventHandlers = handlers[event::class.java]?.map {
            it as suspend (T) -> Unit
        }?.toSet() ?: emptySet()

        dispatcher.dispatch(event, eventHandlers)
    }

    override fun shutdown() {
        scope.cancel()
        handlers.clear()
    }
}
