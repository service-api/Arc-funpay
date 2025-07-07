package arc.funpay.core.api

import arc.funpay.event.api.EventBus
import arc.funpay.system.FunPayAPI
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class Module : KoinComponent {
    val api by inject<FunPayAPI>()
    val eventBus by inject<EventBus>()

    var isRunning = false
        private set

    open suspend fun onStart() {
        isRunning = true
    }

    open suspend fun onStop() {
        isRunning = false
    }


    open suspend fun onTick() {}
}

