package com.trueprint.app.model

/**
 * The editable printer profile that later phases will persist and use for IPP.
 * Phase 1 displays these values only.
 */
data class PrinterProfile(
    val displayName: String,
    val protocol: String,
    val server: String,
    val port: Int,
    val queue: String
) {
    val isConfigured: Boolean
        get() = displayName.isNotBlank() && server.isNotBlank() && queue.isNotBlank()
    val serverAddress: String
        get() = "$server:$port"

    val generatedUri: String
        get() = generatedUri(protocol, server, port.toString(), queue)

    companion object {
        val Default = PrinterProfile(
            displayName = "",
            protocol = "IPP",
            server = "",
            port = 631,
            queue = ""
        )

        fun generatedUri(protocol: String, server: String, port: String, queue: String): String {
            return "${protocol.trim().lowercase()}://${server.trim()}:${port.trim()}/printers/${queue.trim()}"
        }
    }
}
