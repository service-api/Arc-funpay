package arc.funpay.system

import arc.funpay.ext.extract
import arc.funpay.ext.parse
import arc.funpay.models.funpay.Account
import arc.funpay.models.funpay.AccountInfo
import arc.funpay.models.other.Balance
import arc.funpay.models.other.Currency
import arc.funpay.models.other.Orders
import arc.funpay.models.response.RaiseResponse
import arc.funpay.system.api.FunpayHttpClient
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

/**
 * Class representing the Funpay API.
 *
 * @property client The HTTP client used for making requests.
 * @property account The account associated with the API.
 */
class FunpayAPI(
    val client: FunpayHttpClient,
    val account: Account,
) {
    /**
     * Retrieves account information.
     *
     * @return An AccountInfo object containing user ID, username, and balance.
     */
    suspend fun getInfo(): AccountInfo = runBlocking {
        val html = client.get().bodyAsText()

        val username = html.extract("""<div class="user-link-name">(.*?)</div>""")
        val balanceText = html.extract("""<span class="badge badge-balance">(.*?)</span>""")

        val (balance, currency) = Regex("""(\d+)\s*([^\d\s]+)""").find(balanceText)
            ?.destructured?.let { (b, c) -> b.toIntOrNull() to Currency.fromString(c) }
            ?: (0 to Currency.RUB)

        val userId = Json.parseToJsonElement(html.extract("""<body data-app-data='(.*?)'""", "{}"))
            .jsonObject["userId"]?.jsonPrimitive?.content ?: ""

        AccountInfo(userId, username, Balance(balance ?: 0, currency))
    }

    /**
     * Retrieves the orders for the account.
     *
     * @return An Orders object containing the buyer and seller counts.
     * @throws Exception if the request fails or the response is invalid.
     */
    suspend fun getOrders(): Orders {
        val cookies = mapOf("golden_key" to account.goldenKey)
        val jsonRequest = buildJsonArray {
            addJsonObject {
                put("type", JsonPrimitive("orders_counters"))
                put("id", JsonPrimitive(9479684))
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
     * Raises lots for a specific game and node.
     *
     * @param gameId The ID of the game.
     * @param nodeId The ID of the node.
     * @return A RaiseResponse object indicating the success or failure of the operation.
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