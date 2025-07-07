package arc.funpay.http.impl

import arc.funpay.http.api.HeadersBuilder
import arc.funpay.http.api.HttpClient
import arc.funpay.http.api.ProxyConfigurator
import arc.funpay.http.config.HttpClientConfig
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class FunpayKtorClient(
    val config: HttpClientConfig,
    val proxyConfigurator: ProxyConfigurator,
    val headersBuilder: HeadersBuilder
) : HttpClient {
    val client = io.ktor.client.HttpClient(OkHttp) {
        proxyConfigurator.configure(this)

        install(ContentNegotiation) {
            json(Json {
                prettyPrint = config.jsonConfig.prettyPrint
                isLenient = config.jsonConfig.isLenient
                ignoreUnknownKeys = config.jsonConfig.ignoreUnknownKeys
            })
        }
    }

    override suspend fun get(
        endpoint: String,
        headers: Map<String, String>,
        cookies: Map<String, String>
    ): HttpResponse {
        return client.get("${config.baseUrl}$endpoint") {
            headers {
                headersBuilder.buildHeaders(this, headers, cookies)
            }
        }
    }

    override suspend fun post(
        endpoint: String,
        headers: Map<String, String>,
        cookies: Map<String, String>,
        body: Map<String, String>
    ): HttpResponse {
        return client.post("${config.baseUrl}$endpoint") {
            headers {
                headersBuilder.buildHeaders(this, headers, cookies)
            }
            setBody(FormDataContent(Parameters.build {
                body.forEach { (key, value) -> append(key, value) }
            }))
        }
    }
}
