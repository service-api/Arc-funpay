package arc.funpay.di.module

import arc.funpay.core.LotsRaiseModule
import arc.funpay.di.api.AbstractModule
import arc.funpay.di.api.Binding

class FunPayModule : AbstractModule() {
    override fun bindings(): List<Binding<*>> = listOf(
        singleton<LotsRaiseModule> { container ->
            LotsRaiseModule(container)
        },
    )
}