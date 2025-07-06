package arc.funpay.di.impl

import arc.funpay.di.api.Binding
import arc.funpay.di.api.DependencyContainer
import arc.funpay.di.api.Module

class KoinContainer : DependencyContainer {
    val singletons = mutableMapOf<Class<*>, Any>()
    val bindings = mutableMapOf<Class<*>, Binding<*>>()

    override fun <T : Any> get(type: Class<T>): T {
        val binding = bindings[type] ?: throw IllegalStateException("No binding for ${type.name}")

        @Suppress("UNCHECKED_CAST")
        return when {
            binding.isSingleton -> {
                singletons.getOrPut(type) { binding.create(this) } as T
            }
            else -> binding.create(this) as T
        }
    }

    override fun loadModules(vararg modules: Module) {
        modules.forEach { module ->
            module.bindings().forEach { binding ->
                bindings[binding.type] = binding
            }
        }
    }

    override fun close() {
        singletons.clear()
        bindings.clear()
    }
}
