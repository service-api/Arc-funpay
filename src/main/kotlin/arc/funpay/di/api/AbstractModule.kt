
package arc.funpay.di.api

abstract class AbstractModule : Module {
    protected inline fun <reified T : Any> singleton(
        noinline factory: (DependencyContainer) -> T
    ): Binding<T> = SingletonBinding(T::class.java, factory)

    protected inline fun <reified T : Any> transient(
        noinline factory: (DependencyContainer) -> T
    ): Binding<T> = TransientBinding(T::class.java, factory)
}