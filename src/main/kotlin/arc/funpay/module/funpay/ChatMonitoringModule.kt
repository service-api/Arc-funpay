package arc.funpay.module.funpay

import arc.funpay.event.NewChatEvent
import arc.funpay.model.funpay.Account
import arc.funpay.module.api.Module
import arc.funpay.system.api.FunpayHttpClient
import io.ktor.client.statement.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.koin.core.component.inject

class ChatMonitoringModule : Module() {
    val client by inject<FunpayHttpClient>()
    val account by inject<Account>()

    var currentChats: MutableMap<String, String> = mutableMapOf()

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

        val newChats: MutableMap<String, String> = mutableMapOf()
        chatElements.forEach { element ->
            val nodeId = element.attr("data-id")
            val userName = element.selectFirst(".media-user-name")?.text()?.trim()
            if (nodeId.isNotEmpty() && !userName.isNullOrEmpty()) {
                newChats[nodeId] = userName
            }
        }

        if (currentChats.isEmpty()) {
            currentChats.putAll(newChats)
            return
        }

        val addedChats = newChats.filter { (nodeId, _) -> !currentChats.containsKey(nodeId) }

        if (addedChats.isNotEmpty()) {
            addedChats.forEach { (nodeId, userName) ->
                eventBus.post(NewChatEvent(userName, nodeId))
            }
            currentChats = newChats.toMutableMap()
        }
    }
}