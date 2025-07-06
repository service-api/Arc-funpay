package arc.funpay.http.api

import io.ktor.client.*

interface ProxyConfigurator {
    fun configure(builder: HttpClientConfig<*>)
}
