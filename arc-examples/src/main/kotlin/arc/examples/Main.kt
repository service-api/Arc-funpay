package arc.examples

import arc.examples.ArcPath.absolute
import arc.examples.modules.LotsRaiseModule
import arc.funpay.FunpayApplication
import arc.funpay.event.*
import arc.funpay.event.pre.PreLotsRaiseEvent
import arc.funpay.model.funpay.Account
import arc.funpay.model.funpay.Category
import arc.funpay.system.FunpayAPI
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString

/**
 * Main entry point for the application.
 *
 * This function demonstrates the usage of the Funpay API client.
 * It performs the following operations:
 * 1. Loads configuration from a YAML file
 * 2. Initializes the Funpay application with a token
 * 3. Retrieves and displays account information
 * 4. Fetches order counts
 * 5. Raises lots for specific game categories
 * 6. Demonstrates module integration with LotsRaiseModule
 * 7. Sets up event listeners for various application events
 * 8. Starts the application
 */
suspend fun main() {
    val configFile = absolute("D:\\Java\\Arc-funpay\\arc-examples\\src\\main\\resources\\config.yml").readText()
    val config = Yaml.default.decodeFromString<Config>(configFile)

    val app = FunpayApplication(config.token)
    // Http proxy:
    //  FunpayApplication(config.token, Proxy.HttpProxy("Here http proxy"))
    // Socks proxy:
    // FunpayApplication(config.token, Proxy.SocksProxy("host", 1080))

    app.start()

    val api = app.koin.get<FunpayAPI>()
    val chatId = api.getChatNodeByUsername("JIeT") ?: "0"

    api.sendMessage(chatId, "Hello from Arc!")
    // Account info

    val accountInfo = api.getInfo()

    val account = app.koin.get<Account>()
    println("""
        Account info:
        - Balance: ${accountInfo.balance.amount}
        - Currency: ${accountInfo.balance.currency}
        - ID: ${accountInfo.id}
        - Name: ${accountInfo.name}
        - Token: ${account.csrfToken}
    """.trimIndent())

    // Order count

    val orderCount = api.getOrders()

    println("""
        Order info:
        - Orders where user is seller: ${orderCount.seller}
        - Orders where user is buyer: ${orderCount.buyer}
    """.trimIndent())

    // Lots raise

    val lots = api.raiseLots(
        "41", // Dota 2
        "504", // Прочее
    )

    println("""
        Lots raise:
        - Lots status: ${lots.success}
        - Lots raise message: ${lots.msg}
    """.trimIndent())

    // Module example
    app.addModule(LotsRaiseModule(listOf(
        Category(
            gameId = "41", // Dota 2
            nodeId = "504", // Прочее
            name = "Dota 2 Other",
        )
    )))


    // Event example
    app.eventBus.on<PreLotsRaiseEvent> {
        println("PreLotsRaiseEvent: ${it.category.name} -> ${it.category.nextCheck}")
        // it.isCancelled = true // Can be cancelled if event is Pre
    }

    app.eventBus.on<LotsRaiseEvent> {
        println("LotsRaiseEvent: ${it.category.name} -> ${it.message}")
    }

    app.eventBus.on<NewOrderEvent> {
        println("NewOrderEvent: ${it.order}")
    }

    app.eventBus.on<OrderCloseEvent> {
        println("OrderCloseEvent: ${it.order}")
    }

    app.eventBus.on<NewPurchaseEvent> {
        println("NewPurchaseEvent: ${it.oldCount} -> ${it.newCount}")
    }

    app.eventBus.on<NewChatEvent> {
        println("NewChatEvent: ${it.nodeId} -> ${it.userName}")
    }

    app.eventBus.on<NewMessageEvent> {
        println("NewMessageEvent: ${it.userName} -> ${it.message}")
    }

}