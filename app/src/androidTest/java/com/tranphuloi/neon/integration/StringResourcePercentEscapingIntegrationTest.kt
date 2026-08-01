package com.tranphuloi.neon.integration

import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tranphuloi.neon.App
import com.tranphuloi.neon.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Integration test — xác nhận Android resource system THẬT (aapt-compiled,
 * khác với raw XML text ở [com.tranphuloi.neon.StringResourcePercentEscapingTest])
 * resolve buff_*_desc / synergy_*_desc không còn "%%" literal, ở locale mặc
 * định lẫn locale ép buộc vi/en. Các string này render qua stringResource()
 * KHÔNG format args (SynergyBanner.kt, DialogBuffPicker.kt) nên "%%" trong XML
 * sẽ không được Android tự rút gọn lúc runtime.
 */
@RunWith(AndroidJUnit4::class)
class StringResourcePercentEscapingIntegrationTest {

    private val descKeys = listOf(
        R.string.buff_glass_cannon_desc,
        R.string.buff_sloth_desc,
        R.string.buff_reckless_desc,
        R.string.synergy_shield_weapon_desc,
        R.string.synergy_magnet_supercharge_desc,
        R.string.synergy_vampire_dmg_desc,
    )

    private fun assertResolvesWithSinglePercent(label: String, resolve: (Int) -> String) {
        descKeys.forEach { id ->
            val value = resolve(id)
            assertTrue("$label resource $id should contain a literal '%': $value", value.contains("%"))
            assertFalse("$label resource $id resolved with '%%' at runtime: $value", value.contains("%%"))
        }
    }

    @Test
    fun default_locale_resolves_without_double_percent() {
        val context = ApplicationProvider.getApplicationContext<App>()
        assertResolvesWithSinglePercent("default") { context.getString(it) }
    }

    @Test
    fun forced_english_locale_resolves_without_double_percent() {
        val base = ApplicationProvider.getApplicationContext<App>()
        val config = Configuration(base.resources.configuration).apply { setLocale(Locale.ENGLISH) }
        val enContext = base.createConfigurationContext(config)
        assertResolvesWithSinglePercent("en") { enContext.getString(it) }
    }

    @Test
    fun forced_vietnamese_locale_resolves_without_double_percent() {
        val base = ApplicationProvider.getApplicationContext<App>()
        val config = Configuration(base.resources.configuration).apply { setLocale(Locale("vi")) }
        val viContext = base.createConfigurationContext(config)
        assertResolvesWithSinglePercent("vi") { viContext.getString(it) }
    }
}
