package arc.funpay.event.api

interface EventPublisher {
    suspend fun <T : Event> publish(event: T)
}
