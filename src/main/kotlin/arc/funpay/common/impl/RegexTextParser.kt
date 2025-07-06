package arc.funpay.common.impl

import arc.funpay.common.api.TextParser

class RegexTextParser : TextParser {
    override fun extractText(input: String, pattern: String, default: String): String =
        Regex(pattern).find(input)?.groupValues?.get(1) ?: default

    override fun extractNumber(input: String): Long =
        Regex("(\\d+)").find(input)?.value?.toLong() ?: 0L
}
