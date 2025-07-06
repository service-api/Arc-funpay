package arc.funpay.common.api

interface TimingParser {
    fun parseToMillis(input: String): Long
}