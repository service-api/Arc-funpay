package arc.funpay.http.impl

import arc.funpay.http.api.HeadersBuilder
import arc.funpay.http.config.HttpClientConfig
import io.ktor.http.*

class DefaultHeadersBuilder(
    val config: HttpClientConfig
) : HeadersBuilder {
    override fun buildHeaders(
        base: io.ktor.http.HeadersBuilder,
        headers: Map<String, String>,
        cookies: Map<String, String>
    ) {
        base.append(HttpHeaders.UserAgent, config.userAgent)
        headers.forEach { (key, value) -> base.append(key, value) }
        if (cookies.isNotEmpty()) {
            val cookieString = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            base.append(HttpHeaders.Cookie, cookieString)
        }
    }

}
