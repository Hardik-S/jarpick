package com.batb4016.jarpick.domain

data class LimitDecision(
    val allowed: Boolean,
    val reason: String? = null,
)

object JarLimits {
    fun canCreateJar(currentJarCount: Int, isPremium: Boolean, freeLimit: Int = 3): LimitDecision {
        if (isPremium || currentJarCount < freeLimit) return LimitDecision(allowed = true)
        return LimitDecision(false, "Free JarPick supports up to $freeLimit jars. Upgrade for unlimited jars.")
    }

    fun canCreateOption(currentOptionCount: Int, isPremium: Boolean, freeLimit: Int = 25): LimitDecision {
        if (isPremium || currentOptionCount < freeLimit) return LimitDecision(allowed = true)
        return LimitDecision(false, "Free JarPick supports up to $freeLimit choices per jar.")
    }
}
