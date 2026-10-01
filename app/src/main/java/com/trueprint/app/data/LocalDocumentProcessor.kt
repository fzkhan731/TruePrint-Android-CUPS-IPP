package com.trueprint.app.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.trueprint.app.model.PageRange
import com.trueprint.app.model.PrintOrientation
import java.io.File
import java.util.UUID
import kotlin.math.min

/** Local-only transformations used only when CUPS cannot express the requested composition. */
class LocalDocumentProcessor(private val context: Context) {
    init { PDFBoxResourceLoader.init(context.applicationContext) }

    fun pdfPageCount(uri: Uri): Int = context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
        PdfRenderer(descriptor).use { it.pageCount }
    } ?: throw IllegalStateException("Unable to inspect the selected PDF.")

    fun readBytes(uri: Uri): ByteArray = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IllegalStateException("Unable to open the selected document.")

    /** Imports selected existing PDF pages without rasterizing them. Caller deletes the returned file. */
    fun createReducedPdf(uri: Uri, ranges: List<PageRange>, orientation: PrintOrientation = PrintOrientation.Portrait): File {
        val sourceFile = copyUriToCache(uri, "source", ".pdf")
        val destination = cacheFile("selected-pages", ".pdf")
        try {
            PDDocument.load(sourceFile).use { source ->
                PDDocument().use { output ->
                    ranges.flatMap { range -> (range.start..range.end).toList() }.forEach { pageNumber ->
                        if (pageNumber !in 1..source.numberOfPages) {
                            throw IllegalArgumentException("Selected page $pageNumber is outside this document.")
                        }
                        val sourcePage = source.getPage(pageNumber - 1)
                        output.importPage(sourcePage).rotation = if (orientation == PrintOrientation.Landscape) (sourcePage.rotation + 90) % 360 else sourcePage.rotation
                    }
                    output.save(destination)
                }
            }
            return destination
        } catch (error: Throwable) {
            destination.delete()
            throw error
        } finally {
            sourceFile.delete()
        }
    }

    /** Rewrites a local temporary copy only; the source PDF remains untouched. */
    fun createOrientedPdf(uri: Uri, orientation: PrintOrientation): File {
        if (orientation == PrintOrientation.Portrait) return copyUriToCache(uri, "print-source", ".pdf")
        val sourceFile = copyUriToCache(uri, "source", ".pdf")
        val destination = cacheFile("landscape", ".pdf")
        try {
            PDDocument.load(sourceFile).use { source -> PDDocument().use { output ->
                (0 until source.numberOfPages).forEach { index ->
                    val page = source.getPage(index)
                    output.importPage(page).rotation = (page.rotation + 90) % 360
                }
                output.save(destination)
            } }
            return destination
        } catch (error: Throwable) { destination.delete(); throw error } finally { sourceFile.delete() }
    }

    /** Creates a one-page, aspect-ratio-preserving PDF in cache from a JPEG or PNG. */
    fun createImagePdf(uri: Uri, mimeType: String, paperSize: String, orientation: PrintOrientation): File {
        val sourceFile = copyUriToCache(uri, "image", imageExtension(mimeType))
        val destination = cacheFile("image-print", ".pdf")
        try {
            val bitmap = decodeScaledBitmap(sourceFile, mimeType)
            try {
                PDDocument().use { document ->
                    val page = PDPage(pageRectangle(paperSize, orientation))
                    document.addPage(page)
                    val image = if (mimeType.equals("image/png", true)) {
                        LosslessFactory.createFromImage(document, bitmap)
                    } else {
                        JPEGFactory.createFromImage(document, bitmap, 0.92f)
                    }
                    val margin = 24f
                    val availableWidth = page.mediaBox.width - margin * 2
                    val availableHeight = page.mediaBox.height - margin * 2
                    val scale = min(availableWidth / bitmap.width, availableHeight / bitmap.height)
                    val width = bitmap.width * scale
                    val height = bitmap.height * scale
                    PDPageContentStream(document, page).use { stream ->
                        stream.drawImage(image, (page.mediaBox.width - width) / 2, (page.mediaBox.height - height) / 2, width, height)
                    }
                    document.save(destination)
                }
            } finally {
                bitmap.recycle()
            }
            return destination
        } catch (error: Throwable) {
            destination.delete()
            throw error
        } finally {
            sourceFile.delete()
        }
    }

    private fun copyUriToCache(uri: Uri, prefix: String, extension: String): File =
        cacheFile(prefix, extension).also { target ->
            try {
                context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use(input::copyTo) }
                    ?: throw IllegalStateException("Unable to open the selected document.")
            } catch (error: Throwable) { target.delete(); throw error }
        }

    private fun cacheFile(prefix: String, extension: String) = File(context.cacheDir, "$prefix-${UUID.randomUUID()}$extension")

    private fun imageExtension(mimeType: String) = if (mimeType.equals("image/png", true)) ".png" else ".jpg"

    private fun decodeScaledBitmap(file: File, mimeType: String): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "The image cannot be decoded." }
        var sample = 1
        while (bounds.outWidth / sample > MaxImageDimension || bounds.outHeight / sample > MaxImageDimension) sample *= 2
        val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IllegalStateException("The image cannot be decoded.")
        if (!mimeType.equals("image/jpeg", true)) return decoded
        val degrees = when (ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return decoded
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true).also { decoded.recycle() }
    }

    private fun pageRectangle(size: String, orientation: PrintOrientation): PDRectangle {
        val portrait = when (size) {
        "A4" -> PDRectangle.A4
        "A5" -> PDRectangle.A5
        "A6" -> PDRectangle(298f, 420f)
        "Letter" -> PDRectangle.LETTER
        "Legal" -> PDRectangle.LEGAL
        "Executive" -> PDRectangle(522f, 756f)
            else -> PDRectangle.A4
        }
        return if (orientation == PrintOrientation.Landscape) PDRectangle(portrait.height, portrait.width) else portrait
    }

    private companion object { const val MaxImageDimension = 2_048 }
}
