package com.trueprint.app.network

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import com.trueprint.app.model.PageRange
import com.trueprint.app.model.PrintOptions

object IppRequestEncoder {
    private const val IppVersionMajor = 0x02
    private const val IppVersionMinor = 0x00
    private const val PrintResolutionDpi = 600
    private const val ResolutionValueLength = 9
    private const val ResolutionUnitsDotsPerInch = 3

    const val PrintJob = 0x0002
    const val GetPrinterAttributes = 0x000B

    fun encodePrintJob(
        printerUri: String,
        requestId: Int,
        pdfBytes: ByteArray,
        documentName: String,
        copies: Int,
        pageSize: String,
        printOptions: PrintOptions = PrintOptions()
    ): ByteArray {
        require(printerUri.isNotBlank()) {
            "Printer URI is required."
        }

        require(requestId > 0) {
            "IPP request ID must be positive."
        }

        require(pdfBytes.isNotEmpty()) {
            "PDF document is empty."
        }

        require(documentName.isNotBlank()) {
            "Document name is required."
        }

        require(copies in 1..99) {
            "Copies must be between 1 and 99."
        }

        require(pageSize.isNotBlank()) {
            "Paper size is required."
        }

        return ByteArrayOutputStream().apply {
            // IPP version
            write(IppVersionMajor)
            write(IppVersionMinor)

            // Operation: Print-Job
            writeUnsignedShort(PrintJob)

            // Request ID
            writeInt(requestId)

            // Operation attributes group
            write(IppTags.OperationAttributes)

            // Required operation attributes
            writeAttribute(
                IppTags.Charset,
                "attributes-charset",
                "utf-8"
            )

            writeAttribute(
                IppTags.NaturalLanguage,
                "attributes-natural-language",
                "en"
            )

            writeAttribute(
                IppTags.Uri,
                "printer-uri",
                printerUri
            )

            writeAttribute(
                IppTags.NameWithoutLanguage,
                "requesting-user-name",
                "TruePrint"
            )

            writeAttribute(
                IppTags.NameWithoutLanguage,
                "job-name",
                documentName
            )

            writeAttribute(
                IppTags.MimeMediaType,
                "document-format",
                "application/pdf"
            )

            // Job template attributes for this queue.
            write(IppTags.JobAttributes)

            // This PPD-specific CUPS option selects the verified 600 dpi brlaser path.
            // Keep it: this queue accepts standard printer-resolution metadata but does
            // not apply it to the PPD Resolution setting.
            writeAttribute(
                IppTags.NameWithoutLanguage,
                "Resolution",
                "600dpi"
            )

            writeIntegerAttribute("copies", copies)
            writeAttribute(
                IppTags.NameWithoutLanguage,
                "PageSize",
                pageSize
            )
            writeAttribute(
                IppTags.NameWithoutLanguage,
                "MediaType",
                printOptions.paperType.cupsValue
            )

            val customRanges = printOptions.pageSelection
                .let { it as? com.trueprint.app.model.PageSelection.Custom }
                ?.ranges
                .orEmpty()
            if (customRanges.isNotEmpty()) writePageRangesAttribute(customRanges)
            if (printOptions.pagesPerSheet > 1) {
                writeIntegerAttribute("number-up", printOptions.pagesPerSheet)
            }
            writeAttribute(IppTags.Keyword, "sides", printOptions.duplex.ippValue)

            // Retain the standard IPP resolution metadata for compatible IPP queues.
            writeResolutionAttribute(
                name = "printer-resolution",
                crossFeedDpi = PrintResolutionDpi,
                feedDpi = PrintResolutionDpi
            )

            // End of IPP attributes
            write(IppTags.EndOfAttributes)

            // Document data
            write(pdfBytes)
        }.toByteArray()
    }

    fun encodeGetPrinterAttributes(
        printerUri: String,
        requestId: Int
    ): ByteArray {
        require(printerUri.isNotBlank()) {
            "Printer URI is required."
        }

        require(requestId > 0) {
            "IPP request ID must be positive."
        }

        return ByteArrayOutputStream().apply {
            write(IppVersionMajor)
            write(IppVersionMinor)

            writeUnsignedShort(GetPrinterAttributes)

            writeInt(requestId)

            write(IppTags.OperationAttributes)

            writeAttribute(
                IppTags.Charset,
                "attributes-charset",
                "utf-8"
            )

            writeAttribute(
                IppTags.NaturalLanguage,
                "attributes-natural-language",
                "en"
            )

            writeAttribute(
                IppTags.Uri,
                "printer-uri",
                printerUri
            )

            val requestedAttributes = listOf(
                "printer-name",
                "printer-info",
                "printer-make-and-model",
                "printer-state",
                "printer-state-reasons",
                "printer-is-accepting-jobs"
            )

            requestedAttributes.forEachIndexed { index, attributeName ->
                if (index == 0) {
                    writeAttribute(
                        IppTags.Keyword,
                        "requested-attributes",
                        attributeName
                    )
                } else {
                    writeAdditionalValue(
                        IppTags.Keyword,
                        attributeName
                    )
                }
            }

            write(IppTags.EndOfAttributes)
        }.toByteArray()
    }

    private fun ByteArrayOutputStream.writeAttribute(
        tag: Int,
        name: String,
        value: String
    ) {
        val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
        val valueBytes = value.toByteArray(StandardCharsets.UTF_8)

        write(tag)

        writeUnsignedShort(nameBytes.size)
        write(nameBytes)

        writeUnsignedShort(valueBytes.size)
        write(valueBytes)
    }

    private fun ByteArrayOutputStream.writeAdditionalValue(
        tag: Int,
        value: String
    ) {
        val valueBytes = value.toByteArray(StandardCharsets.UTF_8)

        write(tag)

        // Empty attribute name for additional value
        writeUnsignedShort(0)

        writeUnsignedShort(valueBytes.size)
        write(valueBytes)
    }

    private fun ByteArrayOutputStream.writeResolutionAttribute(
        name: String,
        crossFeedDpi: Int,
        feedDpi: Int
    ) {
        require(crossFeedDpi > 0 && feedDpi > 0) {
            "IPP printer resolution must be positive."
        }

        val nameBytes = name.toByteArray(StandardCharsets.UTF_8)

        write(IppTags.Resolution)
        writeUnsignedShort(nameBytes.size)
        write(nameBytes)
        writeUnsignedShort(ResolutionValueLength)
        writeInt(crossFeedDpi)
        writeInt(feedDpi)
        write(ResolutionUnitsDotsPerInch)
    }

    private fun ByteArrayOutputStream.writeIntegerAttribute(name: String, value: Int) {
        val nameBytes = name.toByteArray(StandardCharsets.UTF_8)

        write(IppTags.Integer)
        writeUnsignedShort(nameBytes.size)
        write(nameBytes)
        writeUnsignedShort(Int.SIZE_BYTES)
        writeInt(value)
    }

    private fun ByteArrayOutputStream.writePageRangesAttribute(ranges: List<PageRange>) {
        ranges.forEachIndexed { index, range ->
            write(IppTags.RangeOfInteger)
            if (index == 0) {
                val nameBytes = "page-ranges".toByteArray(StandardCharsets.UTF_8)
                writeUnsignedShort(nameBytes.size)
                write(nameBytes)
            } else {
                writeUnsignedShort(0)
            }
            writeUnsignedShort(8)
            writeInt(range.start)
            writeInt(range.end)
        }
    }

    private fun ByteArrayOutputStream.writeUnsignedShort(value: Int) {
        write((value ushr 8) and 0xFF)
        write(value and 0xFF)
    }

    private fun ByteArrayOutputStream.writeInt(value: Int) {
        write((value ushr 24) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 8) and 0xFF)
        write(value and 0xFF)
    }
}
