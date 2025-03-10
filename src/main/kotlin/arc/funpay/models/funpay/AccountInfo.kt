package arc.funpay.models.funpay

import arc.funpay.models.other.Balance

/**
 * Data class representing account information.
 *
 * @property id The ID of the account.
 * @property name The name of the account holder.
 * @property balance The balance associated with the account.
 */
data class AccountInfo(
    val id: String,
    val name: String,
    val balance: Balance
)