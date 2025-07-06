package arc.funpay.event.core

import arc.funpay.event.api.Event
import arc.funpay.event.api.EventDispatcher
import arc.funpay.event.api.EventErrorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DefaultEventDispatcher(
    val errorHandler: EventErrorHandler,
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : EventDispatcher {
    override suspend fun <T : Event> dispatch(event: T, handlers: Set<suspend (T) -> Unit>) {
        handlers.forEach { handler ->
            scope.launch {
                try {
                    handler(event)
                } catch (e: Exception) {
                    errorHandler.handleError(e, event)
                }
            }
        }
    }
}
