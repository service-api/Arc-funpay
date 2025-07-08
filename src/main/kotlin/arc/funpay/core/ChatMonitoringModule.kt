
package arc.funpay.core

import arc.funpay.core.api.Module
import arc.funpay.domain.account.Account
import arc.funpay.event.impl.ChatEvent
import arc.funpay.http.api.HttpClient
import io.ktor.client.statement.*
import org.jsoup.Jsoup
import org.koin.core.component.inject

class ChatMonitoringModule : Module() {
    val client by inject<HttpClient>()
    val account by inject<Account>()

    private var chatLastMessage = emptyMap<Long, String>()

    override suspend fun onTick() {
        val newChats = getNewChats()

        if (chatLastMessage.isEmpty()) {
            chatLastMessage = newChats
            return
        }

        newChats.forEach { (nodeId, chat) ->
            val oldChat = chatLastMessage[nodeId]
            if (oldChat != chat) {
                api.getLastMessageInfo(nodeId)?.let { messageInfo ->
                    if (messageInfo.author != account.username) {
                        eventBus.publish(ChatEvent.NewMessage(messageInfo))
                    }
                }
            }
        }
        chatLastMessage = newChats
    }

    suspend fun getNewChats(): Map<Long, String> {
        val html = client.get(
            "/chat/",
            cookies = buildMap {
                put("golden_key", account.goldenKey)
                put("PHPSESSID", account.phpSessionId)
            }
        ).bodyAsText()

        return Jsoup.parse(html)
            .select("a.contact-item")
            .mapNotNull { element ->
                val nodeId = element.attr("data-id")
                val lastMessage = element.selectFirst(".contact-item-message")?.text()?.trim().orEmpty()
                if (lastMessage.isNotEmpty()) nodeId.toLong() to lastMessage else null
            }
            .toMap()
    }

}