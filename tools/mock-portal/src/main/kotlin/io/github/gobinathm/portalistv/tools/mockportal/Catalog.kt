package io.github.gobinathm.portalistv.tools.mockportal

/** A channel served by the mock portal. */
data class MockChannel(val id: Int, val name: String, val genreId: String, val streamUrl: String)

data class MockGenre(val id: String, val title: String, val censored: Boolean = false)

/**
 * The mock portal's content. Every stream is freely licensed and published for testing:
 * Big Buck Bunny is (c) Blender Foundation, CC BY 3.0; the Apple and Mux streams are public
 * HLS test streams. No third-party TV channel is referenced.
 */
class Catalog(
    /** Filler channels (and as many filler movies); the default spans several pages, more stress long lists. */
    fillerChannels: Int = DEFAULT_FILLER_CHANNELS,
) {
    private companion object {
        const val DEFAULT_FILLER_CHANNELS = 30
        const val BIG_BUCK_BUNNY = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        const val APPLE_BIPBOP =
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8"
        const val APPLE_ADVANCED =
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8"
    }

    val genres = listOf(
        MockGenre("1", "Test streams"),
        MockGenre("2", "Movies"),
        MockGenre("3", "Filler"),
        MockGenre("4", "Adult (empty)", censored = true),
        // Named like the language categories of real portals, so their tiles can be checked.
        MockGenre("5", "HINDI"),
        MockGenre("6", "TAMIL MOVIES"),
        MockGenre("7", "Bangla News"),
    )

    val vodCategories = listOf("10" to "Films", "11" to "Series", "12" to "Filler movies")

    /** On-demand items in the order they were "added"; the portal lists the newest first. */
    val vod: List<MockVod> = buildList {
        add(MockVod(1, "Big Buck Bunny", "10", BIG_BUCK_BUNNY))
        add(MockVod(2, "Bip-Bop", "10", APPLE_BIPBOP))
        add(MockVod(3, "Test Pattern Stories", "11", APPLE_ADVANCED, seasons = listOf(3, 2)))
        val digits = fillerChannels.toString().length.coerceAtLeast(2)
        repeat(fillerChannels) { i ->
            add(MockVod(size + 1, "Filler movie %0${digits}d".format(i + 1), "12", BIG_BUCK_BUNNY))
        }
    }

    val channels: List<MockChannel> = buildList {
        add(MockChannel(1, "Apple Bip-Bop", "1", APPLE_BIPBOP))
        add(MockChannel(2, "Apple Advanced Stream", "1", APPLE_ADVANCED))
        add(MockChannel(3, "Big Buck Bunny", "2", BIG_BUCK_BUNNY))
        val digits = fillerChannels.toString().length.coerceAtLeast(2)
        repeat(fillerChannels) { i ->
            val number = size + 1
            val stream = if (i % 2 == 0) APPLE_BIPBOP else BIG_BUCK_BUNNY
            add(MockChannel(number, "Filler %0${digits}d".format(i + 1), "3", stream))
        }
        add(MockChannel(size + 1, "Language Test One", "5", BIG_BUCK_BUNNY))
        add(MockChannel(size + 1, "Language Test Two", "6", APPLE_BIPBOP))
        add(MockChannel(size + 1, "Language Test Three", "7", BIG_BUCK_BUNNY))
    }
}
