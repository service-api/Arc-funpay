package arc.funpay.module.funpay

import arc.funpay.event.NewReviewEvent
import arc.funpay.event.api.FunpayEvent
import arc.funpay.model.funpay.Account
import arc.funpay.module.api.Module
import arc.funpay.system.api.FunpayHttpClient
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.koin.core.component.inject

/**
 * Module responsible for monitoring and handling review events from Funpay.
 *
 * This module periodically fetches review data from the user's Funpay profile,
 * detects new reviews, and dispatches appropriate events to the event bus.
 */
class ReviewEventModule : Module() {
    /**
     * Flag indicating whether this is the first run of the module.
     * Used to establish a baseline of reviews without triggering events on startup.
     */
    var isFirst = true

    /**
     * Stores the ID of the most recent review to detect new reviews.
     */
    var lastReviewId: String? = null

    /**
     * The Funpay account associated with this module.
     * Injected using Koin dependency injection.
     */
    val account by inject<Account>()

    /**
     * HTTP client for making requests to Funpay.
     * Injected using Koin dependency injection.
     */
    val client by inject<FunpayHttpClient>()

    /**
     * Executes on each tick of the module.
     * Fetches new review events and posts them to the event bus.
     */
    override suspend fun onTick() {
        val events = fetchEvents()
        events.forEach { eventBus.post(it) }
    }

    /**
     * Fetches and processes new review events.
     *
     * On first run, it just establishes the latest review ID without generating events.
     * On subsequent runs, it compares the latest review with the previously saved one
     * and generates events for new reviews.
     *
     * @return List of new Funpay events detected during this fetch operation
     */
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
            newEvents.add(NewReviewEvent(latest))
            lastReviewId = latest.id
        }

        return newEvents
    }

    /**
     * Parses the user's Funpay profile page to extract reviews.
     *
     * This method fetches the HTML content of the user's profile page and extracts
     * review information including user ID, order link, review text, and rating.
     *
     * @return List of parsed Review objects sorted by most recent first
     */
    suspend fun parseReviews(): List<Review> = withContext(Dispatchers.IO) {
        val userId = account.userId

        val html = client.get(
            "/users/$userId/",
            cookies = mapOf(
                "golden_key" to account.goldenKey,
                "PHPSESSID" to account.phpSessionId
            )
        ).bodyAsText()

        val doc: Document = Jsoup.parse(html)
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

    /**
     * Extracts the numeric rating from a review element.
     *
     * Looks for CSS classes in the format "ratingX" where X is a number
     * representing the rating value (1-5).
     *
     * @param element The HTML element containing the review
     * @return The numeric rating (1-5) or 0 if not found
     */
    private fun parseReviewRating(element: Element): Int {
        // Ищем все div с классом ratingX внутри .rating
        val ratingBlocks = element.select(".review-item-rating .rating > div")
        for (div in ratingBlocks) {
            val match = Regex("""rating(\d)""").find(div.className())
            if (match != null) {
                return match.groupValues[1].toIntOrNull() ?: 0
            }
        }
        return 0
    }

    /**
     * Computes statistics about review ratings.
     *
     * @return Map where keys are rating values (1-5) and values are the count of reviews with that rating
     */
    suspend fun getReviewStats(): Map<Int, Int> {
        val reviews = parseReviews()
        return (1..5).associateWith { rating ->
            reviews.count { it.rating == rating }
        }
    }

    /**
     * Data class representing a review on Funpay.
     *
     * @property id Unique identifier for the review (format: "review-{index}")
     * @property userId The numeric ID of the user who left the review
     * @property orderLink URL linking to the order associated with this review
     * @property text The text content of the review
     * @property rating Numeric rating given in the review (1-5)
     */
    data class Review(
        val id: String,
        val userId: Int,
        val orderLink: String,
        val text: String,
        val rating: Int
    )
}