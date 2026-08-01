package com.tranphuloi.neon

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

/**
 * Task 33 audit fix — buff_*_desc / synergy_*_desc render via plain
 * stringResource(id) with no formatArgs (SynergyBanner.kt, DialogBuffPicker.kt),
 * so a literal '%' must stay single in the XML: Android only collapses '%%' to
 * '%' when the resource goes through String.format/getString(id, args).
 */
class StringResourcePercentEscapingTest {

    private val plainRenderedDescKeys = listOf(
        "buff_glass_cannon_desc",
        "buff_sloth_desc",
        "buff_reckless_desc",
        "synergy_shield_weapon_desc",
        "synergy_magnet_supercharge_desc",
        "synergy_vampire_dmg_desc",
    )

    private val localeFiles = listOf(
        "src/main/res/values/strings.xml",
        "src/main/res/values-en/strings.xml",
        "src/main/res/values-vi/strings.xml",
    )

    @Test
    fun `buff and synergy description strings have no double percent escaping`() {
        localeFiles.forEach { path ->
            val text = File(path).readText()
            plainRenderedDescKeys.forEach { key ->
                val match = Regex("""name="$key"[^>]*>([^<]*)<""").find(text)
                assertNotNull("$key missing from $path", match)
                val value = match!!.groupValues[1]
                assertFalse(
                    "$key in $path must not contain '%%' (rendered via plain stringResource with no formatArgs): $value",
                    value.contains("%%"),
                )
            }
        }
    }
}
