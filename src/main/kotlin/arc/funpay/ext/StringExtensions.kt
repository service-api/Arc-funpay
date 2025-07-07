package arc.funpay.ext

import arc.funpay.common.api.JsonProcessor
import arc.funpay.common.api.TextParser
import arc.funpay.common.api.TimingParser
import arc.funpay.di.ServiceLocator
import org.koin.core.component.get

fun String.extract(regex: String, default: String = ""): String =
    ServiceLocator.get<TextParser>().extractText(this, regex, default)

fun String.parseTiming(): Long =
    ServiceLocator.get<TimingParser>().parseToMillis(this)

fun String.extractNumber(): Long =
    ServiceLocator.get<TextParser>().extractNumber(this)

fun String.parse(): String =
    ServiceLocator.get<JsonProcessor>().parseAndEncode(this)