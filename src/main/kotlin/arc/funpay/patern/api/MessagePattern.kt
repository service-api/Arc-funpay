package arc.funpay.patern.api

interface MessagePattern {
    val pattern: Regex

    fun matches(message: String): Boolean = pattern.matches(message)
    fun extractData(message: String): Map<String, String>
}
