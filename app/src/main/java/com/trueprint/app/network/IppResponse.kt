package com.trueprint.app.network

import java.nio.charset.StandardCharsets

data class IppResponse(
    val versionMajor: Int,
    val versionMinor: Int,
    val statusCode: Int,
    val requestId: Int,
    val attributes: List<IppAttribute>
) {
    fun firstText(name: String): String? = attributes
        .firstOrNull { it.name == name }
        ?.value
        ?.toString(StandardCharsets.UTF_8)

    fun textValues(name: String): List<String> = attributes
        .filter { it.name == name }
        .map { it.value.toString(StandardCharsets.UTF_8) }

    fun firstInteger(name: String): Int? = attributes
        .firstOrNull { it.name == name }
        ?.value
        ?.takeIf { it.size == 4 }
        ?.let { bytes ->
            ((bytes[0].toInt() and 0xFF) shl 24) or
                ((bytes[1].toInt() and 0xFF) shl 16) or
                ((bytes[2].toInt() and 0xFF) shl 8) or
                (bytes[3].toInt() and 0xFF)
        }

    fun firstBoolean(name: String): Boolean? = attributes
        .firstOrNull { it.name == name }
        ?.value
        ?.firstOrNull()
        ?.let { it.toInt() != 0 }

    fun toPrinterAttributes() = IppPrinterAttributes(
        printerName = firstText("printer-name"),
        printerInfo = firstText("printer-info"),
        printerMakeAndModel = firstText("printer-make-and-model"),
        printerState = printerStateName(firstInteger("printer-state")),
        printerStateReasons = textValues("printer-state-reasons"),
        acceptingJobs = firstBoolean("printer-is-accepting-jobs")
    )

    private fun printerStateName(value: Int?): String? = when (value) {
        3 -> "Idle"
        4 -> "Processing"
        5 -> "Stopped"
        null -> null
        else -> "Unknown ($value)"
    }
}

data class IppPrinterAttributes(
    val printerName: String?,
    val printerInfo: String?,
    val printerMakeAndModel: String?,
    val printerState: String?,
    val printerStateReasons: List<String>,
    val acceptingJobs: Boolean?
)

object IppResponseParser {
    fun parse(bytes: ByteArray): IppResponse {
        val reader = IppByteReader(bytes)
        require(reader.remaining >= 8) { "IPP response is shorter than its required header." }
        val versionMajor = reader.readByte()
        val versionMinor = reader.readByte()
        val statusCode = reader.readUnsignedShort()
        val requestId = reader.readInt()
        val attributes = mutableListOf<IppAttribute>()
        var currentGroup = -1
        var lastName: String? = null

        while (reader.remaining > 0) {
            val tag = reader.readByte()
            when (tag) {
                IppTags.EndOfAttributes -> break
                in IppTags.OperationAttributes..IppTags.PrinterAttributes + 1 -> {
                    currentGroup = tag
                    lastName = null
                }
                else -> {
                    require(currentGroup != -1) { "IPP attribute appeared before an attribute group." }
                    val nameLength = reader.readUnsignedShort()
                    val name = if (nameLength == 0) {
                        requireNotNull(lastName) { "IPP attribute omitted its name without a preceding name." }
                    } else {
                        reader.readString(nameLength).also { lastName = it }
                    }
                    val valueLength = reader.readUnsignedShort()
                    attributes += IppAttribute(currentGroup, tag, name, reader.readBytes(valueLength))
                }
            }
        }
        return IppResponse(versionMajor, versionMinor, statusCode, requestId, attributes)
    }
}

private class IppByteReader(private val bytes: ByteArray) {
    private var position = 0
    val remaining: Int get() = bytes.size - position

    fun readByte(): Int = readBytes(1)[0].toInt() and 0xFF

    fun readUnsignedShort(): Int {
        val value = readBytes(2)
        return ((value[0].toInt() and 0xFF) shl 8) or (value[1].toInt() and 0xFF)
    }

    fun readInt(): Int {
        val value = readBytes(4)
        return ((value[0].toInt() and 0xFF) shl 24) or
            ((value[1].toInt() and 0xFF) shl 16) or
            ((value[2].toInt() and 0xFF) shl 8) or
            (value[3].toInt() and 0xFF)
    }

    fun readString(length: Int): String = readBytes(length).toString(StandardCharsets.UTF_8)

    fun readBytes(length: Int): ByteArray {
        require(length >= 0 && length <= remaining) { "IPP response ended unexpectedly." }
        return bytes.copyOfRange(position, position + length).also { position += length }
    }
}
