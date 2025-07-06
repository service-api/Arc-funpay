package arc.funpay.domain.account

import arc.funpay.domain.common.Balance

data class AccountInfo(
    val userId: Long,
    val userName: String,
    val balance: Balance
)