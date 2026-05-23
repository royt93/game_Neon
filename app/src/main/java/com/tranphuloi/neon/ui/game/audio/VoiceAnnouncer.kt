package com.tranphuloi.neon.ui.game.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.staticCompositionLocalOf
import com.tranphuloi.neon.utils.Logger
import java.util.Locale

/**
 * Round 62 (created) / Round 63 (enhanced) — Voice announcer via Android
 * TextToSpeech. Activity-scoped (same lifecycle as [AudioPlayerHolder] /
 * [SfxController]). Speaks short hype callouts on combo escalation, boss
 * kill, achievement unlock, and new best.
 *
 * Why TTS instead of pre-recorded voice clips: zero new assets, fully localizable
 * via existing strings.xml mechanism. Tradeoff: voice quality depends on device
 * TTS engine (Pixel/OEM devices ship Google TTS w/ Vietnamese; older/lower-end
 * devices may fall back to English or robotic voice). Player can toggle off in
 * Settings (`voiceAnnouncerEnabled`) if quality is poor on their hardware.
 *
 * Round 63 — combats the "robotic" feel with three knobs:
 *   1. Engine-wide base pitch 0.92 + rate 1.12 → less neutral, more announcer.
 *   2. Per-call [VoicePersonality] applies a delta on top of the base for
 *      different event types (HYPE / DRAMATIC / TRIUMPH / NORMAL). Combo callouts
 *      are higher + faster (excited), boss kill is deeper + slower (dramatic).
 *   3. Auto-pick the best available voice variant — Android exposes multiple
 *      voices per language with different quality + latency tiers. We prefer
 *      QUALITY_HIGH or VERY_HIGH and offline-capable variants.
 *
 * Throttle: 1500ms between utterances to avoid spam during rapid combo
 * escalation. Latest event wins (`speak(..., QUEUE_FLUSH)`).
 *
 * Locale: tries Vietnamese first, falls back to English if VI unavailable.
 * Caller passes resolved string from strings.xml (already locale-correct).
 */
class VoiceAnnouncer(private val appContext: Context) {

    private var tts: TextToSpeech? = null
    private var initialized: Boolean = false
    private var enabled: Boolean = true
    private var volume: Float = 1.0f
    private var lastUtteranceMillis: Long = 0L

    init {
        Logger.d("VoiceAnnouncer init: loading TTS engine")
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val viResult = tts?.setLanguage(Locale.forLanguageTag("vi-VN"))
                val fallback = viResult == TextToSpeech.LANG_MISSING_DATA ||
                    viResult == TextToSpeech.LANG_NOT_SUPPORTED
                if (fallback) {
                    val enResult = tts?.setLanguage(Locale.US)
                    Logger.d("VoiceAnnouncer: VI unavailable (code=$viResult), fallback to EN (code=$enResult)")
                } else {
                    Logger.d("VoiceAnnouncer: VI locale set OK")
                }
                // Round 63 — pick highest-quality offline voice for the active
                // locale. Some OEMs ship multiple Voice variants with different
                // quality tiers; default voice is often the lowest-latency
                // (most robotic). This swap nudges quality up where possible.
                pickBestVoice()
                // Round 63 — engine-wide tuning. Pitch 0.92 = slightly deeper
                // than neutral 1.0 (less feminine/robotic monotone). Rate 1.12 =
                // slightly faster than neutral (more energetic). Per-call
                // VoicePersonality applies a delta on top of these.
                tts?.setPitch(BASE_PITCH)
                tts?.setSpeechRate(BASE_RATE)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {}
                    @Deprecated("API < 21")
                    override fun onError(utteranceId: String?) {
                        Logger.w("VoiceAnnouncer utterance error id=$utteranceId")
                    }
                })
                initialized = true
            } else {
                Logger.w("VoiceAnnouncer: TTS init FAILED status=$status — voice announcer disabled")
            }
        }
    }

    /**
     * Round 63 — voice variant selection. Iterates the engine's available
     * Voice objects, filters to the current locale, and prefers:
     *   1. Higher QUALITY tier
     *   2. Offline (network not required)
     *   3. Lower LATENCY (faster response)
     *
     * Silently no-op if no candidates found or the engine doesn't expose Voice
     * API (very old devices). Logs the chosen voice for diagnostic.
     */
    private fun pickBestVoice() {
        val engine = tts ?: return
        val locale = engine.voice?.locale ?: Locale.forLanguageTag("vi-VN")
        runCatching {
            // Voice.QUALITY_VERY_LOW=100, QUALITY_LOW=200, NORMAL=300, HIGH=400, VERY_HIGH=500.
            // Exclude both VERY_LOW and LOW so we never fall back to obviously
            // bad variants when better ones exist; sort below picks best anyway.
            val candidates = engine.voices?.filter { v ->
                v.locale.language == locale.language &&
                    v.quality > Voice.QUALITY_LOW
            } ?: emptyList()
            if (candidates.isEmpty()) {
                Logger.d("VoiceAnnouncer: no candidate voices for ${locale.language} — keeping default")
                return
            }
            val best = candidates.sortedWith(
                compareByDescending<Voice> { it.quality }
                    .thenBy { it.isNetworkConnectionRequired }
                    .thenBy { it.latency }
            ).first()
            val result = engine.setVoice(best)
            Logger.d(
                "VoiceAnnouncer: picked voice='${best.name}' quality=${best.quality} " +
                    "latency=${best.latency} network=${best.isNetworkConnectionRequired} result=$result"
            )
        }.onFailure { Logger.w("VoiceAnnouncer.pickBestVoice failed: ${it.message}") }
    }

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        Logger.d("VoiceAnnouncer.setEnabled $value")
        if (!enabled) tts?.stop()
    }

    fun setVolume(percent: Int) {
        volume = (percent.coerceIn(0, 100)) / 100f
        Logger.v { "VoiceAnnouncer.setVolume percent=$percent → vol=$volume" }
    }

    /**
     * Round 63 — speak [text] with optional [personality] prosody adjustment.
     * Per-personality pitch+rate delta applied just before [TextToSpeech.speak].
     * After speak the engine retains the changed pitch/rate until the next
     * personality change — we always reset to personality-appropriate values
     * so back-to-back differing events read correctly.
     *
     * No-op when disabled, TTS not yet initialized, or last utterance fired
     * within [throttleMillis] ms (default 1500). Cancels any in-flight
     * utterance via QUEUE_FLUSH so the most recent event wins.
     */
    fun announce(
        text: String,
        personality: VoicePersonality = VoicePersonality.NORMAL,
        throttleMillis: Long = 1500L,
    ) {
        if (!enabled) return
        val now = System.currentTimeMillis()
        if (now - lastUtteranceMillis < throttleMillis) {
            Logger.v { "VoiceAnnouncer.announce SKIPPED (throttle): '$text'" }
            return
        }
        val engine = tts ?: return
        if (!initialized) {
            Logger.v { "VoiceAnnouncer.announce SKIPPED (not init): '$text'" }
            return
        }
        lastUtteranceMillis = now
        // Per-personality prosody. Engine call is cheap (no allocation), takes
        // effect on the very next speak() call. Coerce to safe range so we
        // don't push beyond what the synthesizer can render cleanly.
        val pitch = (BASE_PITCH + personality.pitchDelta).coerceIn(0.5f, 1.5f)
        val rate = (BASE_RATE + personality.rateDelta).coerceIn(0.5f, 1.6f)
        engine.setPitch(pitch)
        engine.setSpeechRate(rate)
        val params = android.os.Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
        }
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "neon-${now}")
        Logger.d(
            "VoiceAnnouncer.announce '$text' personality=$personality " +
                "pitch=$pitch rate=$rate result=$result vol=$volume"
        )
    }

    fun release() {
        Logger.d("VoiceAnnouncer.release: shutdown TTS")
        // Defensive teardown order matters: clear the progress listener BEFORE
        // shutdown so any in-flight onDone/onError can't fire against a
        // half-destroyed VoiceAnnouncer reference (some OEM TTS engines deliver
        // utterance callbacks on a worker thread that races with shutdown).
        runCatching { tts?.setOnUtteranceProgressListener(null) }
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        initialized = false
        enabled = false
    }

    companion object {
        // Round 64 — pivoted to alien/Darth-Vader voice per user feedback
        // ("voice tệ quá robot" → "muốn nghe như người ngoài hành tinh").
        // BASE_PITCH 0.65 = very deep (Darth Vader chest voice register).
        // BASE_RATE 0.85 = slow + ominous (sci-fi villain pacing).
        //
        // Effective per personality (BASE + delta):
        //   HYPE      → pitch 0.80 / rate 1.05 (alien combo callout, still recognizable)
        //   DRAMATIC  → pitch 0.55 / rate 0.70 (DEEPEST + slowest — boss kill gravitas)
        //   TRIUMPH   → pitch 0.73 / rate 0.80 (slightly higher alien, celebration)
        //   NORMAL    → pitch 0.65 / rate 0.85 (baseline alien)
        //
        // Engine pitch range is generally 0.5-2.0. 0.55 (DRAMATIC) is near the
        // floor — may sound muffled on cheap OEM TTS engines but renders cleanly
        // on Google TTS. Coerced inside [announce] to [0.5, 1.5] for safety.
        const val BASE_PITCH: Float = 0.65f
        const val BASE_RATE: Float = 0.85f
    }
}

/**
 * Round 63 — prosody personality. Each maps to a pitch/rate delta applied on
 * top of the engine-wide BASE_PITCH / BASE_RATE before [TextToSpeech.speak].
 *
 * - HYPE: combo callouts. Higher + faster = excitement.
 * - DRAMATIC: boss kill. Lower + slower = gravitas.
 * - TRIUMPH: achievement / new best. Higher + medium pace = celebration.
 * - NORMAL: fallback / neutral events.
 */
enum class VoicePersonality(val pitchDelta: Float, val rateDelta: Float) {
    HYPE(pitchDelta = +0.15f, rateDelta = +0.20f),
    DRAMATIC(pitchDelta = -0.10f, rateDelta = -0.15f),
    TRIUMPH(pitchDelta = +0.08f, rateDelta = -0.05f),
    NORMAL(pitchDelta = 0f, rateDelta = 0f),
}

val LocalVoiceAnnouncer = staticCompositionLocalOf<VoiceAnnouncer> {
    error("VoiceAnnouncer not provided. Wrap in CompositionLocalProvider(LocalVoiceAnnouncer provides ...).")
}
