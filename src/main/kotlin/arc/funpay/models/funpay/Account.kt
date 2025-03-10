package arc.funpay.models.funpay

import arc.funpay.system.api.FunpayHttpClient
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup

/**
 * Data class, содержащий информацию об аккаунте, включая userId, goldenKey и PHPSESSID.
 *
 * @property userId Идентификатор пользователя.
 * @property goldenKey Золотой ключ аккаунта.
 * @property phpSessionId Значение куки PHPSESSID (может быть null, если не найден).
 */
@Serializable
data class Account(
    val userId: String,
    val goldenKey: String,
    val phpSessionId: String
) {
    fun isValid() = userId != "0" && goldenKey.isNotBlank() && phpSessionId != "0"

    companion object {
        suspend fun fromToken(goldKey: String, client: FunpayHttpClient): Account? {
            if (goldKey.isBlank()) return null

            val response: HttpResponse = client.get("/", cookies = mapOf("golden_key" to goldKey))
            val responseText = response.bodyAsText()
            val rawJson = Jsoup.parse(responseText).body().attr("data-app-data")

            val phpSessionId: String? = response.headers.getAll("Set-Cookie")?.firstNotNullOfOrNull { header ->
                val cookiePart = header.split(";").firstOrNull()
                val parts = cookiePart?.split("=")
                if (parts != null && parts.size == 2 && parts[0].trim() == "PHPSESSID") parts[1] else null
            }

            return try {
                val jsonElement = Json.parseToJsonElement(rawJson)
                if (jsonElement !is kotlinx.serialization.json.JsonObject) return null
                val id = jsonElement.jsonObject["userId"]?.jsonPrimitive?.contentOrNull ?: return null
                Account(id, goldKey, phpSessionId ?: "0")
            } catch (e: Exception) {
                null
            }
        }
    }
}