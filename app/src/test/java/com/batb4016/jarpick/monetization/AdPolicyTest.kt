package com.batb4016.jarpick.monetization

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPolicyTest {
    @Test fun premiumHidesAds() {
        assertFalse(shouldShowJarPickBanner(isPremium = true, surface = JarPickAdSurface.JarList, adUnitId = "test"))
    }

    @Test fun freeUserCanSeeAdsOnAllowedSurfaces() {
        assertTrue(shouldShowJarPickBanner(isPremium = false, surface = JarPickAdSurface.JarList, adUnitId = "test"))
    }
}
