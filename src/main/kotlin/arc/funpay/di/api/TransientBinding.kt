package arc.funpay.di.api

data class TransientBinding<T : Any>(
    override val type: Class<T>,
    val factory: (DependencyContainer) -> T
) : Binding<T> {
    override fun create(container: DependencyContainer): T = factory(container)
    override val isSingleton: Boolean = false
}
