package arc.funpay.models.other

sealed class Proxy {
    data class HttpProxy(
        val url: String
    ) : Proxy()

    data class SocksProxy(
        val host: String,
        val port: Int
    ) : Proxy()
}