package com.portalhost.player

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

class PlayerProfileRepositoryTest {

    @Test
    fun normalizeUuid_addsDashes() {
        val normalized = PlayerProfileRepository.normalizeUuid("2a2adc8bcbfb538934c34edfa27081e9")
        assertEquals("2a2adc8b-cbfb-5389-34c3-4edfa27081e9", normalized)
    }

    @Test
    fun normalizeUuid_alreadyDashed() {
        val normalized = PlayerProfileRepository.normalizeUuid("2a2adc8b-cbfb-5389-34c3-4edfa27081e9")
        assertEquals("2a2adc8b-cbfb-5389-34c3-4edfa27081e9", normalized)
    }

    @Test
    fun normalizeUuid_uppercase() {
        val normalized = PlayerProfileRepository.normalizeUuid("2A2ADC8BCBFB538934C34EDFA27081E9")
        assertEquals("2a2adc8b-cbfb-5389-34c3-4edfa27081e9", normalized)
    }

    @Test
    fun normalizeUuid_invalid_returnsLowercased() {
        val normalized = PlayerProfileRepository.normalizeUuid("not-a-uuid")
        assertEquals("not-a-uuid", normalized)
    }

    @Test
    fun stripDashes_removesDashes() {
        assertEquals("2a2adc8bcbfb538934c34edfa27081e9",
            PlayerProfileRepository.stripDashes("2a2adc8b-cbfb-5389-34c3-4edfa27081e9"))
    }
}
