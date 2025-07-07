package arc.funpay.di.module

import arc.funpay.common.api.JsonProcessor
import arc.funpay.common.api.TextParser
import arc.funpay.common.api.TimeProvider
import arc.funpay.common.api.TimingParser
import arc.funpay.common.impl.DefaultTimingParser
import arc.funpay.common.impl.KotlinxJsonProcessor
import arc.funpay.common.impl.RegexTextParser
import arc.funpay.common.impl.SystemTimeProvider
import arc.funpay.di.api.ServiceModule
import org.koin.dsl.module

class CoreServicesModule : ServiceModule {
    override fun createModule() = module {
        single<TimeProvider> { SystemTimeProvider() }
        single<TextParser> { RegexTextParser() }
        single<TimingParser> { DefaultTimingParser(get()) }
        single<JsonProcessor> { KotlinxJsonProcessor() }
    }
}