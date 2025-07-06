package arc.funpay.event.api

interface EventBus : EventPublisher, EventSubscriber {
    fun shutdown()
}