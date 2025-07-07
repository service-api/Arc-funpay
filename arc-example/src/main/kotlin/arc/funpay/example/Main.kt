package arc.funpay.example

import arc.funpay.FunpayApplication
import arc.funpay.event.dsl.on
import arc.funpay.event.impl.ChatEvent
import arc.funpay.event.impl.LotEvent
import arc.funpay.event.impl.SystemEvent
import arc.funpay.system.FunPayAPI

@OptIn(ExperimentalStdlibApi::class)
suspend fun main() {
    val app = FunpayApplication("")

    app.eventBus.on<SystemEvent.ApplicationReady> {
        println("Application ready: ${it.account} (${it.timestamp})")

        val api = app.koin.get<FunPayAPI>()
        val accountInfo = api.getInfo()

        println("""
            Account info:
            - Balance: ${accountInfo.balance.amount}
            - Currency: ${accountInfo.balance.currency}
        """.trimIndent())
    }

    app.eventBus.on<ChatEvent.NewMessage> {
        println("New message: ${it.message}")
    }

    app.eventBus.on<LotEvent.LotsRaised> {
        println("Lots raised: ${it.category} (${it.message})")
    }

    app.eventBus.on<SystemEvent.ApplicationStopping> {
        println("Application stopping: ${it.reason}")
    }

    app.start()
}