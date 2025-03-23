package arc.funpay

import arc.funpay.event.ReadyEvent
import arc.funpay.event.api.EventBus
import arc.funpay.model.funpay.Account
import arc.funpay.model.other.Proxy
import arc.funpay.module.api.Module
import arc.funpay.module.funpay.OrderEventModule
import arc.funpay.module.funpay.OrderStatusModule
import arc.funpay.system.FunpayAPI
import arc.funpay.system.api.FunpayHttpClient
import kotlinx.coroutines.*
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

/**
 * Main application class for Funpay.
 *
 * @property goldToken The token used for authentication.
 */
class FunpayApplication(
    val goldToken: String,
    val proxy: Proxy? = null
) {
    var isRunnable = false
        private set
    val logger = LoggerFactory.getLogger(this::class.qualifiedName)
    val koin = startKoin {
        modules(module {
            single { FunpayHttpClient(proxy) }
        })
    }.koin

    val modules = hashSetOf<Module>(
        OrderEventModule(),
        OrderStatusModule()
    )

    val eventBus = EventBus()

    /**
     * Starts the application.
     */
    suspend fun start() {
        val account = Account.fromToken(goldToken, koin.get())
        if (account == null || !account.isValid()) {
            logger.error("Invalid token")
            stop()
            return
        }

        println("""
 ________                                                 _       _______  _____
|_   __  |                                               / \     |_   __ \|_   _|
  | |_ \_|__   _   _ .--.  _ .--.   ,--.    _   __      / _ \      | |__) | | |
  |  _|  [  | | | [ `.-. |[ '/'`\ \`'_\ :  [ \ [  ]    / ___ \     |  ___/  | |
 _| |_    | \_/ |, | | | | | \__/ |// | |,  \ '/ /   _/ /   \ \_  _| |_    _| |_
|_____|   '.__.'_/[___||__]| ;.__/ \'-;__/[\_:  /   |____| |____||_____|  |_____|
                          [__|             \__.'

           Funpay API started on account with id: ${account.userId}
        """.trimIndent())

        koin.loadModules(listOf(module {
            single { account }
            single { FunpayAPI(get(), get()) }
            single { eventBus }
            modules.forEach { module -> single { module } bind module.bind as KClass<Module> }
        }))

        modules.forEach {
            it.isRunning = true
            it.onStart()
            logger.info("${it.javaClass.simpleName} started")
        }

        isRunnable = true
        eventBus.post(ReadyEvent())

        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        scope.launch {
            while (isRunnable) {
                modules.forEach { it.onTick() }
                delay(600L * 10)
            }
            scope.cancel()
        }
    }

    /**
     * Stops the application.
     */
    suspend fun stop() {
        isRunnable = false
        runBlocking {
            koin.close()
        }

        modules.forEach {
            if (!it.isRunning) return@forEach
            it.onStop()
            logger.info("${it.javaClass.simpleName} stopped")
        }
    }

    /**
     * Adds a module to the application.
     * Use on startup.
     *
     * @param module The module to add.
     */
    fun addModule(module: Module) {
        modules.add(module)
    }

    /**
     * Removes a module from the application.
     * Use on startup.
     *
     * @param module The module to remove.
     */
    fun removeModule(module: Module) {
        modules.remove(module)
    }

    /**
     * Starts a specific module.
     * Use in runtime.
     *
     * @param module The module to start.
     */
    suspend fun startModule(module: Module) {
        module.onStart()
        modules.add(module)
    }

    /**
     * Stops a specific module.
     * Use in runtime.
     *
     * @param module The module to stop.
     */
    suspend fun stopModule(module: Module) {
        module.onStop()
        modules.remove(module)
    }
}