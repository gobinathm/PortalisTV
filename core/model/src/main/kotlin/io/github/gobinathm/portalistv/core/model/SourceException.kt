package io.github.gobinathm.portalistv.core.model

import java.io.IOException

/** Why a [ContentSource] call failed, in terms a screen can explain to the user. */
class SourceException(val reason: Reason, val portalMessage: String? = null, cause: Throwable? = null) :
    IOException(reason.name, cause) {
    enum class Reason {
        /** The address cannot be used, or is plain HTTP without the profile's opt-in. */
        INVALID_ADDRESS,

        /** Nothing at the address answered like a portal. */
        NOT_FOUND,

        /** The portal knows the device but refuses it, e.g. an unregistered MAC address. */
        DEVICE_REFUSED,

        /** The portal rejected the session. */
        AUTHORIZATION,

        /** The portal answered with something unexpected. */
        BAD_RESPONSE,
    }
}
