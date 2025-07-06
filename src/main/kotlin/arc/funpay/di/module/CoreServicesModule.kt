package arc.funpay.di.module

import arc.funpay.common.api.JsonProcessor
import arc.funpay.common.api.TextParser
import arc.funpay.common.api.TimeProvider
import arc.funpay.common.api.TimingParser
import arc.funpay.common.impl.DefaultTimingParser
import arc.funpay.common.impl.KotlinxJsonProcessor
import arc.funpay.common.impl.RegexTextParser
import arc.funpay.common.impl.SystemTimeProvider
import arc.funpay.di.api.AbstractModule
import arc.funpay.di.api.Binding
import arc.funpay.di.api.get
import arc.funpay.ext.StringExtensions

class CoreServicesModule : AbstractModule() {
    override fun bindings(): List<Binding<*>> = listOf(
        singleton<TimeProvider> {
            SystemTimeProvider()
        },

        singleton<TextParser> {
            RegexTextParser()
        },

        singleton<TimingParser> { container ->
            DefaultTimingParser(container.get())
        },

        singleton<JsonProcessor> {
            KotlinxJsonProcessor()
        },

        // Extensions
        singleton { container ->
            StringExtensions(container)
        }
    )
}