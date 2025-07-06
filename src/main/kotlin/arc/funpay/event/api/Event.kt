package arc.funpay.event.api

interface Event {
    val timestamp: Long get() = System.currentTimeMillis()
}

interface CancellableEvent : Event {
    var isCancelled: Boolean
}
