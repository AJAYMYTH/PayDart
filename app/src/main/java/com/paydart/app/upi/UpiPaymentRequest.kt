package com.paydart.app.upi

/**
 * Immutable data representation of a validated UPI payment request parsed from a QR code.
 */
data class UpiPaymentRequest(
    val rawUri: String,
    val payeeVpa: String,
    val payeeName: String? = null,
    val amount: String? = null,
    val currency: String = "INR",
    val note: String? = null,
    val txnRef: String? = null,
    val merchantCode: String? = null
) {
    /**
     * Formats amount for display, e.g. "₹500.00" or null if no amount is pre-specified.
     */
    fun formattedAmount(): String? {
        val amt = amount?.trim() ?: return null
        return if (amt.isNotEmpty()) "₹$amt" else null
    }

    /**
     * Display name or fallback to VPA.
     */
    val displayName: String
        get() = payeeName?.takeIf { it.isNotBlank() } ?: payeeVpa
}
