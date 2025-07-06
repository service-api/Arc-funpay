package arc.funpay.di.api

interface Binding<T : Any> {
    val type: Class<T>
    fun create(container: DependencyContainer): T
    val isSingleton: Boolean
}
