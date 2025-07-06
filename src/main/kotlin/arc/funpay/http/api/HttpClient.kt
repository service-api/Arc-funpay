package arc.funpay.http.api

import io.ktor.client.statement.*

interface HttpClient {
    suspend fun get(
        endpoint: String,
        headers: Map<String, String> = mapOf(),
        cookies: Map<String, String> = mapOf()
    ): HttpResponse

    suspend fun post(
        endpoint: String,
        headers: Map<String, String> = mapOf(),
        cookies: Map<String, String> = mapOf(),
        body: Map<String, String> = mapOf()
    ): HttpResponse
}
