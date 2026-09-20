package com.example.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentNotificationParserTest {

    @Test
    fun testDanaNotification() {
        val parsed = PaymentNotificationParser.parse(
            packageName = "id.dana",
            title = "DANA",
            text = "Kamu menerima uang Rp 50.000 dari Ahmad"
        )
        assertNotNull(parsed)
        assertEquals(50000L, parsed?.amount)
        assertEquals("DANA", parsed?.appName)
    }

    @Test
    fun testBcaNotificationWithDecimals() {
        val parsed = PaymentNotificationParser.parse(
            packageName = "com.bca",
            title = "BCA mobile",
            text = "M-Transfer: Rekening Anda menerima transfer Rp 150.000,00 dari Budi"
        )
        assertNotNull(parsed)
        assertEquals(150000L, parsed?.amount)
        assertEquals("BCA Mobile", parsed?.appName)
    }

    @Test
    fun testGoPayQrisNotification() {
        val parsed = PaymentNotificationParser.parse(
            packageName = "com.gojek.gobiz",
            title = "GoBiz",
            text = "Pembayaran QRIS Rp 25.000 berhasil diterima"
        )
        assertNotNull(parsed)
        assertEquals(25000L, parsed?.amount)
        assertEquals("GoBiz", parsed?.appName)
    }

    @Test
    fun testOvoNotification() {
        val parsed = PaymentNotificationParser.parse(
            packageName = "ovo.id",
            title = "OVO",
            text = "Uang masuk Rp 75.000 dari Siti"
        )
        assertNotNull(parsed)
        assertEquals(75000L, parsed?.amount)
        assertEquals("OVO", parsed?.appName)
    }

    @Test
    fun testIgnorePromoNotification() {
        val parsed = PaymentNotificationParser.parse(
            packageName = "id.dana",
            title = "DANA Promo",
            text = "Dapatkan diskon dan voucher cashback hingga Rp 20.000"
        )
        assertNull(parsed)
    }

    @Test
    fun testIgnoreOutgoingTransfer() {
        val parsed = PaymentNotificationParser.parse(
            packageName = "id.dana",
            title = "DANA",
            text = "Kamu telah mentransfer Rp 30.000 ke John"
        )
        assertNull(parsed)
    }
}
