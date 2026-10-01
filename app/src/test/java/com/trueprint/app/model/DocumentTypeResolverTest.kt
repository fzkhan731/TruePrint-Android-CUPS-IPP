package com.trueprint.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DocumentTypeResolverTest {
    @Test fun resolvesSupportedMimeAndExtensionCombinations() {
        assertEquals(DocumentType.Jpeg, DocumentTypeResolver.resolve("image/jpeg", null, "PHOTO.JPG"))
        assertEquals(DocumentType.Jpeg, DocumentTypeResolver.resolve("image/jpeg", null, "photo.jpeg"))
        assertEquals(DocumentType.Jpeg, DocumentTypeResolver.resolve("image/jpg", null, null))
        assertEquals(DocumentType.Png, DocumentTypeResolver.resolve("image/png", null, null))
        assertEquals(DocumentType.Pdf, DocumentTypeResolver.resolve("application/pdf", null, null))
        assertEquals(DocumentType.Jpeg, DocumentTypeResolver.resolve("image/jpeg", "image/*", "photo.bin"))
        assertEquals(DocumentType.Png, DocumentTypeResolver.resolve("image/png", "image/*", "photo.bin"))
        assertEquals(DocumentType.Jpeg, DocumentTypeResolver.resolve("image/jpeg", null, null))
        assertEquals(DocumentType.Jpeg, DocumentTypeResolver.resolve("application/octet-stream", null, "PHOTO.JPG"))
        assertEquals(DocumentType.Pdf, DocumentTypeResolver.resolve("application/octet-stream", null, "scan.PDF"))
    }
    @Test fun rejectsUnsupportedOrGenericImagesWithoutSupportedEvidence() {
        assertNull(DocumentTypeResolver.resolve("image/gif", null, "animation.gif"))
        assertNull(DocumentTypeResolver.resolve("image/webp", null, "image.webp"))
        assertNull(DocumentTypeResolver.resolve("image/*", null, "unknown.bin"))
    }
}
