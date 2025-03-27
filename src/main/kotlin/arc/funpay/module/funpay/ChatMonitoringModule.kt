package arc.funpay.module.funpay

import arc.funpay.GlobalSettings
import arc.funpay.event.NewChatEvent
import arc.funpay.event.NewMessageEvent
import arc.funpay.ext.now
import arc.funpay.model.chat.ChatInfo
import arc.funpay.model.funpay.Account
import arc.funpay.module.api.Module
import arc.funpay.patern.ChatPattern
import arc.funpay.system.api.FunpayHttpClient
import io.ktor.client.statement.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.koin.core.component.inject

/**
 * This module is responsible for monitoring chats on the Funpay platform.
 * It fetches chat data, identifies new chats and messages, and posts corresponding events.
 */
class ChatMonitoringModule : Module() {
    val client by inject<FunpayHttpClient>()
    val account by inject<Account>()

    /**
     * Stores the current chats with their information.
     * Key is the node ID of the chat.
     */
    var currentChats: MutableMap<String, ChatInfo> = mutableMapOf()

    /**
     * This function is executed on each tick of the module.
     * It fetches the chat list from Funpay, parses it, and identifies new or updated chats.
     */
    override suspend fun onTick() {
        if (GlobalSettings.isSendingMessage) return

        val html = client.get(
            "/chat/",
            cookies = mapOf(
                "golden_key" to account.goldenKey,
                "PHPSESSID" to account.phpSessionId
            )
        ).bodyAsText()

        val doc: Document = Jsoup.parse(html)
        val chatElements = doc.select("a.contact-item")

        val newChats = mutableMapOf<String, ChatInfo>()
        chatElements.forEach { element ->
            val nodeId = element.attr("data-id")
            val userName = element.selectFirst(".media-user-name")?.text()?.trim() ?: return@forEach
            val lastMessage = element.selectFirst(".contact-item-message")?.text()?.trim() ?: ""
            if (nodeId.isNotEmpty() && userName.isNotEmpty()) {
                newChats[nodeId] = ChatInfo(nodeId, userName, lastMessage, now())
            }
        }

        if (currentChats.isEmpty()) {
            currentChats.putAll(newChats)
            newChats.forEach { (nodeId, chatInfo) ->
                if (chatInfo.lastMessage.isNotEmpty()) {
                    eventBus.post(NewMessageEvent(chatInfo.userName, nodeId, chatInfo.lastMessage))
                }
            }
            return
        }
        newChats.forEach { (nodeId, newChat) ->
            val oldChat = currentChats[nodeId]

            if (oldChat == null) {
                val isNotSystemMessage = !ChatPattern.ORDER_OPENED.matches(newChat.lastMessage)

                if (isNotSystemMessage) {
                    eventBus.post(NewChatEvent(newChat.userName, nodeId))
                }
                if (newChat.lastMessage.isNotEmpty()) {
                    eventBus.post(NewMessageEvent(newChat.userName, nodeId, newChat.lastMessage))
                }
                return@forEach
            }

            if (oldChat.lastMessage != newChat.lastMessage) {
                eventBus.post(NewMessageEvent(newChat.userName, nodeId, newChat.lastMessage))
            }
        }

        currentChats = newChats.toMutableMap()
    }
}