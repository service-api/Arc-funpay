package arc.funpay.module.funpay

import arc.funpay.event.NewOrderEvent
import arc.funpay.event.NewPurchaseEvent
import arc.funpay.event.api.FunpayEvent
import arc.funpay.model.other.Orders
import arc.funpay.module.api.Module

/**
 * Module responsible for handling order events.
 */
class OrderEventModule : Module() {
    /**
     * Indicates if this is the first tick.
     */
    var isFirst = false

    /**
     * Stores the last known orders.
     */
    var lastOrders: Orders? = null

    /**
     * Called periodically to fetch and post new events.
     */
    override suspend fun onTick() {
        val events = fetchEvents()

        events.forEach { event ->
            eventBus.post(event)
        }
    }

    /**
     * Fetches new events based on the current and last known orders.
     *
     * @return A list of FunpayEvent objects representing new events.
     */
    suspend fun fetchEvents(): List<FunpayEvent> {
        val currentOrders = api.getOrders()
        val events = mutableListOf<FunpayEvent>()

        if (!isFirst) {
            isFirst = true
            lastOrders = currentOrders
            return events
        }
        lastOrders?.seller?.let {
            if (it < currentOrders.seller)
                events.add(NewOrderEvent(lastOrders?.seller ?: 0, currentOrders.seller))
        }
        lastOrders?.buyer?.let {
            if (it < currentOrders.buyer)
                events.add(NewPurchaseEvent(lastOrders?.buyer ?: 0, currentOrders.buyer))
        }

        lastOrders = currentOrders
        return events
    }
}