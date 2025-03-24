package arc.funpay.module.funpay

import arc.funpay.event.NewChatEvent
import arc.funpay.event.NewMessageEvent
import arc.funpay.model.funpay.Account
import arc.funpay.module.api.Module
import arc.funpay.module.chat.ChatInfo
import arc.funpay.system.api.FunpayHttpClient
import io.ktor.client.statement.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.koin.core.component.inject

/**
 * A module that monitors chats on FunPay.
 *
 * This module periodically checks for new chats and new messages in existing chats,
 * and posts corresponding events to the event bus.
 */
class ChatMonitoringModule : Module() {
    /**
     * Injects the FunpayHttpClient for making HTTP requests.
     */
    val client by inject<FunpayHttpClient>()

    /**
     * Injects the Account to retrieve user's golden key and PHPSESSID.
     */
    val account by inject<Account>()

    /**
     * Stores the current chats with their information.
     * The key is the node ID of the chat, and the value is a ChatInfo object.
     */
    var currentChats: MutableMap<String, ChatInfo> = mutableMapOf()

    /**
     * This function is called periodically to check for updates in chats.
     *
     * It fetches the chat list from FunPay, parses the HTML, and identifies new chats
     * and new messages. It then posts events to the event bus accordingly.
     */
    override suspend fun onTick() {
        // Fetch the HTML content of the chat page.
        val html = client.get(
            "/chat/",
            cookies = mapOf(
                "golden_key" to account.goldenKey,
                "PHPSESSID" to account.phpSessionId
            )
        ).bodyAsText()

        val doc: Document = Jsoup.parse(html)
        val chatElements = doc.select("a.contact-item")

        val newChats: MutableMap<String, ChatInfo> = mutableMapOf()
        chatElements.forEach { element ->
            val nodeId = element.attr("data-id")
            val userName = element.selectFirst(".media-user-name")?.text()?.trim() ?: return@forEach
            val lastMessage = element.selectFirst(".contact-item-message")?.text()?.trim() ?: ""

            val lastMessageTime = element.selectFirst(".contact-item-time")?.text()?.trim() ?: ""
            if (nodeId.isNotEmpty() && userName.isNotEmpty()) {
                newChats[nodeId] = ChatInfo(nodeId, userName, lastMessage, lastMessageTime)
            }
        }

        if (currentChats.isEmpty()) {
            currentChats.putAll(newChats)
            newChats.forEach { (nodeId, chatInfo) ->
                if (chatInfo.lastMessageTime.isNotEmpty() || chatInfo.lastMessage.isNotEmpty()) {
                    eventBus.post(NewMessageEvent(chatInfo.userName, nodeId, chatInfo.lastMessage))
                }
            }
            return
        }

        newChats.forEach { (nodeId, newChat) ->
            val oldChat = currentChats[nodeId]

            if (oldChat == null) {
                eventBus.post(NewChatEvent(newChat.userName, nodeId))
                if (newChat.lastMessageTime.isNotEmpty() || newChat.lastMessage.isNotEmpty()) {
                    eventBus.post(NewMessageEvent(newChat.userName, nodeId, newChat.lastMessage))
                }
            } else {
                if (oldChat.lastMessage != newChat.lastMessage || oldChat.lastMessageTime != newChat.lastMessageTime) {
                    eventBus.post(NewMessageEvent(newChat.userName, nodeId, newChat.lastMessage))
                }
            }
        }

        currentChats = newChats.toMutableMap()
    }
}