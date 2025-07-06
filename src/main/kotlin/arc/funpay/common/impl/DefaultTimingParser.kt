package arc.funpay.common.impl

import arc.funpay.common.api.TimeProvider
import arc.funpay.common.api.TimingParser

class DefaultTimingParser(
    val timeProvider: TimeProvider
) : TimingParser {
    override fun parseToMillis(input: String): Long = when {
        input.isBlank() -> timeProvider.getCurrentTime() + DEFAULT_DELAY
        "час" in input -> timeProvider.getCurrentTime() + HOUR_DELAY
        "минут" in input -> timeProvider.getCurrentTime() + MINUTE_DELAY
        "секунд" in input -> timeProvider.getCurrentTime() + SECOND_DELAY
        else -> timeProvider.getCurrentTime() + DEFAULT_DELAY
    }

    companion object {
        const val DEFAULT_DELAY = 60_000L
        const val HOUR_DELAY = 3_600_000L
        const val MINUTE_DELAY = 60_000L
        const val SECOND_DELAY = 30_000L
    }
}
