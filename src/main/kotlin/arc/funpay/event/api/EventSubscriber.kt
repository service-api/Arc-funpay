package arc.funpay.event.api

interface EventSubscriber {
    fun <T : Event> subscribe(eventType: Class<T>, handler: suspend (T) -> Unit)
    fun <T : Event> unsubscribe(eventType: Class<T>, handler: suspend (T) -> Unit)
}
