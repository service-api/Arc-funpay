package arc.funpay.http.config

data class HttpClientConfig(
    val baseUrl: String = "https://funpay.com",
    val userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    val jsonConfig: JsonConfig = JsonConfig()
)
