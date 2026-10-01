package com.trueprint.app

import com.trueprint.app.model.PrinterProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class PrinterProfileTest {
    @Test
    fun freshDefaultProfileIsUnconfigured() {
        assertEquals(false, PrinterProfile.Default.isConfigured)
    }

    @Test
    fun generatedUriUsesEditedProfileFields() {
        val profile = PrinterProfile.Default.copy(
            protocol = "IPPS",
            server = "print-server.local",
            port = 8631,
            queue = "office_queue"
        )

        assertEquals(
            "ipps://print-server.local:8631/printers/office_queue",
            profile.generatedUri
        )
    }
}
