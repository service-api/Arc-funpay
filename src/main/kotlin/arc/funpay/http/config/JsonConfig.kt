package arc.funpay.http.config

data class JsonConfig(
    val prettyPrint: Boolean = true,
    val isLenient: Boolean = true,
    val ignoreUnknownKeys: Boolean = true
)
