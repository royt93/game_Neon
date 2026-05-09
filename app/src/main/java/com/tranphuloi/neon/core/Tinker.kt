package com.tranphuloi.neon.core

import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.common.Never
import com.tranphuloi.neon.ui.game.common.Once
import com.tranphuloi.neon.ui.game.common.RepeatTime

/**
 * Stores unique work id as key and start time in milliseconds as value.
 * Module-level so IDs survive Composable recomposition. Must be cleared on
 * game-state disposal (see [tinkerClearAll]) to avoid unbounded growth across
 * game restarts where new UUIDs are generated.
 */
private val tinkerMap = mutableMapOf<String, Long>()

/**
 *  Runs work for a given job. Each work has unique [id] and
 *  is stored in memory. Any new unique [doWork] will be invoked
 *  instantly when [tinker] is called, and depending on [repeatTime],
 *  will repeat periodically every [Millis], run [Once] or [Never].
 *
 *  @param id unique work ID
 *  @param repeatTime time after which work will be triggered
 *  @param doWork any periodic work
 */
fun tinker(id: String, repeatTime: RepeatTime, doWork: () -> Unit) {
    if (repeatTime is Never) return
    if (repeatTime is Once && tinkerMap.containsKey(id)) return
    if (!tinkerMap.containsKey(id)) {
        tinkerMap[id] = System.currentTimeMillis()
        doWork()
    }
    val value: Long? = tinkerMap[id]
    val elapseTimeMillis = value?.let { System.currentTimeMillis() - it } ?: -1
    if (repeatTime is Millis && elapseTimeMillis > repeatTime.timeMillis) {
        tinkerMap[id] = System.currentTimeMillis()
        doWork()
    }
}

/**
 * Wipes all stored work timestamps. Call on game-state disposal so the map
 * does not accumulate stale UUIDs across restarts (potential leak: each
 * `rememberSaveable { UUID.randomUUID().toString() }` adds a fresh entry).
 */
fun tinkerClearAll() {
    tinkerMap.clear()
}
