package com.trueprint.app.network

import com.trueprint.app.model.PrinterProfile
import com.trueprint.app.model.PrintOptions
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class IppClientException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidConfiguration(message: String) : IppClientException(message)
    class HttpFailure(val httpStatus: Int, message: String) : IppClientException(message)
    class IppFailure(val ippStatus: Int) : IppClientException(IppStatus.describe(ippStatus))
    class Transport(message: String, cause: Throwable? = null) : IppClientException(message, cause)
    class Protocol(message: String, cause: Throwable? = null) : IppClientException(message, cause)
}
data class IppPrintJobResult(
    val jobId: Int?,
    val jobUri: String?
)

object IppConnectionValidator {
    fun validationError(profile: PrinterProfile): String? = when {
        profile.protocol.trim().lowercase() !in setOf("ipp", "ipps") -> "Protocol must be IPP or IPPS."
        profile.server.isBlank() -> "Server or IP address is required."
        profile.port !in 1..65535 -> "Port must be between 1 and 65535."
        profile.queue.isBlank() -> "Printer queue is required."
        else -> null
    }
}

class IppClient {
    suspend fun getPrinterAttributes(profile: PrinterProfile): IppPrinterAttributes = withContext(Dispatchers.IO) {
        IppConnectionValidator.validationError(profile)?.let { error ->
            throw IppClientException.InvalidConfiguration(error)
        }

        val requestId = requestIds.next()
        val request = IppRequestEncoder.encodeGetPrinterAttributes(profile.generatedUri, requestId)
        val endpoint = try {
            endpointFor(profile)
        } catch (error: Exception) {
            throw IppClientException.InvalidConfiguration("The configured server address is not valid.")
        }

        val connection = try {
            endpoint.openConnection() as HttpURLConnection
        } catch (error: IOException) {
            throw IppClientException.Transport("Cannot connect to CUPS at ${profile.serverAddress}.", error)
        }

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = ConnectTimeoutMillis
            connection.readTimeout = ReadTimeoutMillis
            connection.doOutput = true
            connection.useCaches = false
            connection.instanceFollowRedirects = false
            connection.setFixedLengthStreamingMode(request.size)
            connection.setRequestProperty("Content-Type", "application/ipp")
            connection.setRequestProperty("Accept", "application/ipp")
            connection.setRequestProperty("Connection", "close")
            connection.setRequestProperty("Host", profile.serverAddress)

            connection.outputStream.use { it.write(request) }
            val httpStatus = connection.responseCode
            if (httpStatus !in 200..299) {
                throw IppClientException.HttpFailure(
                    httpStatus,
                    "CUPS returned HTTP $httpStatus instead of an IPP response."
                )
            }

            val contentType = connection.contentType.orEmpty()
            if (!contentType.startsWith("application/ipp", ignoreCase = true)) {
                throw IppClientException.Protocol(
                    "CUPS returned HTTP $httpStatus, but the response was not application/ipp."
                )
            }

            val responseBytes = connection.inputStream.use { it.readBytes() }
            val response = try {
                IppResponseParser.parse(responseBytes)
            } catch (error: IllegalArgumentException) {
                throw IppClientException.Protocol("CUPS returned an invalid IPP response.", error)
            }
            if (response.statusCode != IppStatus.SuccessfulOk) {
                throw IppClientException.IppFailure(response.statusCode)
            }
            response.toPrinterAttributes()
        } catch (error: IppClientException) {
            throw error
        } catch (error: SecurityException) {
            throw IppClientException.Transport(
                "Cleartext HTTP is blocked for this CUPS server configuration.",
                error
            )
        } catch (error: IOException) {
            throw IppClientException.Transport("Cannot connect to CUPS at ${profile.serverAddress}.", error)
        } finally {
            connection.disconnect()
        }
    }
    suspend fun printPdf(
        profile: PrinterProfile,
        pdfBytes: ByteArray,
        documentName: String,
        copies: Int,
        pageSize: String,
        printOptions: PrintOptions = PrintOptions()
    ): IppPrintJobResult = withContext(Dispatchers.IO) {
        IppConnectionValidator.validationError(profile)?.let { error ->
            throw IppClientException.InvalidConfiguration(error)
        }

        require(pdfBytes.isNotEmpty()) {
            "PDF document is empty."
        }

        require(copies in 1..99) {
            "Copies must be between 1 and 99."
        }

        require(pageSize.isNotBlank()) {
            "Paper size is required."
        }

        val safeDocumentName = documentName
            .trim()
            .ifBlank { "TruePrint.pdf" }

        val requestId = requestIds.next()

        val request = try {
            IppRequestEncoder.encodePrintJob(
                printerUri = profile.generatedUri,
                requestId = requestId,
                pdfBytes = pdfBytes,
                documentName = safeDocumentName,
                copies = copies,
                pageSize = pageSize,
                printOptions = printOptions
            )
        } catch (error: IllegalArgumentException) {
            throw IppClientException.InvalidConfiguration(
                error.message ?: "The PDF print request is invalid."
            )
        }

        val endpoint = try {
            endpointFor(profile)
        } catch (error: Exception) {
            throw IppClientException.InvalidConfiguration(
                "The configured server address is not valid."
            )
        }

        val connection = try {
            endpoint.openConnection() as HttpURLConnection
        } catch (error: IOException) {
            throw IppClientException.Transport(
                "Cannot connect to CUPS at ${profile.serverAddress}.",
                error
            )
        }

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = ConnectTimeoutMillis
            connection.readTimeout = ReadTimeoutMillis
            connection.doOutput = true
            connection.useCaches = false
            connection.instanceFollowRedirects = false
            connection.setFixedLengthStreamingMode(request.size)

            connection.setRequestProperty(
                "Content-Type",
                "application/ipp"
            )
            connection.setRequestProperty(
                "Accept",
                "application/ipp"
            )
            connection.setRequestProperty(
                "Connection",
                "close"
            )
            connection.setRequestProperty(
                "Host",
                profile.serverAddress
            )

            connection.outputStream.use { output ->
                output.write(request)
            }

            val httpStatus = connection.responseCode

            if (httpStatus !in 200..299) {
                throw IppClientException.HttpFailure(
                    httpStatus,
                    "CUPS returned HTTP $httpStatus instead of an IPP response."
                )
            }

            val contentType = connection.contentType.orEmpty()

            if (!contentType.startsWith(
                    "application/ipp",
                    ignoreCase = true
                )
            ) {
                throw IppClientException.Protocol(
                    "CUPS returned HTTP $httpStatus, but the response was not application/ipp."
                )
            }

            val responseBytes = connection.inputStream.use {
                it.readBytes()
            }

            val response = try {
                IppResponseParser.parse(responseBytes)
            } catch (error: IllegalArgumentException) {
                throw IppClientException.Protocol(
                    "CUPS returned an invalid IPP response.",
                    error
                )
            }

            if (response.statusCode != IppStatus.SuccessfulOk) {
                throw IppClientException.IppFailure(
                    response.statusCode
                )
            }

            IppPrintJobResult(
                jobId = response.firstInteger("job-id"),
                jobUri = response.firstText("job-uri")
            )
        } catch (error: IppClientException) {
            throw error
        } catch (error: SecurityException) {
            throw IppClientException.Transport(
                "Cleartext HTTP is blocked for this CUPS server configuration.",
                error
            )
        } catch (error: IOException) {
            throw IppClientException.Transport(
                "Cannot connect to CUPS at ${profile.serverAddress}.",
                error
            )
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val ConnectTimeoutMillis = 10_000
        private const val ReadTimeoutMillis = 15_000

        internal fun endpointFor(profile: PrinterProfile): URL {
            val scheme = if (profile.protocol.equals("ipps", ignoreCase = true)) "https" else "http"
            return URI(
                scheme,
                null,
                profile.server.trim(),
                profile.port,
                "/printers/${profile.queue.trim()}",
                null,
                null
            ).toURL()
        }
    }
}

private class IppRequestIds {
    private val nextId = AtomicInteger(1)

    fun next(): Int = nextId.getAndUpdate { current ->
        if (current == Int.MAX_VALUE) 1 else current + 1
    }
}

private val requestIds = IppRequestIds()

object IppStatus {
    const val SuccessfulOk = 0x0000

    fun describe(statusCode: Int): String = when (statusCode) {
        SuccessfulOk -> "IPP request completed successfully."
        0x0400 -> "CUPS rejected the IPP request as malformed."
        0x0401 -> "CUPS denied the request."
        0x0402 -> "CUPS requires authentication."
        0x0406 -> "CUPS could not find the configured printer queue."
        0x0501 -> "CUPS does not support this IPP operation."
        0x0502 -> "CUPS is temporarily unavailable."
        else -> "CUPS returned IPP status 0x${statusCode.toString(16).padStart(4, '0')}."
    }
}
