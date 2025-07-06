package arc.funpay

import arc.funpay.di.api.AbstractModule
import arc.funpay.di.api.DependencyContainer
import arc.funpay.di.api.get
import arc.funpay.di.impl.KoinContainer
import arc.funpay.di.module.CoreServicesModule
import arc.funpay.di.module.EventModule
import arc.funpay.di.module.FunPayModule
import arc.funpay.di.module.HttpModule
import arc.funpay.domain.account.Account
import arc.funpay.event.api.EventBus
import arc.funpay.event.impl.system.SystemEvent
import arc.funpay.system.FunPayAPI
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FunpayApplication(
    val goldKey: String,
    val container: DependencyContainer = KoinContainer()
) {
    init{
        container.loadModules(
            CoreServicesModule(),
            EventModule(),
            HttpModule(),
            FunPayModule()
        )
    }

    val logger: Logger = LoggerFactory.getLogger(this::class.qualifiedName)

    val eventBus by lazy { container.get<EventBus>() }

    suspend fun start() {
        val account = Account.fromToken(goldKey, container.get())
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
        container.loadModules(object : AbstractModule() {
            override fun bindings() = listOf(
                singleton<Account> { account },
                singleton<FunPayAPI> { c -> FunPayAPI(c.get(), c.get(), c.get()) }
            )
        })

        eventBus.publish(SystemEvent.ApplicationReady(account.userId))
    }

    suspend fun stop() {
        eventBus.publish(SystemEvent.ApplicationStopping("User requested to stop"))
        container.close()
        eventBus.shutdown()
    }
}