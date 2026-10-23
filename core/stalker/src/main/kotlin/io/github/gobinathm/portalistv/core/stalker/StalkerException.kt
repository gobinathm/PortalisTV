package io.github.gobinathm.portalistv.core.stalker

import java.io.IOException

/** Base type for everything the portal client reports. Messages never contain secrets. */
sealed class StalkerException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** The portal URL is not usable: unparseable, or plain HTTP without the profile's opt-in. */
class InvalidPortalUrlException(message: String) : StalkerException(message)

/** None of the candidate endpoints answered like a Stalker portal. */
class PortalNotFoundException(cause: Throwable? = null) : StalkerException("No Stalker portal found", cause)

/** The portal rejected the session even after a fresh handshake. */
class AuthorizationException(message: String = "Authorization failed") : StalkerException(message)

/** The portal knows the device but refuses service (for example, an unregistered MAC address). */
class AccountBlockedException(val status: Int, val portalMessage: String?) :
    StalkerException("Portal refused the device (status $status)")

/** The response was not what the protocol promises: wrong shape, too large, or an HTTP error. */
class ProtocolException(message: String, cause: Throwable? = null) : StalkerException(message, cause)
