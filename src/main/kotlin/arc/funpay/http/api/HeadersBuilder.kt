package arc.funpay.http.api

import io.ktor.http.HeadersBuilder

interface HeadersBuilder {
    fun buildHeaders(
        base: HeadersBuilder,
        headers: Map<String, String>,
        cookies: Map<String, String>
    )
}
