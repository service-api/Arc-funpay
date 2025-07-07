package arc.funpay.di.module

import arc.funpay.di.api.ServiceModule
import arc.funpay.event.api.EventBus
import arc.funpay.event.api.EventDispatcher
import arc.funpay.event.core.DefaultEventBus
import arc.funpay.event.core.DefaultEventDispatcher
import arc.funpay.event.core.LoggingEventErrorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

class EventModule : ServiceModule {
    override fun createModule() = module {
        single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
        single<EventDispatcher> { DefaultEventDispatcher(LoggingEventErrorHandler()) }
        single<EventBus> { DefaultEventBus(get(), get()) }
    }
}