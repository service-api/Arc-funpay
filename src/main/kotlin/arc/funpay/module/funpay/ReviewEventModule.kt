package arc.funpay.module.funpay

import arc.funpay.event.NewReviewEvent
import arc.funpay.event.api.FunpayEvent
import arc.funpay.model.funpay.Account
import arc.funpay.module.api.Module
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.koin.core.component.inject

class ReviewEventModule : Module() {
    var isFirst = true
    var lastReviewId: String? = null
    val account by inject<Account>()

    override suspend fun onTick() {
        val events = fetchEvents()
        events.forEach { eventBus.post(it) }
    }

    suspend fun fetchEvents(): List<FunpayEvent> {
        val reviews = parseReviews()
        val newEvents = mutableListOf<FunpayEvent>()

        val latest = reviews.firstOrNull() ?: return emptyList()

        if (isFirst) {
            lastReviewId = latest.id
            isFirst = false
            return emptyList()
        }

        if (latest.id != lastReviewId) {
            newEvents.add(NewReviewEvent(latest.userId, latest.text, latest.orderLink))
            lastReviewId = latest.id
        }

        return newEvents
    }

    suspend fun parseReviews(): List<Review> = withContext(Dispatchers.IO) {
        val userId = account.userId
        val url = "https://funpay.com/users/$userId/"
        val doc = Jsoup.connect(url).get()
        val reviews = mutableListOf<Review>()

        val reviewElements = doc.select(".review-item")

        for ((index, el) in reviewElements.withIndex()) {
            val userLink = el.selectFirst(".review-item-user a")?.attr("href") ?: continue
            val userIdParsed = Regex("""/users/(\d+)/?""").find(userLink)?.groupValues?.get(1)?.toIntOrNull() ?: continue

            val orderLink = el.selectFirst(".review-item-order a")?.attr("href") ?: ""

            val text = el.selectFirst(".review-item-text")?.text()?.trim() ?: ""

            val rating = parseReviewRating(el)

            reviews.add(
                Review(
                    id = "review-$index",
                    userId = userIdParsed,
                    orderLink = orderLink,
                    text = text,
                    rating = rating
                )
            )
        }

        return@withContext reviews
    }

    fun parseReviewRating(element: Element): Int {
        val ratingDiv = element.selectFirst(".review-item-user .rating > div")
        val className = ratingDiv?.className() ?: return 0

        return Regex("""rating(\d)""").find(className)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    suspend fun getReviewStats(): Map<Int, Int> {
        val reviews = parseReviews()
        return (1..5).associateWith { rating ->
            reviews.count { it.rating == rating }
        }
    }

    data class Review(
        val id: String,
        val userId: Int,
        val orderLink: String,
        val text: String,
        val rating: Int
    )
}
