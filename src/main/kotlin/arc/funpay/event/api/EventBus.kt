package arc.funpay.event.api

typealias EventListener<T> = (T) -> Unit

/**
 * A class that manages event listeners and dispatches events to them.
 */
class EventBus {
    /**
     * A map of event listener collections, keyed by event class.
     */
    val listeners = mutableMapOf<Class<out FunpayEvent>, MutableCollection<(FunpayEvent) -> Unit>>()

    /**
     * Registers an event listener for the specified event type.
     *
     * @param T The type of the event.
     * @param action The event listener to register.
     */
    suspend inline fun <reified T : FunpayEvent> on(noinline action: EventListener<T>) {
        val clazz = T::class.java
        listeners.computeIfAbsent(clazz) { mutableListOf() }.add { action(it as T) }
    }

    /**
     * Dispatches an event to all registered listeners.
     *
     * @param event The event to dispatch.
     */
    fun post(event: FunpayEvent) {
        if (event is Cancelable && event.isCancelled) return
        listeners[event::class.java]?.forEach { it(event) }
    }
}