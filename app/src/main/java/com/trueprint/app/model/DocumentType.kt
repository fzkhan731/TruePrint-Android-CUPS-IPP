package com.trueprint.app.model

enum class DocumentType(val canonicalMimeType: String) {
    Pdf("application/pdf"),
    Jpeg("image/jpeg"),
    Png("image/png")
}

/** Resolves only explicitly supported content types; a generic image MIME is never enough alone. */
object DocumentTypeResolver {
    fun resolve(resolverMimeType: String?, intentMimeType: String?, displayName: String?): DocumentType? {
        listOf(resolverMimeType, intentMimeType).forEach { mime -> explicitMime(mime)?.let { return it } }
        return fromExtension(displayName)
    }

    private fun explicitMime(mime: String?) = when (mime?.trim()?.lowercase()) {
        "application/pdf" -> DocumentType.Pdf
        "image/jpeg", "image/jpg" -> DocumentType.Jpeg
        "image/png" -> DocumentType.Png
        else -> null
    }

    private fun fromExtension(name: String?) = when (name?.substringAfterLast('.', "")?.lowercase()) {
        "pdf" -> DocumentType.Pdf
        "jpg", "jpeg" -> DocumentType.Jpeg
        "png" -> DocumentType.Png
        else -> null
    }
}
