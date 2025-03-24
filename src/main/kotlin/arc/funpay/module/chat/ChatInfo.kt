package arc.funpay.module.chat

/**
 * Data class representing chat information.
 *
 * @property nodeId The ID of the node associated with the chat.
 * @property userName The name of the user in the chat.
 * @property lastMessage The content of the last message in the chat.
 * @property lastMessageTime The timestamp of the last message in the chat.
 */
data class ChatInfo(
    val nodeId: String,
    val userName: String,
    val lastMessage: String,
    val lastMessageTime: String
)