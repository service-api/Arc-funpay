package arc.funpay.common.impl

import arc.funpay.common.api.JsonProcessor
import kotlinx.serialization.json.Json

class KotlinxJsonProcessor : JsonProcessor {
    val json = Json { prettyPrint = true }

    override fun parseAndEncode(input: String): String =
        json.encodeToString(json.parseToJsonElement(input))
}
