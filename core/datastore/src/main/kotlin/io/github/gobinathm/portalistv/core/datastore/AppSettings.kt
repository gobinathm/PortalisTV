package io.github.gobinathm.portalistv.core.datastore

import io.github.gobinathm.portalistv.core.player.PlayerSettings
import io.github.gobinathm.portalistv.core.security.PinHash

/** Settings that apply to every profile. */
data class AppSettings(
    val player: PlayerSettings = PlayerSettings(),
    /** Keep playing in a small window when the user presses Home during playback. */
    val pictureInPicture: Boolean = true,
    /** Show categories and channels the portal marks as adult. Off until the parental PIN exists (M7). */
    val showAdultContent: Boolean = false,
    /** The profile opened at start when there are several; a single profile always opens. Null shows the list. */
    val startProfileUuid: String? = null,
    val theme: AppTheme = AppTheme.DARK,
    val accent: AccentColor = AccentColor.AZURE,
    /** Home's rows in order; always lists every [HomeRow] once. */
    val homeRows: List<HomeRowSetting> = normalizedHomeRows(emptyList()),
    /** Remote button (Android key code) to what it does during playback. */
    val keyBindings: Map<Int, PlayerAction> = RemoteKeys.DEFAULT,
    val parental: ParentalSettings = ParentalSettings(),
)

data class ParentalSettings(
    /** Null when no parental PIN is set. */
    val pin: PinHash? = null,
    /** Also ask for the PIN before Settings and profile editing. */
    val lockSettings: Boolean = false,
)
