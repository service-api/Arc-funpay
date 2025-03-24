package arc.funpay.module.funpay

import arc.funpay.event.NewChatEvent
import arc.funpay.event.NewMessageEvent
import arc.funpay.ext.now
import arc.funpay.model.funpay.Account
import arc.funpay.module.api.Module
import arc.funpay.module.chat.ChatInfo
import arc.funpay.system.api.FunpayHttpClient
import io.ktor.client.statement.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.koin.core.component.inject

class ChatMonitoringModule : Module() {
    val client by inject<FunpayHttpClient>()
    val account by inject<Account>()

    var currentChats: MutableMap<String, ChatInfo> = mutableMapOf()

    override suspend fun onTick() {
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
                eventBus.post(NewChatEvent(newChat.userName, nodeId))

                if (newChat.lastMessage.isNotEmpty()) {
                    eventBus.post(NewMessageEvent(newChat.userName, nodeId, newChat.lastMessage))
                }
            } else {
                if (oldChat.lastMessageTime != newChat.lastMessageTime) {
                    eventBus.post(NewMessageEvent(newChat.userName, nodeId, newChat.lastMessage))
                }
            }
        }

        currentChats = newChats.toMutableMap()
    }
}