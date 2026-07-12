package com.aniauth.authenticator.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TotpGeneratorTest {

    private val seedBase32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

    @Test
    fun testRfc6238TestVectors() {
        // RFC 6238 Test Vectors for SHA-1 with 30s time step
        assertEquals("942870", TotpGenerator.generateTOTP(seedBase32, 30, 59L))
        assertEquals("070818", TotpGenerator.generateTOTP(seedBase32, 30, 1111111111L))
        assertEquals("890059", TotpGenerator.generateTOTP(seedBase32, 30, 1234567890L))
        assertEquals("692790", TotpGenerator.generateTOTP(seedBase32, 30, 2000000000L))
    }

    @Test
    fun testBase32DecodingRobustness() {
        // Test lower-case, spaces, and dashes
        val dirtySeed = "  gezd-gnbv-gy3t-qojq-gezd-gnbv-gy3t-qojq  "
        assertEquals("942870", TotpGenerator.generateTOTP(dirtySeed, 30, 59L))
    }

    @Test
    fun testInvalidSecret() {
        assertNull(TotpGenerator.generateTOTP("INVALID123!"))
        assertNull(TotpGenerator.generateTOTP(""))
    }
}
