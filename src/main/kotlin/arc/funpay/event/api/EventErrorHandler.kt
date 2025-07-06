package arc.funpay.event.api

interface EventErrorHandler {
    fun handleError(error: Throwable, event: Event)
}
