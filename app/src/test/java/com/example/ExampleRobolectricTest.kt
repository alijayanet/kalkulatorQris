package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.qris.QrisEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Kalkulator QRIS", appName)
    }

    @Test
    fun `test qris dynamic generation and crc16`() {
        val dynamicQris = QrisEngine.generateDynamicQris(
            baseStaticQris = QrisEngine.DEFAULT_STATIC_QRIS,
            amount = 50000L
        )
        assertNotNull(dynamicQris)
        assertTrue(dynamicQris.contains("540550000"))
        assertTrue(dynamicQris.contains("010212")) // Dynamic tag 01 = 12
        assertTrue(dynamicQris.endsWith(QrisEngine.calculateCrc16(dynamicQris.dropLast(4))))
    }
}
