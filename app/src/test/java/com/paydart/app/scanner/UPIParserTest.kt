package com.paydart.app.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UPIParserTest {

    @Test
    fun parse_validFullUpiUri_returnsPopulatedRequest() {
        val raw = "upi://pay?pa=merchant@okaxis&pn=Super%20Mart&am=250.50&cu=INR&tn=Snacks&tr=TXN123456&mc=5411"
        val result = UPIParser.parse(raw)

        assertNotNull(result)
        result?.let {
            assertEquals("merchant@okaxis", it.payeeVpa)
            assertEquals("Super Mart", it.payeeName)
            assertEquals("250.50", it.amount)
            assertEquals("₹250.50", it.formattedAmount())
            assertEquals("INR", it.currency)
            assertEquals("Snacks", it.note)
            assertEquals("TXN123456", it.txnRef)
            assertEquals("5411", it.merchantCode)
        }
    }

    @Test
    fun parse_validMinimalUpiUri_returnsRequestWithDefaults() {
        val raw = "upi://pay?pa=friend@oksbi"
        val result = UPIParser.parse(raw)

        assertNotNull(result)
        result?.let {
            assertEquals("friend@oksbi", it.payeeVpa)
            assertNull(it.payeeName)
            assertNull(it.amount)
            assertNull(it.formattedAmount())
            assertEquals("INR", it.currency)
            assertEquals("friend@oksbi", it.displayName)
        }
    }

    @Test
    fun parse_nonUpiUri_returnsNull() {
        assertNull(UPIParser.parse("https://google.com"))
        assertNull(UPIParser.parse("tel:+919876543210"))
        assertNull(UPIParser.parse("WIFI:S:MyWifi;T:WPA;P:secret;;"))
        assertNull(UPIParser.parse("Just some random text from barcode"))
        assertNull(UPIParser.parse(null))
        assertNull(UPIParser.parse(""))
    }

    @Test
    fun parse_invalidSchemeOrHost_returnsNull() {
        // Must be upi://pay
        assertNull(UPIParser.parse("upi://collect?pa=someone@upi"))
        assertNull(UPIParser.parse("http://pay?pa=someone@upi"))
    }

    @Test
    fun parse_missingOrMalformedVpa_returnsNull() {
        assertNull(UPIParser.parse("upi://pay?pn=Someone&am=100"))
        assertNull(UPIParser.parse("upi://pay?pa=invalid-vpa-without-at&am=100"))
        assertNull(UPIParser.parse("upi://pay?pa=@bank&am=100"))
        assertNull(UPIParser.parse("upi://pay?pa=user@&am=100"))
    }

    @Test
    fun parse_invalidAmount_returnsNull() {
        // Negative amount
        assertNull(UPIParser.parse("upi://pay?pa=user@upi&am=-50"))
        // Zero amount
        assertNull(UPIParser.parse("upi://pay?pa=user@upi&am=0"))
        assertNull(UPIParser.parse("upi://pay?pa=user@upi&am=0.00"))
        // Non-numeric amount
        assertNull(UPIParser.parse("upi://pay?pa=user@upi&am=abc"))
        // More than 2 decimal places
        assertNull(UPIParser.parse("upi://pay?pa=user@upi&am=10.999"))
    }
}
