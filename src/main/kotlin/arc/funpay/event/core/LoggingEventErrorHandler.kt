package arc.funpay.event.core

import arc.funpay.event.api.Event
import arc.funpay.event.api.EventErrorHandler
import org.slf4j.LoggerFactory

class LoggingEventErrorHandler : EventErrorHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun handleError(error: Throwable, event: Event) {
        logger.error("Error handling event: $event", error)
    }
}
