package arc.funpay.common.api

interface JsonProcessor {
    fun parseAndEncode(input: String): String
}
