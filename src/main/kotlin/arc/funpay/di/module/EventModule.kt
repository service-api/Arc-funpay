package arc.funpay.di.module

import arc.funpay.di.api.AbstractModule
import arc.funpay.di.api.Binding
import arc.funpay.di.api.get
import arc.funpay.event.api.EventBus
import arc.funpay.event.api.EventDispatcher
import arc.funpay.event.core.DefaultEventBus
import arc.funpay.event.core.DefaultEventDispatcher
import arc.funpay.event.core.LoggingEventErrorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class EventModule : AbstractModule() {
    override fun bindings(): List<Binding<*>> = listOf(
        singleton<CoroutineScope> {
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
        },

        singleton<EventDispatcher> {
            DefaultEventDispatcher(LoggingEventErrorHandler())
        },

        singleton<EventBus> { container ->
            DefaultEventBus(container.get(), container.get())
        }
    )
}