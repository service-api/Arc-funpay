package arc.funpay.domain.account

import arc.funpay.http.api.HttpClient
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

@Serializable
data class Account(
    val userId: Long,
    val goldenKey: String,
    val phpSessionId: String,
    val csrfToken: String,
    val username: String = ""
) {
    fun isValid() = userId != 0L && goldenKey.isNotBlank() && phpSessionId != "0"

    companion object {
        suspend fun fromToken(goldKey: String, client: HttpClient): Account? {
            if (goldKey.isBlank()) return null

            val response: HttpResponse = client.get("/", cookies = mapOf("golden_key" to goldKey))
            val responseText = response.bodyAsText()

            val document = Jsoup.parse(responseText)
            val rawJsonEncoded = document.body().attr("data-app-data")
            val rawJson = Parser.unescapeEntities(rawJsonEncoded, false)

            val usernameElement = document.selectFirst("div.user-link-name")
            val username = usernameElement?.text() ?: ""

            val phpSessionId: String? = response.headers.getAll("Set-Cookie")?.firstNotNullOfOrNull { header ->
                val cookiePart = header.split(";").firstOrNull()
                val parts = cookiePart?.split("=")
                if (parts != null && parts.size == 2 && parts[0].trim() == "PHPSESSID") parts[1] else null
            }

            return try {
                val jsonElement = Json.parseToJsonElement(rawJson)
                if (jsonElement !is JsonObject) return null

                val id = jsonElement.jsonObject["userId"]?.jsonPrimitive?.longOrNull ?: return null
                val csrfToken = jsonElement.jsonObject["csrf-token"]?.jsonPrimitive?.content ?: ""

                Account(id, goldKey, phpSessionId ?: "0", csrfToken, username)
            } catch (_: Exception) {
                null
            }
        }
    }
}