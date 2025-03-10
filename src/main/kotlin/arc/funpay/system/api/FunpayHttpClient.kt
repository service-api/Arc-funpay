package arc.funpay.system.api

import arc.funpay.models.other.Proxy
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * A client for making HTTP requests to the Funpay API.
 */
class FunpayHttpClient(
    val proxyData: Proxy? = null
) {
    /**
     * The HTTP client instance configured with OkHttp engine and JSON content negotiation.
     */
    val client = HttpClient(OkHttp) {
        proxyData?.let {
            engine {
                proxy = when (it) {
                    is Proxy.HttpProxy -> ProxyBuilder.http(Url(it.url))
                    is Proxy.SocksProxy -> ProxyBuilder.socks(it.host, it.port)
                }
            }
        }
        install(ContentNegotiation) {
            json(Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true })
        }
    }

    /**
     * The base URL for the Funpay API.
     */
    val baseUrl = "https://funpay.com"

    /**
     * Makes a GET request to the specified endpoint.
     *
     * @param endpoint The API endpoint to send the request to.
     * @param headers The headers to include in the request.
     * @param cookies The cookies to include in the request.
     * @return The HTTP response.
     */
    suspend fun get(
        endpoint: String = "/",
        headers: Map<String, String> = mapOf(),
        cookies: Map<String, String> = mapOf()
    ): HttpResponse {
        return client.get("$baseUrl$endpoint") {
            headers {
                append(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                headers.forEach { (key, value) -> append(key, value) }
                if (cookies.isNotEmpty()) {
                    val cookieString = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
                    append(HttpHeaders.Cookie, cookieString)
                }
            }
        }
    }

    /**
     * Makes a POST request to the specified endpoint.
     *
     * @param endpoint The API endpoint to send the request to.
     * @param headers The headers to include in the request.
     * @param cookies The cookies to include in the request.
     * @param body The form data to include in the request body.
     * @return The HTTP response.
     */
    suspend fun post(
        endpoint: String = "/",
        headers: Map<String, String> = mapOf(),
        cookies: Map<String, String> = mapOf(),
        body: Map<String, String> = mapOf()
    ): HttpResponse {
        return client.post("$baseUrl$endpoint") {
            headers {
                append(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                if (cookies.isNotEmpty()) {
                    val cookieString = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
                    append(HttpHeaders.Cookie, cookieString)
                }
                headers.forEach { (key, value) -> append(key, value) }
            }

            setBody(FormDataContent(Parameters.build {
                body.forEach { (key, value) -> append(key, value) }
            }))
        }
    }
}