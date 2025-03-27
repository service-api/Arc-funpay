package arc.funpay.module.funpay

import arc.funpay.event.NewOrderCloseEvent
import arc.funpay.event.NewOrderEvent
import arc.funpay.event.NewOrderRefundEvent
import arc.funpay.model.funpay.Account
import arc.funpay.model.funpay.Order
import arc.funpay.model.funpay.OrderStatus
import arc.funpay.module.api.Module
import arc.funpay.system.api.FunpayHttpClient
import io.ktor.client.statement.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.koin.core.component.inject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class OrderStatusModule : Module() {
    val client by inject<FunpayHttpClient>()
    val account by inject<Account>()
    var firstRun = true
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
    val moscowZone = ZoneId.of("Europe/Moscow")
    var orders: List<Order> = emptyList()

    override suspend fun onTick() {
        val html = client.get("/orders/trade", cookies = mapOf("golden_key" to account.goldenKey)).bodyAsText()
        val doc = Jsoup.parse(html)
        val newOrders = parseOrders(doc)

        if (firstRun) {
            firstRun = false
        } else {
            newOrders.forEach { newOrder ->
                val oldOrder = orders.find { it.orderId == newOrder.orderId }
                when {
                    oldOrder == null && newOrder.status == OrderStatus.OPEN && newOrder.status != OrderStatus.REFUND ->
                        eventBus.post(NewOrderEvent(newOrder))
                    oldOrder != null && newOrder.status == OrderStatus.REFUND ->
                        eventBus.post(NewOrderRefundEvent(newOrder))
                    oldOrder != null && oldOrder.status != OrderStatus.CLOSED && newOrder.status == OrderStatus.CLOSED ->
                        eventBus.post(NewOrderCloseEvent(newOrder))
                }
            }
        }
        orders = newOrders
    }

    fun parseOrders(doc: Document): List<Order> {
        val today = LocalDate.now(moscowZone)
        return doc.select("a.tc-item").mapNotNull { el ->
            val dateText = el.selectFirst(".tc-date-time")?.text() ?: return@mapNotNull null
            val orderId = el.selectFirst(".tc-order")?.text()?.removePrefix("#") ?: return@mapNotNull null
            val description = el.selectFirst(".order-desc > div:nth-child(1)")?.text() ?: return@mapNotNull null
            val category = el.selectFirst(".order-desc > .text-muted")?.text() ?: ""
            val buyerEl = el.selectFirst(".media-user-name .pseudo-a") ?: return@mapNotNull null
            val buyer = buyerEl.text()
            val userId = Regex("""\d+""").find(buyerEl.attr("data-href"))?.value ?: return@mapNotNull null
            val status = el.selectFirst(".tc-status")?.text() ?: return@mapNotNull null
            val price = el.selectFirst(".tc-price")?.text()
                ?.replace("₽", "")?.replace(",", ".")?.trim()?.toDoubleOrNull() ?: return@mapNotNull null
            val date = parseDate(dateText, today) ?: return@mapNotNull null

            Order(date, orderId, description, category, buyer, userId, OrderStatus.from(status), price)
        }
    }

    fun parseDate(text: String, today: LocalDate): LocalDate? {
        val lower = text.lowercase()
        return when {
            "сегодня" in lower -> today
            "вчера" in lower -> today.minusDays(1)
            Regex("""\d+ (час|часа|часов) назад""").containsMatchIn(lower) -> today
            Regex("""\d+ (минут|минуты|минуту) назад""").containsMatchIn(lower) -> today
            Regex("""\d+ дней назад""").containsMatchIn(lower) ->
                Regex("""(\d+) дней назад""").find(lower)?.groupValues?.get(1)?.toLongOrNull()?.let { today.minusDays(it) }
            Regex("""\d+ дня назад""").containsMatchIn(lower) ->
                Regex("""(\d+) дня назад""").find(lower)?.groupValues?.get(1)?.toLongOrNull()?.let { today.minusDays(it) }
            "неделю назад" in lower -> today.minusWeeks(1)
            Regex("""\d+ недель назад""").containsMatchIn(lower) ->
                Regex("""(\d+) недель назад""").find(lower)?.groupValues?.get(1)?.toLongOrNull()?.let { today.minusWeeks(it) }
            Regex("""\d+ месяцев назад""").containsMatchIn(lower) ->
                Regex("""(\d+) месяцев назад""").find(lower)?.groupValues?.get(1)?.toLongOrNull()?.let { today.minusMonths(it) }
            "месяц назад" in lower -> today.minusMonths(1)
            else -> {
                val datePart = text.substringBefore(",").trim()
                val year = if (!datePart.contains(Regex("""\d{4}"""))) " ${today.year}" else ""
                runCatching { LocalDate.parse(datePart + year, formatter) }.getOrNull()
            }
        }
    }

    val today get() = LocalDate.now(moscowZone)
    val monday get() = today.with(DayOfWeek.MONDAY)
    val firstDayOfMonth get() = today.withDayOfMonth(1)
    val firstDayOfYear get() = today.withDayOfYear(1)

    fun getOpenedOrders() = orders.filter { it.status == OrderStatus.OPEN }
    fun getAllOrders() = orders
    fun getRefundOrders() = orders.filter { it.status == OrderStatus.REFUND }

    fun getOrdersToday() = orders.filter {
        it.date == today && it.status != OrderStatus.REFUND
    }

    fun getOrdersThisWeek() = orders.filter {
        it.date >= monday && it.status != OrderStatus.REFUND
    }

    fun getOrdersThisMonth() = orders.filter {
        it.date >= firstDayOfMonth && it.status != OrderStatus.REFUND
    }

    fun getOrdersThisYear() = orders.filter {
        it.date >= firstDayOfYear && it.status != OrderStatus.REFUND
    }

    fun getAllOrdersEver() = orders.filter {
        it.status != OrderStatus.REFUND
    }

    fun getRefundsToday() = orders.filter {
        it.date == today && it.status == OrderStatus.REFUND
    }

    fun getRefundsThisWeek() = orders.filter {
        it.date >= monday && it.status == OrderStatus.REFUND
    }

    fun getRefundsThisMonth() = orders.filter {
        it.date >= firstDayOfMonth && it.status == OrderStatus.REFUND
    }

    fun getRefundsThisYear() = orders.filter {
        it.date >= firstDayOfYear && it.status == OrderStatus.REFUND
    }

    fun getAllRefundsEver() = orders.filter {
        it.status == OrderStatus.REFUND
    }
}
