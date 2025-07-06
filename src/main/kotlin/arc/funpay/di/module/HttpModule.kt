package arc.funpay.di.module

import arc.funpay.di.api.AbstractModule
import arc.funpay.di.api.Binding
import arc.funpay.di.api.get
import arc.funpay.http.api.HeadersBuilder
import arc.funpay.http.api.HttpClient
import arc.funpay.http.api.ProxyConfigurator
import arc.funpay.http.config.HttpClientConfig
import arc.funpay.http.impl.DefaultHeadersBuilder
import arc.funpay.http.impl.DefaultProxyConfigurator
import arc.funpay.http.impl.FunpayKtorClient

class HttpModule : AbstractModule() {
    override fun bindings(): List<Binding<*>> = listOf(
        singleton<HttpClientConfig> {
            HttpClientConfig()
        },

        singleton<ProxyConfigurator> {
            DefaultProxyConfigurator()
        },

        singleton<HeadersBuilder> { container ->
            DefaultHeadersBuilder(container.get())
        },

        singleton<HttpClient> { container ->
            FunpayKtorClient(
                container.get(),
                container.get(),
                container.get()
            )
        }
    )
}