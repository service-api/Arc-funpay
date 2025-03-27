package arc.funpay.module.funpay

import arc.funpay.event.NewOrderEvent
import arc.funpay.event.OrderCloseEvent
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
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Module for managing and parsing order statuses from Funpay.
 *
 * This module is responsible for fetching order data, parsing it,
 * and triggering events based on order status changes.
 */
class OrderStatusModule : Module() {
    val client by inject<FunpayHttpClient>()
    val account by inject<Account>()

    var firstRun = true
    val fullDateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
    var orders: List<Order> = emptyList()

    /**
     * Fetches order data from Funpay and processes it.
     *
     * This function retrieves the order list, parses it, and then checks for
     * new orders or changes in order status, posting corresponding events.
     */
    override suspend fun onTick() {
        val html = client.get("/orders/trade", cookies = mapOf("golden_key" to account.goldenKey)).bodyAsText()
        val doc = Jsoup.parse(html)
        val newOrders = parseOrders(doc)

        if (!firstRun) {
            newOrders.forEach { newOrder ->
                val oldOrder = orders.find { it.orderId == newOrder.orderId }

                if (oldOrder == null && newOrder.status == OrderStatus.OPEN) {
                    eventBus.post(NewOrderEvent(newOrder))
                }

                if (oldOrder != null && oldOrder.status != OrderStatus.CLOSED && newOrder.status == OrderStatus.CLOSED) {
                    eventBus.post(OrderCloseEvent(newOrder))
                }
            }
        } else {
            firstRun = false
        }

        orders = newOrders
    }

    /**
     * Parses the HTML document to extract order information.
     *
     * @param doc The Jsoup Document object to parse.
     * @return A list of [Order] objects extracted from the HTML.
     */
    fun parseOrders(doc: Document): List<Order> {
        val now = LocalDate.now()
        return doc.select("a.tc-item").mapNotNull { el ->
            val dateText = el.selectFirst(".tc-date-time")?.text() ?: return@mapNotNull null
            val orderId = el.selectFirst(".tc-order")?.text()?.removePrefix("#") ?: return@mapNotNull null
            val description = el.selectFirst(".order-desc > div:nth-child(1)")?.text() ?: return@mapNotNull null
            val category = el.selectFirst(".order-desc > .text-muted")?.text() ?: ""
            val buyerEl = el.selectFirst(".media-user-name .pseudo-a") ?: return@mapNotNull null
            val buyer = buyerEl.text()
            val href = buyerEl.attr("data-href")

            val userId = Regex("""\d+""").find(href)?.value ?: return@mapNotNull null

            val status = el.selectFirst(".tc-status")?.text() ?: return@mapNotNull null
            val priceText = el.selectFirst(".tc-price")?.text() ?: return@mapNotNull null
            val price = priceText.replace("₽", "").replace(",", ".").trim().toDoubleOrNull() ?: return@mapNotNull null
            val date = parseDate(dateText, now) ?: return@mapNotNull null

            Order(date, orderId, description, category, buyer, userId, OrderStatus.from(status), price)
        }
    }

    /**
     * Parses a date string into a LocalDate object.
     *
     * @param text The date string to parse.
     * @param now The current LocalDate, used as a reference for relative dates.
     * @return The parsed LocalDate, or null if parsing fails.
     */
    fun parseDate(text: String, now: LocalDate): LocalDate? {
        val lower = text.lowercase()

        return when {
            "сегодня" in lower -> now
            "вчера" in lower -> now.minusDays(1)
            Regex("""\d+ (час|часа|часов) назад""") in lower -> now
            Regex("""\d+ (минут|минуты|минуту) назад""") in lower -> now
            Regex("""\d+ дней назад""") in lower -> {
                val days = Regex("""(\d+) дней назад""").find(lower)?.groupValues?.get(1)?.toLongOrNull()
                days?.let { now.minusDays(it) }
            }
            Regex("""\d+ дня назад""") in lower -> {
                val days = Regex("""(\d+) дня назад""").find(lower)?.groupValues?.get(1)?.toLongOrNull()
                days?.let { now.minusDays(it) }
            }
            "неделю назад" in lower -> now.minusWeeks(1)
            Regex("""\d+ недель назад""") in lower -> {
                val weeks = Regex("""(\d+) недель назад""").find(lower)?.groupValues?.get(1)?.toLongOrNull()
                weeks?.let { now.minusWeeks(it) }
            }
            Regex("""\d+ месяцев назад""") in lower -> {
                val months = Regex("""(\d+) месяцев назад""").find(lower)?.groupValues?.get(1)?.toLongOrNull()
                months?.let { now.minusMonths(it) }
            }
            "месяц наз��д" in lower -> now.minusMonths(1)
            else -> {
                val datePart = text.substringBefore(",").trim()
                val formatter = fullDateTimeFormatter
                val year = if (datePart.contains("\\d{4}".toRegex())) "" else " ${now.year}"
                return runCatching { LocalDate.parse(datePart + year, formatter) }.getOrNull()
            }
        }
    }

    /**
     * Retrieves a list of currently open orders.
     *
     * @return A list of [Order] objects with status OPEN.
     */
    fun getOpenedOrders(): List<Order> = orders.filter { it.status == OrderStatus.OPEN }

    /**
     * Retrieves all orders.
     *
     * @return A list of all [Order] objects.
     */
    fun getAllOrders(): List<Order> = orders

    /**
     * Retrieves a list of refunded orders.
     *
     * @return A list of [Order] objects with status REFUND.
     */
    fun getRefundOrders(): List<Order> = orders.filter { it.status == OrderStatus.REFUND }

    /**
     * Retrieves a list of orders placed today, excluding refunds.
     *
     * @return A list of [Order] objects placed today with status not REFUND.
     */
    fun getOrdersToday(): List<Order> =
        orders.filter { it.date == LocalDate.now().minusDays(1) && it.status != OrderStatus.REFUND }

    /**
     * Retrieves a list of orders placed this week, excluding refunds.
     *
     * @return A list of [Order] objects placed this week with status not REFUND.
     */
    fun getOrdersThisWeek(): List<Order> {
        val startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY)
        return orders.filter { it.date >= startOfWeek && it.status != OrderStatus.REFUND }
    }

    /**
     * Retrieves a list of orders placed this month, excluding refunds.
     *
     * @return A list of [Order] objects placed this month with status not REFUND.
     */
    fun getOrdersThisMonth(): List<Order> {
        val startOfMonth = LocalDate.now().withDayOfMonth(1)
        return orders.filter { it.date >= startOfMonth && it.status != OrderStatus.REFUND }
    }

    /**
     * Retrieves a list of orders placed this year, excluding refunds.
     *
     * @return A list of [Order] objects placed this year with status not REFUND.
     */
    fun getOrdersThisYear(): List<Order> {
        val startOfYear = LocalDate.now().withDayOfYear(1)
        return orders.filter { it.date >= startOfYear && it.status != OrderStatus.REFUND }
    }

    /**
     * Retrieves all orders ever placed, excluding refunds.
     *
     * @return A list of all [Order] objects ever placed with status not REFUND.
     */
    fun getAllOrdersEver(): List<Order> =
        orders.filter { it.status != OrderStatus.REFUND }

    /**
     * Retrieves a list of refunds processed today.
     *
     * @return A list of [Order] objects refunded today.
     */
    fun getRefundsToday(): List<Order> =
        orders.filter { it.date == LocalDate.now().minusDays(1) && it.status == OrderStatus.REFUND }

    /**
     * Retrieves a list of refunds processed this week.
     *
     * @return A list of [Order] objects refunded this week.
     */
    fun getRefundsThisWeek(): List<Order> {
        val startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY)
        return orders.filter { it.date >= startOfWeek && it.status == OrderStatus.REFUND }
    }

    /**
     * Retrieves a list of refunds processed this month.
     *
     * @return A list of [Order] objects refunded this month.
     */
    fun getRefundsThisMonth(): List<Order> {
        val startOfMonth = LocalDate.now().withDayOfMonth(1)
        return orders.filter { it.date >= startOfMonth && it.status == OrderStatus.REFUND }
    }

    /**
     * Retrieves a list of refunds processed this year.
     *
     * @return A list of [Order] objects refunded this year.
     */
    fun getRefundsThisYear(): List<Order> {
        val startOfYear = LocalDate.now().withDayOfYear(1)
        return orders.filter { it.date >= startOfYear && it.status == OrderStatus.REFUND }
    }

    /**
     * Retrieves all refunds ever processed.
     *
     * @return A list of all [Order] objects ever refunded.
     */
    fun getAllRefundsEver(): List<Order> =
        orders.filter { it.status == OrderStatus.REFUND }
}