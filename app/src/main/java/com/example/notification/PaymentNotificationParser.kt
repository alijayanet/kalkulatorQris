package com.example.notification

data class ParsedPayment(
    val amount: Long,
    val appName: String,
    val packageName: String,
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis()
)

object PaymentNotificationParser {

    private val KNOWN_APPS = mapOf(
        "id.dana" to "DANA",
        "ovo.id" to "OVO",
        "ovo.merchant" to "OVO Merchant",
        "com.gojek.app" to "GoPay",
        "com.gojek.gobiz" to "GoBiz",
        "com.shopee.id" to "ShopeePay",
        "com.shopee.partner" to "Shopee Partner",
        "com.telkom.mwallet" to "LinkAja",
        "com.bca" to "BCA Mobile",
        "com.bca.mybca" to "myBCA",
        "com.bca.merchant" to "BCA Merchant",
        "id.bmri.livin" to "Livin' Mandiri",
        "id.co.bri.brimo" to "BRImo",
        "id.co.bni.wondr" to "wondr by BNI",
        "src.com.bni" to "BNI Mobile",
        "com.seabank.mobile" to "SeaBank",
        "com.jago.digitalbanking" to "Bank Jago",
        "id.co.bcadigital.blu" to "blu by BCA",
        "com.bank.neo" to "NeoBank"
    )

    private val INCOMING_KEYWORDS = listOf(
        "masuk", "diterima", "berhasil", "kredit", "cr", "terima bayar",
        "menerima", "payment received", "transaksi qris", "uang masuk",
        "dana masuk", "transfer masuk", "topup", "top up", "lunas",
        "pembayaran qris", "terima transfer"
    )

    private val IGNORE_KEYWORDS = listOf(
        "berhasil transfer ke", "kamu telah mentransfer", "pembayaran ke",
        "berhasil bayar ke", "tagihan", "promo", "diskon", "cashback",
        "voucher", "pinjaman", "paylater", "ajak teman", "diskon spesial"
    )

    // Regex pattern matching Indonesian Rupiah amounts: e.g. Rp 50.000, Rp50.000,00, IDR 25.000, Rp 10000
    private val AMOUNT_REGEX = Regex("""(?i)(?:Rp\.?|IDR)\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?|[0-9]+)""")

    /**
     * Parses notification title and content. Returns [ParsedPayment] if it's an incoming payment with a valid amount.
     */
    fun parse(packageName: String, title: String?, text: String?): ParsedPayment? {
        val safeTitle = title ?: ""
        val safeText = text ?: ""
        val combined = "$safeTitle $safeText".trim()
        if (combined.isBlank()) return null

        val lowerCombined = combined.lowercase()

        // 1. Check ignore list first (e.g. money sent out, promo notifications)
        for (ignore in IGNORE_KEYWORDS) {
            if (lowerCombined.contains(ignore)) {
                return null
            }
        }

        // 2. Must contain at least one incoming money keyword
        val hasIncomingKeyword = INCOMING_KEYWORDS.any { lowerCombined.contains(it) }
        if (!hasIncomingKeyword) {
            return null
        }

        // 3. Find amount in text
        val match = AMOUNT_REGEX.find(combined) ?: return null
        val rawAmountGroup = match.groupValues[1]

        val amount = parseNominalLong(rawAmountGroup) ?: return null
        if (amount <= 0) return null

        val appName = KNOWN_APPS[packageName] ?: detectAppNameFromText(lowerCombined) ?: "E-Wallet/Bank"

        return ParsedPayment(
            amount = amount,
            appName = appName,
            packageName = packageName,
            rawText = combined
        )
    }

    /**
     * Converts formatted Indonesian amount string into Long.
     */
    fun parseNominalLong(raw: String): Long? {
        var str = raw.trim()
        // Remove trailing cents like ,00 or .00
        if (str.endsWith(",00") || str.endsWith(".00")) {
            str = str.substring(0, str.length - 3)
        } else if (str.matches(Regex(""".*[.,]\d{2}$"""))) {
            str = str.substring(0, str.length - 3)
        }

        // Remove thousand separators
        str = str.replace(".", "").replace(",", "").trim()
        return str.toLongOrNull()
    }

    private fun detectAppNameFromText(text: String): String? {
        return when {
            text.contains("dana") -> "DANA"
            text.contains("ovo") -> "OVO"
            text.contains("gopay") || text.contains("gobiz") -> "GoPay"
            text.contains("shopee") -> "ShopeePay"
            text.contains("linkaja") -> "LinkAja"
            text.contains("bca") -> "BCA"
            text.contains("mandiri") || text.contains("livin") -> "Livin' Mandiri"
            text.contains("brimo") || text.contains("bri") -> "BRImo"
            text.contains("bni") || text.contains("wondr") -> "BNI"
            text.contains("seabank") -> "SeaBank"
            text.contains("jago") -> "Bank Jago"
            else -> null
        }
    }
}
