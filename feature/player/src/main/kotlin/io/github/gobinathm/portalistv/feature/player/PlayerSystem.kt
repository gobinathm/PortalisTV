package io.github.gobinathm.portalistv.feature.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.net.toUri
import androidx.core.util.Consumer
import io.github.gobinathm.portalistv.core.player.MediaSource

private val VideoAspect = Rational(16, 9)

/**
 * Moves playback into a picture-in-picture window when the user presses Home, if [enabled] and the
 * device supports it. @return whether the screen is currently shown in that window.
 */
@Composable
internal fun pictureInPicture(enabled: Boolean): Boolean {
    val activity = LocalContext.current.findActivity() ?: return false
    // The video fills the screen, so the whole view is what shrinks into the window.
    val view = LocalView.current
    var inPip by remember { mutableStateOf(activity.isInPictureInPictureMode) }
    val supported = remember(activity) {
        activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    DisposableEffect(activity) {
        val listener = Consumer<PictureInPictureModeChangedInfo> { inPip = it.isInPictureInPictureMode }
        activity.addOnPictureInPictureModeChangedListener(listener)
        onDispose { activity.removeOnPictureInPictureModeChangedListener(listener) }
    }
    DisposableEffect(activity, enabled && supported) {
        val active = enabled && supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.setPictureInPictureParams(pipParams(view, autoEnter = active))
            onDispose { activity.setPictureInPictureParams(pipParams(view, autoEnter = false)) }
        } else {
            val enter = Runnable { if (active) activity.enterPictureInPictureMode(pipParams(view, autoEnter = false)) }
            activity.addOnUserLeaveHintListener(enter)
            onDispose { activity.removeOnUserLeaveHintListener(enter) }
        }
    }
    return inPip
}

private fun pipParams(view: View, autoEnter: Boolean): PictureInPictureParams = PictureInPictureParams.Builder()
    .setAspectRatio(VideoAspect)
    .setSourceRectHint(Rect().also { view.getGlobalVisibleRect(it) })
    .apply { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setAutoEnterEnabled(autoEnter) }
    .build()

/**
 * Opens [media] in the user's video player app. The user agent goes in the "headers" extra that
 * common players read; others ignore it. @return false when no installed app can play it.
 */
internal fun launchExternalPlayer(context: Context, media: MediaSource): Boolean {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(media.url.toUri(), "video/*")
        .putExtra("title", media.title)
        .apply { media.userAgent?.let { putExtra("headers", arrayOf("User-Agent", it)) } }
        .apply { if (context.findActivity() == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is Activity -> null
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
