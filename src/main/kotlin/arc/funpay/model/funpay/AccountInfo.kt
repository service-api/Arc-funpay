package arc.funpay.model.funpay

import arc.funpay.model.other.Balance

/**
 * Data class representing account information.
 *
 * @property id The ID of the account.
 * @property name The name of the account holder.
 * @property balance The balance associated with the account.
 */
data class AccountInfo(
    val id: Long,
    val name: String,
    val balance: Balance
)