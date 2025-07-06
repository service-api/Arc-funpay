package arc.funpay.system

import arc.funpay.domain.account.Account
import arc.funpay.domain.account.AccountInfo
import arc.funpay.domain.common.Balance
import arc.funpay.domain.common.Currency
import arc.funpay.domain.common.RaiseResponse
import arc.funpay.ext.StringExtensions
import arc.funpay.http.api.HttpClient
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

class FunPayAPI(
    val client: HttpClient,
    val account: Account,
    val string: StringExtensions
) {
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


        val username = with(string) {
            html.extract("""<div class=\"user-link-name\">(.*?)<\/div>""")
        }


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

    suspend fun getOrdersCount(): Pair<Int, Int> {
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
        val jsonData = with(string) {
            val result = response.bodyAsText().parse()
            Json.parseToJsonElement(result).jsonObject["objects"]
                ?.jsonArray?.get(0)?.jsonObject?.get("data")?.jsonObject
        } ?: return Pair(0, 0)

        val buyer = jsonData["buyer"]?.jsonPrimitive?.int ?: 0
        val seller = jsonData["seller"]?.jsonPrimitive?.int ?: 0
        return Pair(buyer, seller)

    }

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