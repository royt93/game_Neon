package com.tranphuloi.neon.utils

/**
 * Release variant: no-op. LeakCanary is intentionally not on the release classpath.
 */
object LeakWatch {
    fun watch(obj: Any, description: String) {
        // intentional no-op
    }
}
