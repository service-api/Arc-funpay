package arc.funpay.common.api

interface TimeProvider {
    fun getCurrentTime(): Long
}
