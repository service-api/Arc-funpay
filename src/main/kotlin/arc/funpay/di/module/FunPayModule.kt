package arc.funpay.di.module

import arc.funpay.core.ChatMonitoringModule
import arc.funpay.core.LotsRaiseModule
import arc.funpay.core.api.Module
import arc.funpay.di.api.ServiceModule
import org.koin.dsl.bind
import org.koin.dsl.module

class FunPayModule : ServiceModule {
    override fun createModule() = module {
        single { ChatMonitoringModule() } bind Module::class
        single { LotsRaiseModule() } bind Module::class
    }
}