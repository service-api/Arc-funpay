package arc.funpay.common.api

interface TextParser {
    fun extractText(input: String, pattern: String, default: String = ""): String
    fun extractNumber(input: String): Long
}