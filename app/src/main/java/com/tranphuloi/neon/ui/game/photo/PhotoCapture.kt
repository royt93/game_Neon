package com.tranphuloi.neon.ui.game.photo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Round 51 (26x Photo mode) — captures a View as a PNG to the app cache and
 * fires an Android Share intent with a FileProvider URI. No runtime permission
 * needed at any API level (we never touch external storage directly — the
 * sharing app gets a temporary read grant via [Intent.FLAG_GRANT_READ_URI_PERMISSION]).
 *
 * **Threading** (round 51 audit fix): `drawToBitmap` must run on Main (touches
 * the view tree); PNG compress + file write run on [Dispatchers.IO] (blocking
 * I/O, can take 200-500ms for a 1440×3120 screen); share intent goes back to
 * Main (Android requirement). `suspend fun` so the caller's `LaunchedEffect`
 * coroutine handles the dispatch hops cleanly.
 *
 * **Memory hygiene**: bitmap is recycled after compress so an 18 MB peak
 * doesn't linger waiting for GC.
 */
object PhotoCapture {

    /** Subdir under [Context.getCacheDir] where screenshots are written. */
    private const val SCREENSHOT_DIR = "screenshots"

    /**
     * Capture [view] (typically the activity's root window decor view) to a
     * PNG file and launch a chooser to share it. Returns true if the share
     * intent was launched, false on any failure.
     */
    suspend fun captureAndShare(context: Context, view: View): Boolean {
        // Step 1 (Main) — snapshot the rendered view tree.
        val bitmap: Bitmap = try {
            view.drawToBitmap()
        } catch (t: Throwable) {
            Logger.e("PhotoCapture.drawToBitmap FAILED", t)
            return false
        }

        // Step 2 (IO) — encode + write the PNG. Compress on a 1440×3120
        // bitmap takes ~200-500 ms; doing this on Main would visibly stall
        // recomposition and trigger ANR warnings on slow devices.
        val file: File = try {
            withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, SCREENSHOT_DIR).apply { mkdirs() }
                val out = File(dir, "neon_${System.currentTimeMillis()}.png")
                out.outputStream().use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                out
            }
        } catch (t: Throwable) {
            Logger.e("PhotoCapture.compress FAILED", t)
            return false
        } finally {
            // Round 51 audit — recycle the 18 MB bitmap immediately so the
            // peak doesn't linger waiting for GC. Safe because we've already
            // serialised the pixels to disk; nothing else holds a ref.
            if (!bitmap.isRecycled) bitmap.recycle()
        }

        // Step 3 (Main) — fire the share intent. ACTION_SEND requires running
        // on Main (Activity.startActivity contract).
        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            Logger.d("PhotoCapture.captureAndShare: wrote ${file.length()} bytes, uri=$uri")

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Chia sẻ ảnh chụp").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (t: Throwable) {
            Logger.e("PhotoCapture.startActivity FAILED", t)
            false
        }
    }
}
