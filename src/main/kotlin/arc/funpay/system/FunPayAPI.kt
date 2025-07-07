package arc.funpay.system

import arc.funpay.domain.account.Account
import arc.funpay.domain.account.AccountInfo
import arc.funpay.domain.category.Category
import arc.funpay.domain.chat.ChatMessage
import arc.funpay.domain.common.Balance
import arc.funpay.domain.common.Currency
import arc.funpay.domain.response.RaiseResponse
import arc.funpay.ext.extract
import arc.funpay.http.api.HttpClient
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

class FunPayAPI(
    val client: HttpClient,
    val account: Account
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

    suspend fun getLastMessageInfo(chatNode: String): ChatMessage? {
        val html = client.get(
            "/chat/?node=$chatNode",
            cookies = mapOf(
                "golden_key" to account.goldenKey,
                "PHPSESSID" to account.phpSessionId
            )
        ).bodyAsText()

        val document = Jsoup.parse(html)
        val messagesList = document.select(".chat-message-list .chat-msg-item")

        val lastMessage = messagesList.lastOrNull() ?: return null

        val author = lastMessage.selectFirst(".chat-msg-author-link")?.text()?.trim() ?: return null
        val content = lastMessage.selectFirst(".chat-msg-text")?.text()?.trim() ?: return null

        return ChatMessage(author, content)
    }

    suspend fun sendMessage(chatNode: Long, content: String) {
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
                val href = contact.attr("href")
                val nodeId = Regex("""node=(\d+)""").find(href)?.groupValues?.get(1)
                return nodeId
            }
        }

        return null
    }

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