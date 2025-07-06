package arc.funpay.event.api

interface EventDispatcher {
    suspend fun <T : Event> dispatch(event: T, handlers: Set<suspend (T) -> Unit>)
}
