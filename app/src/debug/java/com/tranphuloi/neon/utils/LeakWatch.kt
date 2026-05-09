package com.tranphuloi.neon.utils

import leakcanary.AppWatcher

/**
 * Debug variant: hands the object to LeakCanary's ObjectWatcher with an
 * `expectWeaklyReachable` claim. If the object isn't GC'd within a few seconds,
 * LeakCanary triggers a heap dump and logs the retention path.
 */
object LeakWatch {
    fun watch(obj: Any, description: String) {
        Logger.d("LeakWatch.watch [$description] obj=${obj::class.java.simpleName}")
        AppWatcher.objectWatcher.expectWeaklyReachable(obj, description)
    }
}
