
import arc.funpay.event.api.Cancelable
import arc.funpay.event.api.EventBus
import arc.funpay.event.api.FunpayEvent
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class EventBussTest {
    data class PreTestEvent(override var isCancelled: Boolean = false) : FunpayEvent, Cancelable
    data class TestEvent(
        val iq: Int = 0
    ): FunpayEvent


    @Test
    fun testEvent() {
        val eventBus = EventBus()

        var isReceived = false

        runBlocking {
            eventBus.on<TestEvent> {
                isReceived = true
            }

            eventBus.post(TestEvent())
        }

        assertTrue(isReceived)
    }


    @Test
    fun testPreEvent() {
        val eventBus = EventBus()

        val pre = PreTestEvent()
        runBlocking {
            eventBus.on<PreTestEvent> {
                it.isCancelled = true
            }

            eventBus.post(pre)
            eventBus.post(TestEvent())
        }
        assertTrue(pre.isCancelled)

    }

}