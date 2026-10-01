package com.trueprint.app.network

import com.trueprint.app.model.PrinterProfile
import com.trueprint.app.model.DuplexMode
import com.trueprint.app.model.PageRange
import com.trueprint.app.model.PageSelection
import com.trueprint.app.model.PrintOptions
import com.trueprint.app.model.PaperType
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IppProtocolTest {
    @Test
    fun getPrinterAttributesRequestHasCorrectHeaderAndOperationAttributes() {
        val request = IppRequestEncoder.encodeGetPrinterAttributes(
            printerUri = PrinterProfile.Default.generatedUri,
            requestId = 42
        )

        assertEquals(0x02, request[0].toInt() and 0xFF)
        assertEquals(0x00, request[1].toInt() and 0xFF)
        assertEquals(0x00, request[2].toInt() and 0xFF)
        assertEquals(IppRequestEncoder.GetPrinterAttributes, request[3].toInt() and 0xFF)
        assertEquals(42, request[7].toInt() and 0xFF)
        assertEquals(IppTags.OperationAttributes, request[8].toInt() and 0xFF)
        assertTrue(request.toString(StandardCharsets.UTF_8).contains("attributes-charset"))
        assertTrue(request.toString(StandardCharsets.UTF_8).contains("attributes-natural-language"))
        assertTrue(request.toString(StandardCharsets.UTF_8).contains("printer-uri"))
        assertTrue(request.toString(StandardCharsets.UTF_8).contains("requested-attributes"))
        assertEquals(IppTags.EndOfAttributes, request.last().toInt() and 0xFF)
    }

    @Test
    fun printJobIncludesTheCupsPpd600DpiResolutionOption() {
        val request = IppRequestEncoder.encodePrintJob(
            printerUri = PrinterProfile.Default.generatedUri,
            requestId = 42,
            pdfBytes = byteArrayOf(1),
            documentName = "test.pdf",
            copies = 3,
            pageSize = "A4"
        )

        val cupsOptionName = "Resolution".toByteArray(StandardCharsets.UTF_8)
        val cupsOptionValue = "600dpi".toByteArray(StandardCharsets.UTF_8)
        val cupsOptionIndex = request.indexOf(cupsOptionName)

        assertTrue(cupsOptionIndex > 2)
        assertEquals(IppTags.JobAttributes, request[cupsOptionIndex - 4].toInt() and 0xFF)
        assertEquals(IppTags.NameWithoutLanguage, request[cupsOptionIndex - 3].toInt() and 0xFF)
        assertEquals(0, request[cupsOptionIndex - 2].toInt() and 0xFF)
        assertEquals(cupsOptionName.size, request[cupsOptionIndex - 1].toInt() and 0xFF)
        assertEquals(0, request[cupsOptionIndex + cupsOptionName.size].toInt() and 0xFF)
        assertEquals(cupsOptionValue.size, request[cupsOptionIndex + cupsOptionName.size + 1].toInt() and 0xFF)
        assertEquals(
            cupsOptionValue.toList(),
            request
                .copyOfRange(
                    cupsOptionIndex + cupsOptionName.size + 2,
                    cupsOptionIndex + cupsOptionName.size + 2 + cupsOptionValue.size
                )
                .toList()
        )

        val copiesName = "copies".toByteArray(StandardCharsets.UTF_8)
        val copiesIndex = request.indexOf(copiesName)
        assertTrue(copiesIndex > 2)
        assertEquals(IppTags.Integer, request[copiesIndex - 3].toInt() and 0xFF)
        assertEquals(0, request[copiesIndex + copiesName.size].toInt() and 0xFF)
        assertEquals(4, request[copiesIndex + copiesName.size + 1].toInt() and 0xFF)
        assertEquals(
            listOf(0, 0, 0, 3),
            request
                .copyOfRange(
                    copiesIndex + copiesName.size + 2,
                    copiesIndex + copiesName.size + 6
                )
                .map { it.toInt() and 0xFF }
        )

        val pageSizeName = "PageSize".toByteArray(StandardCharsets.UTF_8)
        val pageSizeIndex = request.indexOf(pageSizeName)
        assertTrue(pageSizeIndex > 2)
        assertEquals(IppTags.NameWithoutLanguage, request[pageSizeIndex - 3].toInt() and 0xFF)
        assertEquals(2, request[pageSizeIndex + pageSizeName.size + 1].toInt() and 0xFF)
        assertEquals(
            "A4".toByteArray(StandardCharsets.UTF_8).toList(),
            request.copyOfRange(pageSizeIndex + pageSizeName.size + 2, pageSizeIndex + pageSizeName.size + 4).toList()
        )

        val resolutionName = "printer-resolution".toByteArray(StandardCharsets.UTF_8)
        val nameIndex = request.indexOf(resolutionName)

        assertTrue(nameIndex > 2)
        assertEquals(IppTags.Resolution, request[nameIndex - 3].toInt() and 0xFF)
        assertEquals(0, request[nameIndex - 2].toInt() and 0xFF)
        assertEquals(resolutionName.size, request[nameIndex - 1].toInt() and 0xFF)
        assertEquals(0, request[nameIndex + resolutionName.size].toInt() and 0xFF)
        assertEquals(9, request[nameIndex + resolutionName.size + 1].toInt() and 0xFF)
        assertEquals(
            listOf(0, 0, 2, 88, 0, 0, 2, 88, 3),
            request
                .copyOfRange(
                    nameIndex + resolutionName.size + 2,
                    nameIndex + resolutionName.size + 11
                )
                .map { it.toInt() and 0xFF }
        )
    }

    @Test
    fun printJobSerializesAdvancedJobAttributesWithoutLosingCupsResolution() {
        val request = IppRequestEncoder.encodePrintJob(
            printerUri = PrinterProfile.Default.generatedUri,
            requestId = 42,
            pdfBytes = byteArrayOf(1),
            documentName = "test.pdf",
            copies = 1,
            pageSize = "A4",
            printOptions = PrintOptions(
                pageSelection = PageSelection.Custom("1-3,7", listOf(PageRange(1, 3), PageRange(7, 7))),
                pagesPerSheet = 2,
                duplex = DuplexMode.LongEdge
            )
        )
        assertAttribute(request, IppTags.NameWithoutLanguage, "Resolution", "600dpi")
        assertAttribute(request, IppTags.NameWithoutLanguage, "PageSize", "A4")
        assertAttribute(request, IppTags.NameWithoutLanguage, "MediaType", "PLAIN")
        assertAttribute(request, IppTags.Keyword, "sides", "two-sided-long-edge")
        assertAttribute(request, IppTags.Integer, "number-up", byteArrayOf(0, 0, 0, 2))
        val pageRanges = "page-ranges".toByteArray(StandardCharsets.UTF_8)
        val index = request.indexOf(pageRanges)
        assertTrue(index > 2)
        assertEquals(IppTags.RangeOfInteger, request[index - 3].toInt() and 0xFF)
        assertEquals(listOf(0, 0, 0, 1, 0, 0, 0, 3), request.copyOfRange(index + pageRanges.size + 2, index + pageRanges.size + 10).map { it.toInt() and 0xFF })
    }

    @Test
    fun printJobIncludesCupsPpdMediaTypeWithTheProtectedResolution() {
        val request = IppRequestEncoder.encodePrintJob(
            PrinterProfile.Default.generatedUri, 7, byteArrayOf(1), "test.pdf", 2, "A4",
            PrintOptions(paperType = PaperType.Thick, pagesPerSheet = 4, duplex = DuplexMode.ShortEdge)
        )
        assertAttribute(request, IppTags.NameWithoutLanguage, "Resolution", "600dpi")
        assertAttribute(request, IppTags.NameWithoutLanguage, "MediaType", "THICK")
        assertAttribute(request, IppTags.Integer, "number-up", byteArrayOf(0, 0, 0, 4))
        assertAttribute(request, IppTags.Keyword, "sides", "two-sided-short-edge")
    }

    @Test
    fun parserReadsSuccessfulPrinterAttributes() {
        val response = IppResponseParser.parse(IppResponseFixtures.successfulPrinterResponse())
        val printer = response.toPrinterAttributes()

        assertEquals(IppStatus.SuccessfulOk, response.statusCode)
        assertEquals("Brother HL-L2320D", printer.printerName)
        assertEquals("Brother HL-L2320D series", printer.printerInfo)
        assertEquals("Brother HL-L2320D series", printer.printerMakeAndModel)
        assertEquals("Idle", printer.printerState)
        assertEquals(listOf("none"), printer.printerStateReasons)
        assertEquals(true, printer.acceptingJobs)
    }

    @Test
    fun parserRetainsIppErrorResponse() {
        val response = IppResponseParser.parse(IppResponseFixtures.errorResponse(0x0406))

        assertEquals(0x0406, response.statusCode)
        assertTrue(IppStatus.describe(response.statusCode).contains("could not find"))
    }

    @Test
    fun invalidProfilesAreRejectedBeforeNetworking() {
        val invalid = PrinterProfile.Default.copy(port = 0)

        assertNotNull(IppConnectionValidator.validationError(invalid))
    }
}

private fun assertAttribute(request: ByteArray, tag: Int, name: String, expected: String) =
    assertAttribute(request, tag, name, expected.toByteArray(StandardCharsets.UTF_8))

private fun assertAttribute(request: ByteArray, tag: Int, name: String, expected: ByteArray) {
    val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
    val index = request.indexOf(nameBytes)
    assertTrue(index > 2)
    assertEquals(tag, request[index - 3].toInt() and 0xFF)
    val valueStart = index + nameBytes.size + 2
    assertEquals(expected.toList(), request.copyOfRange(valueStart, valueStart + expected.size).toList())
}

private fun ByteArray.indexOf(target: ByteArray): Int {
    if (target.isEmpty() || target.size > size) return -1

    return indices.firstOrNull { start ->
        start <= size - target.size && target.indices.all { offset ->
            this[start + offset] == target[offset]
        }
    } ?: -1
}

/** Representative binary IPP responses used without a networked printer. */
private object IppResponseFixtures {
    fun successfulPrinterResponse(): ByteArray = ByteArrayOutputStream().apply {
        writeHeader(IppStatus.SuccessfulOk)
        write(IppTags.PrinterAttributes)
        attribute(IppTags.NameWithoutLanguage, "printer-name", "Brother HL-L2320D")
        attribute(IppTags.TextWithoutLanguage, "printer-info", "Brother HL-L2320D series")
        attribute(IppTags.TextWithoutLanguage, "printer-make-and-model", "Brother HL-L2320D series")
        attribute(IppTags.Enum, "printer-state", byteArrayOf(0, 0, 0, 3))
        attribute(IppTags.Keyword, "printer-state-reasons", "none")
        attribute(IppTags.Boolean, "printer-is-accepting-jobs", byteArrayOf(1))
        write(IppTags.EndOfAttributes)
    }.toByteArray()

    fun errorResponse(status: Int): ByteArray = ByteArrayOutputStream().apply {
        writeHeader(status)
        write(IppTags.EndOfAttributes)
    }.toByteArray()

    private fun ByteArrayOutputStream.writeHeader(status: Int) {
        write(0x02)
        write(0x00)
        unsignedShort(status)
        intValue(7)
    }

    private fun ByteArrayOutputStream.attribute(tag: Int, name: String, value: String) {
        attribute(tag, name, value.toByteArray(StandardCharsets.UTF_8))
    }

    private fun ByteArrayOutputStream.attribute(tag: Int, name: String, value: ByteArray) {
        val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
        write(tag)
        unsignedShort(nameBytes.size)
        write(nameBytes)
        unsignedShort(value.size)
        write(value)
    }

    private fun ByteArrayOutputStream.unsignedShort(value: Int) {
        write((value ushr 8) and 0xFF)
        write(value and 0xFF)
    }

    private fun ByteArrayOutputStream.intValue(value: Int) {
        write((value ushr 24) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 8) and 0xFF)
        write(value and 0xFF)
    }
}
