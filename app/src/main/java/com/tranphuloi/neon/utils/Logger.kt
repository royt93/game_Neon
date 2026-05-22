package com.tranphuloi.neon.utils

import android.util.Log
import com.tranphuloi.neon.BuildConfig

object Logger {

    // Round 44 — public so Logger.v (inline) can reference them. The inline
    // function compiles into call sites which can't access private members.
    const val PREFIX = "roy93~"
    const val DEFAULT_TAG = "Neon"

    /**
     * Round 44 — toggle verbose hot-path logging. Default `false` so combat
     * peaks don't pump 50-100 logs/sec through `Log.d` (logcat ring buffer is a
     * mutex-guarded JNI call; the string concat per call also pressures GC).
     * Flip to `true` only while debugging a specific hot path.
     */
    const val VERBOSE: Boolean = false

    fun d(message: String) {
        if (!BuildConfig.DEBUG) return
        Log.d(DEFAULT_TAG, "$PREFIX $message")
    }

    fun d(tag: String, message: String) {
        if (!BuildConfig.DEBUG) return
        Log.d(tag, "$PREFIX $message")
    }

    /**
     * Round 44 — verbose hot-path log. Use for per-event/per-frame logging
     * (collisions, kills, status effects, audio micro-steps, etc.). The
     * `inline` + lambda message means the string is built ONLY when both
     * BuildConfig.DEBUG AND VERBOSE are true — zero-allocation when off.
     *
     * Call sites that fire many times per second MUST use this instead of
     * [d] so combat doesn't spam logcat / pump GC pressure.
     */
    inline fun v(message: () -> String) {
        if (!BuildConfig.DEBUG || !VERBOSE) return
        Log.d(DEFAULT_TAG, "$PREFIX ${message()}")
    }

    fun w(message: String, throwable: Throwable? = null) {
        Log.w(DEFAULT_TAG, "$PREFIX $message", throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(DEFAULT_TAG, "$PREFIX $message", throwable)
    }
}
