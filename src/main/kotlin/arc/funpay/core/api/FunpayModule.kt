package arc.funpay.core.api

import arc.funpay.di.api.DependencyContainer
import arc.funpay.di.api.get

abstract class FunpayModule {
    abstract val container: DependencyContainer
    protected inline fun <reified T : Any> inject(): T = container.get()

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

