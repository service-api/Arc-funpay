package arc.funpay.example

import arc.funpay.FunpayApplication
import arc.funpay.di.api.get
import arc.funpay.event.dsl.on
import arc.funpay.event.impl.system.SystemEvent
import arc.funpay.system.FunpayAPI

suspend fun main() {
    val app = FunpayApplication("da8vmrujr7p4kmuc4w782l5ndr6nbpfs")

    app.eventBus.on<SystemEvent.ApplicationReady> {
        println("Application ready: ${it.accountId} (${it.timestamp})")

        val api = app.container.get<FunpayAPI>()
        val accountInfo = api.getInfo()

        println("""
            Account info:
            - Balance: ${accountInfo.balance.amount}
            - Currency: ${accountInfo.balance.currency}
            - ID: ${accountInfo.userId}
            - Name: ${accountInfo.userName}
        """.trimIndent())

        app.stop()
    }

    app.eventBus.on<SystemEvent.ApplicationStopping> {
        println("Application stopping: ${it.reason}")
    }

    app.start()
}