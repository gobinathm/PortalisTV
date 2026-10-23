package io.github.gobinathm.portalistv.core.model

/** Profiles that exercise every column; values are fake. */
object TestProfiles {
    fun minimal(uuid: String = "6f1c1f0e-0000-4000-8000-000000000001") = Profile(
        uuid = uuid,
        name = "Minimal",
        createdAtMillis = 1_700_000_000_000,
        portal = Portal(url = Secret("https://portal.invalid/c/")),
        device = DeviceIdentity(stbModel = "MAG250", mac = Secret("00:1A:79:00:00:01")),
    )

    fun full(uuid: String = "6f1c1f0e-0000-4000-8000-000000000002") = Profile(
        uuid = uuid,
        name = "Full",
        sortOrder = 3,
        color = 0xFF3D7BFD.toInt(),
        createdAtMillis = 1_700_000_000_123,
        portal = Portal(
            url = Secret("http://portal.invalid:8080/stalker_portal/c/"),
            language = "de",
            timezone = "Europe/Berlin",
            allowInsecureHttp = true,
            login = Secret("user"),
            password = Secret("pass"),
        ),
        proxy = Proxy(enabled = true, host = "proxy.invalid", port = 3128, Secret("pu"), Secret("pp")),
        device = DeviceIdentity(
            stbModel = "MAG424",
            userAgent = "Custom/1.0",
            displayResolution = "3840x2160",
            mac = Secret("00:1A:79:00:00:02"),
            serial = Secret("SN0000000002"),
        ),
        deviceIds = DeviceIds(
            sendDeviceId = true,
            deviceId = Secret("A".repeat(64)),
            deviceIdSeed = Secret("seed"),
            macBasedDeviceId = true,
            deviceId2 = Secret("B".repeat(64)),
            signature = Secret("C".repeat(64)),
        ),
        firmware = Firmware(
            presetId = null,
            playerEngineVersion = "0x58c",
            jsApiVersion = "343",
            stbApiVersion = "146",
            imageVersion = "218",
            imageDescription = "0.2.18-r23-424",
            imageDate = "Thu Jan 1 00:00:00 UTC 2026",
        ),
        hardware = Hardware(vendor = "Vendor", version = "1.7-BD-00"),
        compatibility = Compatibility(ministraMode = true, allowEmulatorDetection = true),
        playback = Playback(
            engine = PlayerEngine.EXTERNAL,
            udpxyEnabled = true,
            udpxyUrl = "http://udpxy.invalid:4022",
            udpxyProtocol = MulticastProtocol.RTP,
            externalPlayerKeys = ExternalPlayerKeys(back = true, exit = true, ok = true),
        ),
    )
}
