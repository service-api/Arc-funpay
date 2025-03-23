package arc.funpay.system

import arc.funpay.ext.extract
import arc.funpay.ext.parse
import arc.funpay.model.funpay.Account
import arc.funpay.model.funpay.AccountInfo
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
        } catch (e: Exception) {
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
}