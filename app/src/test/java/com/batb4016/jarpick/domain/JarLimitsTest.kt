package com.batb4016.jarpick.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarLimitsTest {
    @Test fun freeJarLimitIsEnforced() {
        assertFalse(JarLimits.canCreateJar(currentJarCount = 3, isPremium = false).allowed)
    }

    @Test fun freeOptionLimitIsEnforced() {
        assertFalse(JarLimits.canCreateOption(currentOptionCount = 25, isPremium = false).allowed)
    }

    @Test fun premiumBypassesFreeLimits() {
        assertTrue(JarLimits.canCreateJar(currentJarCount = 100, isPremium = true).allowed)
        assertTrue(JarLimits.canCreateOption(currentOptionCount = 100, isPremium = true).allowed)
    }
}
