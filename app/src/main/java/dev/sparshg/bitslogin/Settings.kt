package dev.sparshg.bitslogin

@kotlinx.serialization.Serializable
data class Settings(
    val credSet: Boolean = false,
    val qsAdded: Boolean = false,
    val service: Boolean = false,
    val review: Long = System.currentTimeMillis(),
    val address: Int = 0,
    val username: String = "",
    val password: String = ""
)
