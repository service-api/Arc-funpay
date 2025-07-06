package arc.funpay.di.api

interface Module {
    fun bindings(): List<Binding<*>>
}
