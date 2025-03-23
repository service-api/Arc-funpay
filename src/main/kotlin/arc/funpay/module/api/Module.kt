package arc.funpay.module.api

import arc.funpay.event.api.EventBus
import arc.funpay.system.FunpayAPI
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.reflect.KClass

/**
 * Abstract class representing a module in the Funpay system.
 * Implements KoinComponent for dependency injection.
 */
abstract class Module : KoinComponent {
    /**
     * Indicates whether the module is currently running.
     */
    var isRunning: Boolean = false

    /**
     * The class type to bind this module to.
     */
    open val bind: KClass<*> = this::class

    /**
     * Injected instance of FunpayAPI.
     */
    protected val api by inject<FunpayAPI>()

    /**
     * Injected instance of EventBus.
     */
    protected val eventBus by inject<EventBus>()

    /**
     * Called when the module is started.
     * Can be overridden by subclasses.
     */
    open suspend fun onStart() {}

    /**
     * Called when the module is stopped.
     * Can be overridden by subclasses.
     */
    open suspend fun onStop() {}

    /**
     * Called periodically to perform module-specific tasks.
     * Can be overridden by subclasses.
     */
    open suspend fun onTick() {}
}