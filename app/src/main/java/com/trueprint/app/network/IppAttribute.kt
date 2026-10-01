package com.trueprint.app.network

/** IPP value and group tags used by the Phase 3 Get-Printer-Attributes subset. */
object IppTags {
    const val OperationAttributes = 0x01
    const val JobAttributes = 0x02
    const val PrinterAttributes = 0x04
    const val EndOfAttributes = 0x03

    const val Integer = 0x21
    const val Boolean = 0x22
    const val Enum = 0x23
    const val Resolution = 0x32
    const val RangeOfInteger = 0x33
    const val Uri = 0x45
    const val Charset = 0x47
    const val NaturalLanguage = 0x48
    const val MimeMediaType = 0x49
    const val Keyword = 0x44
    const val NameWithoutLanguage = 0x42
    const val TextWithoutLanguage = 0x41
}

data class IppAttribute(
    val groupTag: Int,
    val valueTag: Int,
    val name: String,
    val value: ByteArray
)
