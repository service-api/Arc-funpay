package arc.funpay.module.funpay

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

class OrderStatusModule : Module() {
    val client by inject<FunpayHttpClient>()
    val account by inject<Account>()

    val fullDateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
    var orders: List<Order> = emptyList()

    override suspend fun onTick() {
        val html = client.get("/orders/trade", cookies = mapOf("golden_key" to account.goldenKey)).bodyAsText()
        val doc = Jsoup.parse(html)
        orders = parseOrders(doc)
    }

    fun parseOrders(doc: Document): List<Order> {
        val now = LocalDate.now()
        return doc.select("a.tc-item").mapNotNull { el ->
            val dateText = el.selectFirst(".tc-date-time")?.text() ?: return@mapNotNull null
            val orderId = el.selectFirst(".tc-order")?.text()?.removePrefix("#") ?: return@mapNotNull null
            val description = el.selectFirst(".order-desc > div:nth-child(1)")?.text() ?: return@mapNotNull null
            val category = el.selectFirst(".order-desc > .text-muted")?.text() ?: ""
            val buyer = el.selectFirst(".media-user-name span")?.text() ?: return@mapNotNull null
            val status = el.selectFirst(".tc-status")?.text() ?: return@mapNotNull null
            val priceText = el.selectFirst(".tc-price")?.text() ?: return@mapNotNull null
            val price = priceText.replace("₽", "").replace(",", ".").trim().toDoubleOrNull() ?: return@mapNotNull null
            val date = parseDate(dateText, now) ?: return@mapNotNull null

            Order(date, orderId, description, category, buyer, OrderStatus.from(status), price)
        }
    }

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
            "месяц назад" in lower -> now.minusMonths(1)
            else -> {
                val datePart = text.substringBefore(",").trim()
                val formatter = fullDateTimeFormatter
                val year = if (datePart.contains("\\d{4}".toRegex())) "" else " ${now.year}"
                return runCatching { LocalDate.parse(datePart + year, formatter) }.getOrNull()
            }
        }
    }

    fun getOpenedOrders(): List<Order> = orders.filter { it.status == OrderStatus.OPEN }

    fun getAllOrders(): List<Order> = orders

    fun getRefundOrders(): List<Order> = orders.filter { it.status == OrderStatus.REFUND }

    fun getOrdersToday(): List<Order> =
        orders.filter { it.date == LocalDate.now().minusDays(1) && it.status != OrderStatus.REFUND }

    fun getOrdersThisWeek(): List<Order> {
        val startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY)
        return orders.filter { it.date >= startOfWeek && it.status != OrderStatus.REFUND }
    }

    fun getOrdersThisMonth(): List<Order> {
        val startOfMonth = LocalDate.now().withDayOfMonth(1)
        return orders.filter { it.date >= startOfMonth && it.status != OrderStatus.REFUND }
    }

    fun getOrdersThisYear(): List<Order> {
        val startOfYear = LocalDate.now().withDayOfYear(1)
        return orders.filter { it.date >= startOfYear && it.status != OrderStatus.REFUND }
    }

    fun getAllOrdersEver(): List<Order> =
        orders.filter { it.status != OrderStatus.REFUND }

    fun getRefundsToday(): List<Order> =
        orders.filter { it.date == LocalDate.now().minusDays(1) && it.status == OrderStatus.REFUND }

    fun getRefundsThisWeek(): List<Order> {
        val startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY)
        return orders.filter { it.date >= startOfWeek && it.status == OrderStatus.REFUND }
    }

    fun getRefundsThisMonth(): List<Order> {
        val startOfMonth = LocalDate.now().withDayOfMonth(1)
        return orders.filter { it.date >= startOfMonth && it.status == OrderStatus.REFUND }
    }

    fun getRefundsThisYear(): List<Order> {
        val startOfYear = LocalDate.now().withDayOfYear(1)
        return orders.filter { it.date >= startOfYear && it.status == OrderStatus.REFUND }
    }

    fun getAllRefundsEver(): List<Order> =
        orders.filter { it.status == OrderStatus.REFUND }
}