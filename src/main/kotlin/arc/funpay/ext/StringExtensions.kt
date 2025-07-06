package arc.funpay.ext

import arc.funpay.common.api.JsonProcessor
import arc.funpay.common.api.TextParser
import arc.funpay.common.api.TimingParser
import arc.funpay.di.api.DependencyContainer
import arc.funpay.di.api.get

class StringExtensions(val container: DependencyContainer) {
    fun String.extract(regex: String, default: String = ""): String =
        container.get<TextParser>().extractText(this, regex, default)

    fun String.parseTiming(): Long =
        container.get<TimingParser>().parseToMillis(this)

    fun String.extractNumber(): Long =
        container.get<TextParser>().extractNumber(this)

    fun String.parse(): String =
        container.get<JsonProcessor>().parseAndEncode(this)
}
