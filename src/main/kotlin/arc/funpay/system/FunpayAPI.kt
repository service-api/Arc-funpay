package arc.funpay.system

import arc.funpay.GlobalSettings
import arc.funpay.ext.extract
import arc.funpay.ext.parse
import arc.funpay.model.funpay.Account
import arc.funpay.model.funpay.AccountInfo
import arc.funpay.model.funpay.CategoryInfo
import arc.funpay.model.other.Balance
import arc.funpay.model.other.Currency
import arc.funpay.model.other.Orders
import arc.funpay.model.response.RaiseResponse
import arc.funpay.system.api.FunpayHttpClient
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

/**
 * Class representing the Funpay API.
 *
 * This class provides methods to interact with the Funpay API,
 * such as retrieving account information, sending messages,
 * getting order details, refunding orders, and raising lots.
 *
 * @property client The HTTP client used for making requests.
 * @property account The account associated with the API.
 */
class FunpayAPI(
    val client: FunpayHttpClient,
    val account: Account
) {
    /**
     * Retrieves account information.
     *
     * This function fetches account details such as user ID, username, and balance
     * from the Funpay API.
     *
     * @return An AccountInfo object containing user ID, username, and balance.
     */
    fun getInfo(): AccountInfo = runBlocking {
        val html = client.get("/", cookies = mapOf("golden_key" to account.goldenKey)).bodyAsText()

        val rawJsonEncoded = Jsoup.parse(html).body().attr("data-app-data")
        val rawJson = Parser.unescapeEntities(rawJsonEncoded, false)

        val userId = try {
            val jsonElement = Json.parseToJsonElement(rawJson)
            jsonElement.jsonObject["userId"]?.jsonPrimitive?.longOrNull ?: 0L
        } catch (_: Exception) {
            0L
        }

        val username = html.extract("""<div class=\"user-link-name\">(.*?)<\/div>""")

        val balances = Regex("""<span class="badge badge-balance">([\d.,]+)\s*([^\d\s<]+)</span>""")
            .findAll(html)
            .map { matchResult ->
                val (value, currency) = matchResult.destructured
                Balance(value.replace(",", ".").toDouble(), Currency.fromString(currency))
            }
            .toList()

        val primaryBalance = balances.firstOrNull() ?: Balance(0.0, Currency.RUB)

        AccountInfo(userId, username, primaryBalance)
    }

    /**
     * Sends a review or response to a review for a specific order.
     *
     * This function submits a review with text content and a rating for a given order ID.
     * The rating must be between 1 and 5, with 5 being the default (highest rating).
     *
     * @param orderId The ID of the order to review.
     * @param text The content of the review message.
     * @param rating The rating to give, from 1 to 5 (default is 5).
     * @return The content of the successful response from the server.
     * @throws IllegalArgumentException If the rating is not between 1 and 5.
     * @throws Exception If the request fails with HTTP 400 (with details from the response) or any other non-200 status.
     */
    suspend fun sendReview(orderId: String, text: String, rating: Int = 5): String {
        require(rating in 1..5) { "Rating must be between 1 and 5." }

        val headers = mapOf(
            HttpHeaders.Accept to "*/*",
            "X-Requested-With" to "XMLHttpRequest"
        )

        val body = mapOf(
            "authorId" to account.userId.toString(),
            "text" to text,
            "rating" to rating.toString(),
            "csrf_token" to account.csrfToken,
            "orderId" to orderId
        )

        val response = client.post(
            endpoint = "/orders/review",
            headers = headers,
            cookies = mapOf(
                "golden_key" to account.goldenKey,
                "PHPSESSID" to account.phpSessionId
            ),
            body = body
        )

        val status = response.status.value
        val responseText = response.bodyAsText()

        if (status == 400) {
            val json = Json.parseToJsonElement(responseText).jsonObject
            val msg = json["msg"]?.jsonPrimitive?.contentOrNull ?: "Unknown error"
            throw Exception("FeedbackEditingError: $msg (orderId=$orderId)")
        }

        if (status != 200) {
            throw Exception("RequestFailedError: HTTP $status")
        }

        val json = Json.parseToJsonElement(responseText).jsonObject
        return json["content"]?.jsonPrimitive?.content ?: ""
    }


    /**
     * Sends a message to a specific chat node.
     *
     * @param chatNode The identifier of the chat node.
     * @param content The content of the message to be sent.
     */
    suspend fun sendMessage(chatNode: String, content: String) {
        val requestJson = buildJsonObject {
            put("action", "chat_message")
            putJsonObject("data") {
                put("node", chatNode)
                put("last_message", "-1")
                put("content", content)
            }
        }

        val objectsArray = buildJsonArray {
            addJsonObject {
                put("type", "chat_node")
                put("id", chatNode)
                put("tag", "00000000")
                putJsonObject("data") {
                    put("node", chatNode)
                    put("last_message", "-1")
                    put("content", "")
                }
            }
        }


        GlobalSettings.isSendingMessage = true
        client.post(
            endpoint = "/runner/",
            headers = mapOf(
                HttpHeaders.Accept to "*/*",
                HttpHeaders.ContentType to "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Requested-With" to "XMLHttpRequest"
            ),
            cookies = mapOf(
                "golden_key" to account.goldenKey,
                "PHPSESSID" to account.phpSessionId
            ),
            body = mapOf(
                "objects" to objectsArray.toString(),
                "request" to requestJson.toString(),
                "csrf_token" to account.csrfToken,
            )
        )
        GlobalSettings.isSendingMessage = false
    }

    /**
     * Retrieves the chat node ID by username.
     *
     * This function scrapes the chat page to find a contact with the given username
     * and extracts the chat node ID from the contact's URL.
     *
     * @param username The username to search for.
     * @return The chat node ID if found, otherwise null.
     */
    suspend fun getChatNodeByUsername(username: String): String? {
        val html = client.get("/chat/", cookies = mapOf(
            "golden_key" to account.goldenKey,
            "PHPSESSID" to account.phpSessionId
        )).bodyAsText()
        val document = Jsoup.parse(html)
        val contacts = document.select("a.contact-item")

        for (contact in contacts) {
            val name = contact.selectFirst(".media-user-name")?.text()?.trim()
            if (name.equals(username, ignoreCase = true)) {
                val href = contact.attr("href") // Например, https://funpay.com/chat/?node=161986257
                val nodeId = Regex("""node=(\d+)""").find(href)?.groupValues?.get(1)
                return nodeId
            }
        }

        return null
    }

    /**
     * Retrieves the orders for the account.
     *
     * This function fetches the buyer and seller order counts from the Funpay API.
     *
     * @return An Orders object containing the buyer and seller counts.
     * @throws Exception if the request fails or the response is invalid.
     */
    suspend fun getOrders(): Orders {
        val cookies = mapOf("golden_key" to account.goldenKey)
        val jsonRequest = buildJsonArray {
            addJsonObject {
                put("type", JsonPrimitive("orders_counters"))
                put("id", JsonPrimitive(account.userId))
                put("tag", JsonPrimitive(""))
                put("data", JsonPrimitive(false))
            }
        }
        val response = client.post(
            endpoint = "/runner/",
            cookies = cookies,
            headers = mapOf(
                HttpHeaders.Accept to "*/*",
                HttpHeaders.ContentType to "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Requested-With" to "XMLHttpRequest"
            ),
            body = mapOf(
                "objects" to jsonRequest.toString(),
                "request" to "false"
            )
        )
        val result = response.bodyAsText().parse()
        val data = Json.parseToJsonElement(result).jsonObject["objects"]
            ?.jsonArray?.get(0)?.jsonObject?.get("data")?.jsonObject ?: return Orders(0, 0)
        val buyer = data["buyer"]?.jsonPrimitive?.int ?: 0
        val seller = data["seller"]?.jsonPrimitive?.int ?: 0
        return Orders(buyer, seller)
    }

    /**
     * Refunds a specific order.
     *
     * @param orderId The ID of the order to be refunded.
     */
    @ExperimentalStdlibApi
    suspend fun refundOrder(orderId: String) {
        client.post(
            endpoint = "/orders/refund",
            headers = mapOf(
                HttpHeaders.Accept to "*/*",
                HttpHeaders.ContentType to "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Requested-With" to "XMLHttpRequest"
            ),
            cookies = mapOf(
                "golden_key" to account.goldenKey,
                "PHPSESSID" to account.phpSessionId
            ),
            body = mapOf(
                "id" to orderId,
                "csrf_token" to account.csrfToken,
            )
        )
    }

    /**
     * Raises lots for a specific game and node.
     *
     * This function sends a request to raise the lots for a given game and node.
     *
     * @param gameId The ID of the game.
     * @param nodeId The ID of the node.
     * @return A RaiseResponse object indicating the success or failure of the operation.
     *         If successful, the message from the response is included.
     *         If unsuccessful, a default error message is provided.
     * @throws Exception if the request fails or the response is invalid.
     */
    suspend fun raiseLots(gameId: String, nodeId: String): RaiseResponse {
        val response = client.post(
            endpoint = "/lots/raise",
            cookies = mapOf("golden_key" to account.goldenKey),
            headers = mapOf(
                HttpHeaders.Accept to "application/json, text/javascript, */*; q=0.01",
                HttpHeaders.ContentType to "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Requested-With" to "XMLHttpRequest",
                HttpHeaders.Referrer to "https://funpay.com/lots/$nodeId/trade"
            ),
            body = mapOf(
                "game_id" to gameId,
                "node_id" to nodeId
            )
        )
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject

        return if ("msg" in json) {
            RaiseResponse(true, json["msg"]?.jsonPrimitive?.content ?: "None")
        } else {
            RaiseResponse(false, "Ошибка получения ответа от сервера")
        }
    }

    suspend fun loadGameNameToIdMap(): Map<String, String> {
        val html = client.get("/").bodyAsText()
        val document = Jsoup.parse(html)
        val items = document.select(".promo-game-item .game-title")

        return items.associate { element ->
            val name = element.text().trim()
            val gameId = element.attr("data-id")
            name to gameId
        }
    }

    /**
     * Parses the user profile and extracts available categories with active lots.
     *
     * @param userId The ID of the user to fetch categories from.
     * @return List of category pairs (name, nodeId) — gameId must be mapped manually.
     */
    suspend fun getAvailableCategories(userId: Long): List<CategoryInfo> {
        val html = client.get("/users/$userId/", cookies = mapOf(
            "golden_key" to account.goldenKey
        )).bodyAsText()

        val gameMap = loadGameNameToIdMap()

        val document = Jsoup.parse(html)
        val categoryElements = document.select(".offer-list-title h3 a")

        return categoryElements.mapNotNull { element ->
            val name = element.text().trim()
            val href = element.attr("href")
            val nodeId = Regex("/lots/(\\d+)").find(href)?.groupValues?.get(1)
            val gameId = gameMap.entries.find { name.contains(it.key, ignoreCase = true) }?.value

            if (gameId != null && nodeId != null) {
                CategoryInfo(
                    gameId = gameId,
                    nodeId = nodeId,
                    name = name
                )
            } else null
        }
    }
}