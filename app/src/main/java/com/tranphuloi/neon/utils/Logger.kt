package com.tranphuloi.neon.utils

import android.util.Log

object Logger {

    private const val PREFIX = "roy93~"
    private const val DEFAULT_TAG = "Neon"

    fun d(message: String) {
        Log.d(DEFAULT_TAG, "$PREFIX $message")
    }

    fun d(tag: String, message: String) {
        Log.d(tag, "$PREFIX $message")
    }

    fun w(message: String, throwable: Throwable? = null) {
        Log.w(DEFAULT_TAG, "$PREFIX $message", throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(DEFAULT_TAG, "$PREFIX $message", throwable)
    }
}
