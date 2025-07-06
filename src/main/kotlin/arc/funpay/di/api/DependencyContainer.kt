package arc.funpay.di.api

interface DependencyContainer {
    fun <T : Any> get(type: Class<T>): T
    fun loadModules(vararg modules: Module)
    fun close()
}

inline fun <reified T : Any> DependencyContainer.get(): T = get(T::class.java)
