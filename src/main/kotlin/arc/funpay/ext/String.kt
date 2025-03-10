@file:Suppress("NOTHING_TO_INLINE")

package arc.funpay.ext

import kotlinx.serialization.json.Json

/**
 * A Json instance with pretty print enabled.
 */
val json = Json { prettyPrint = true }

/**
 * Extracts a substring that matches the given regular expression.
 *
 * @param regex The regular expression to match.
 * @param default The default value to return if no match is found.
 * @return The extracted substring or the default value if no match is found.
 */
fun String.extract(regex: String, default: String = ""): String =
    Regex(regex).find(this)?.groupValues?.get(1) ?: default

/**
 * Parses a timing string and returns the corresponding time in milliseconds.
 *
 * @return The parsed time in milliseconds.
 */
fun String.parseTiming(): Long {
    return when {
        this == null -> now() + 60000
        "час" in this -> now() + 3600000
        "минут" in this -> now() + 60000
        "секунд" in this -> now() + 30000
        else -> now() + 60000
    }
}

/**
 * Extracts the first number found in the string.
 *
 * @return The extracted number or 0 if no number is found.
 */
fun String.extractNumber(): Long {
    val regex = Regex("(\\d+)")
    return regex.find(this)?.value?.toLong() ?: 0L
}

/**
 * Parses the string as a JSON element and encodes it to a JSON string.
 *
 * @return The JSON string representation of the parsed JSON element.
 */
inline fun String.parse() = json.encodeToString(Json.parseToJsonElement(this))