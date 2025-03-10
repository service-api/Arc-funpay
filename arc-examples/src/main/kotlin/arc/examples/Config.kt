package arc.examples

import kotlinx.serialization.Serializable

/**
 * Configuration data class for the application.
 * This class is serializable and can be converted to/from formats like JSON.
 *
 * @property token The authentication token used for API access.
 */
@Serializable
data class Config(
    val token: String
)