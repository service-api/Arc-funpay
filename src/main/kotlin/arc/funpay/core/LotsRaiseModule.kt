package arc.funpay.core

import arc.funpay.core.api.Module
import arc.funpay.domain.account.Account
import arc.funpay.domain.category.Category
import arc.funpay.event.impl.LotEvent
import arc.funpay.ext.extractNumber
import arc.funpay.http.api.HttpClient
import io.ktor.client.statement.*
import org.jsoup.Jsoup
import org.koin.core.component.inject
import java.time.Instant

class LotsRaiseModule: Module() {
    val client by inject<HttpClient>()
    val account by inject<Account>()

    private val categories = mutableListOf<CategoryRaiseInfo>()

    data class CategoryRaiseInfo(
        val name: String,
        val gameId: String,
        val nodeId: String,
        var nextCheck: Long = 0L
    )

    override suspend fun onStart() {
        super.onStart()
        getAvailableCategories(account.userId).forEach {
            categories.add(CategoryRaiseInfo(it.name, it.gameId, it.nodeId))
        }
    }

    override suspend fun onStop() {
        super.onStop()
        categories.clear()
    }

    override suspend fun onTick() {
        val currentTime = Instant.now().toEpochMilli()
        categories.filter { it.nextCheck <= currentTime }
            .forEach { handleRaise(it) }
    }

    suspend fun handleRaise(category: CategoryRaiseInfo) {
        try {
            val response = api.raiseLots(category.gameId, category.nodeId)
            category.nextCheck = if (response.isSuccess) {
                if (response.message.contains("поднят") == true) {
                    eventBus.publish(LotEvent.LotsRaised(category, response.message))
                }
                calculateNextCheck(response.message)
            } else {
                calculateNextCheck(null)
            }
        } catch (_: Exception) {
            category.nextCheck = Instant.now().toEpochMilli() + 60_000L
        }
    }

    fun calculateNextCheck(message: String?): Long {
        if (message.isNullOrBlank()) return Instant.now().toEpochMilli() + 60_000L

        val currentTime = Instant.now().toEpochMilli()
        return currentTime + when {
            message.contains("час") -> 3_600_000L
            message.contains("минут") -> message.extractNumber() * 60_000L
            message.contains("секунд") -> message.extractNumber() * 1_000L
            else -> 60_000L
        }
    }

    suspend fun getAvailableCategories(userId: Long): List<Category> {
        val html = client.get("/users/$userId/", cookies = mapOf(
            "golden_key" to account.goldenKey
        )).bodyAsText()

        val gameMap = Jsoup.parse(client.get("/").bodyAsText())
            .select(".promo-game-item .game-title")
            .associate { element ->
                element.text().trim() to element.attr("data-id")
            }

        return Jsoup.parse(html)
            .select(".offer-list-title h3 a")
            .mapNotNull { element ->
                val name = element.text().trim()
                val href = element.attr("href")
                val nodeId = Regex("/lots/(\\d+)").find(href)?.groupValues?.get(1)
                val gameId = gameMap.entries.find { name.contains(it.key, ignoreCase = true) }?.value

                if (gameId != null && nodeId != null) {
                    Category(gameId, nodeId, name)
                } else null
            }
    }
}