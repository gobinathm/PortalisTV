package io.github.gobinathm.portalistv.player.vlc

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import io.github.gobinathm.portalistv.core.common.log.Log
import java.io.File
import java.io.IOException

/**
 * Gives libVLC's subtitle renderer a small set of fonts.
 *
 * Without a fontconfig file, libVLC indexes every system font on first use, glyph by glyph. On
 * TVs with large CJK collections that takes long enough to stall the video thread. This links a
 * dozen system fonts (Latin plus the main Indic scripts and Arabic) into a private folder and
 * points fontconfig at it, with a cache, so captions and text subtitles render without the stall.
 */
internal object VlcFonts {
    private const val TAG = "VlcFonts"
    private const val SYSTEM_FONTS = "/system/fonts"

    private val wanted = Regex(
        "(Roboto-Regular|NotoNaskhArabic-Regular|" +
            "NotoSans(Tamil|Devanagari|Bengali|Telugu|Kannada|Malayalam|Gujarati|Gurmukhi|Sinhala)-(VF|Regular))\\.ttf",
    )

    private var ready: Boolean? = null

    /** @return true when text can be rendered without the full font scan. Runs once per process. */
    @Synchronized
    fun prepare(context: Context): Boolean = ready ?: setUp(context).also { ready = it }

    private fun setUp(context: Context): Boolean = try {
        val root = File(context.noBackupFilesDir, "vlc-fonts")
        val fontsDir = File(root, "fonts")
        fontsDir.deleteRecursively()
        check(fontsDir.mkdirs()) { "Can't create $fontsDir" }
        val fonts = File(SYSTEM_FONTS).listFiles { file -> wanted.matches(file.name) }.orEmpty()
        fonts.forEach { Os.symlink(it.canonicalPath, File(fontsDir, it.name).path) }
        if (fonts.isEmpty()) {
            Log.w(TAG) { "No known fonts in $SYSTEM_FONTS; text subtitles stay off" }
            false
        } else {
            val config = File(root, "fonts.conf")
            config.writeText(
                """
                <?xml version="1.0"?>
                <fontconfig>
                  <dir>${fontsDir.path}</dir>
                  <cachedir>${File(context.cacheDir, "fontconfig").path}</cachedir>
                </fontconfig>
                """.trimIndent(),
            )
            Os.setenv("FONTCONFIG_FILE", config.path, true)
            Log.i(TAG) { "Text subtitles use ${fonts.size} fonts" }
            true
        }
    } catch (e: IOException) {
        Log.w(TAG, e) { "Font setup failed; text subtitles stay off" }
        false
    } catch (e: ErrnoException) {
        Log.w(TAG, e) { "Font setup failed; text subtitles stay off" }
        false
    } catch (e: IllegalStateException) {
        Log.w(TAG, e) { "Font setup failed; text subtitles stay off" }
        false
    }
}
