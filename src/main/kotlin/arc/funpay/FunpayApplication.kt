
package arc.funpay

import arc.funpay.core.api.Module
import arc.funpay.di.module.CoreServicesModule
import arc.funpay.di.module.EventModule
import arc.funpay.di.module.FunPayModule
import arc.funpay.di.module.HttpModule
import arc.funpay.domain.account.Account
import arc.funpay.event.api.EventBus
import arc.funpay.event.impl.SystemEvent
import arc.funpay.system.FunPayAPI
import kotlinx.coroutines.*
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

class FunpayApplication(val goldKey: String) {
    val logger: Logger = LoggerFactory.getLogger(this::class.qualifiedName)
    val koin = startKoin {
        modules(
            CoreServicesModule().createModule(),
            EventModule().createModule(),
            HttpModule().createModule(),
            FunPayModule().createModule()
        )
    }.koin

    var job: Job? = null
    val eventBus by lazy { koin.get<EventBus>() }

    suspend fun start() {
        Account.fromToken(goldKey, koin.get())?.takeIf { it.isValid() }?.let { account ->
            koin.loadModules(listOf(module {
                single { account }
                single { FunPayAPI(get(), get()) }
            }))
            startModules()

            eventBus.publish(SystemEvent.ApplicationReady(account))
        } ?: run {
            logger.error("Invalid token")
            stop()
        }
    }

    fun startModules() {
        val modules = koin.getAll<Module>()

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        job = scope.launch {
            modules.forEach { module ->
                runCatching { module.onStart() }
                    .onFailure { logger.error("Error starting module ${module::class.simpleName}", it) }
            }

            while (isActive) {
                modules.forEach { module ->
                    if (module.isRunning) {
                        runCatching { module.onTick() }
                            .onFailure { logger.error("Error in module ${module::class.simpleName}", it) }
                    }
                }
                delay(10.seconds)
            }
        }
    }

    suspend fun stop() {
        eventBus.publish(SystemEvent.ApplicationStopping("User requested to stop"))
        job?.cancel()
        koin.getAll<Module>().forEach { it.onStop() }
        koin.close()
        eventBus.shutdown()
    }
}