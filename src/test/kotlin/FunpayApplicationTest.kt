import arc.funpay.FunpayApplication
import arc.funpay.modules.api.Module
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.koin.core.context.stopKoin
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FunpayApplicationTest {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun validTokenProvided() = runBlocking {
        val app = FunpayApplication(System.getenv("TOKEN") ?: "")
        app.start()

        assertTrue(app.isRunnable)
    }

    @Test
    fun invalidTokenProvided() = runBlocking {
        val app = FunpayApplication("invalid")
        app.start()

        assertFalse(app.isRunnable)
    }

    @Test
    fun addModule() = runBlocking {
        val app = FunpayApplication(System.getenv("TOKEN") ?: "")
        app.addModule(object : Module() {

            override suspend fun onStart() {
                println("Module started")
            }
        })
        app.start()

        assertTrue(app.isRunnable)
    }

    @Test
    fun addRuntimeModule() = runBlocking {
        val app = FunpayApplication(System.getenv("TOKEN") ?: "")
        app.start()

        app.startModule(object : Module() {
            override suspend fun onStart() {
                println("Module started")
            }
        })

        assertTrue(app.isRunnable)
    }


    @Test
    fun removeModule() = runBlocking {
        val app = FunpayApplication(System.getenv("TOKEN") ?: "")
        val module = object : Module() {

            override suspend fun onStart() {
                println("Module started")
            }

            override suspend fun onStop() {
                println("Module stopped")
            }
        }

        app.addModule(module)
        app.modules.forEach {
            println(it)
        }
        app.removeModule(module)
        app.modules.forEach {
            println(it)
        }
        app.start()
        assertFalse(app.modules.contains(module))
    }

    @Test
    fun removeRuntimeModule() = runBlocking {
        val app = FunpayApplication(System.getenv("TOKEN") ?: "")
        val module = object : Module() {

            override suspend fun onStart() {
                println("Module started")
            }

            override suspend fun onStop() {
                println("Module stopped")
            }
        }

        app.start()
        app.startModule(module)
        app.modules.forEach {
            println(it)
        }
        app.stopModule(module)
        app.modules.forEach {
            println(it)
        }

        assertFalse(module.isRunning)
    }
}
