package arc.funpay.di.module

import arc.funpay.di.api.ServiceModule
import arc.funpay.http.api.HeadersBuilder
import arc.funpay.http.api.HttpClient
import arc.funpay.http.api.ProxyConfigurator
import arc.funpay.http.config.HttpClientConfig
import arc.funpay.http.impl.DefaultHeadersBuilder
import arc.funpay.http.impl.DefaultProxyConfigurator
import arc.funpay.http.impl.FunpayKtorClient
import org.koin.dsl.module

class HttpModule : ServiceModule {
    override fun createModule() = module {
        single { HttpClientConfig() }
        single<ProxyConfigurator> { DefaultProxyConfigurator() }
        single<HeadersBuilder> { DefaultHeadersBuilder(get()) }
        single<HttpClient> { FunpayKtorClient(get(), get(), get()) }
    }
}