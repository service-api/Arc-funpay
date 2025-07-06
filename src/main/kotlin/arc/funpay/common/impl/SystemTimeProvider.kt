package arc.funpay.common.impl

import arc.funpay.common.api.TimeProvider

class SystemTimeProvider : TimeProvider {
    override fun getCurrentTime(): Long = System.currentTimeMillis()
}
