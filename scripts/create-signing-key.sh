#!/usr/bin/env bash
# Creates YOUR private release signing key and keystore.properties (both git-ignored).
# Keep a backup of both files: updates must be signed with the same key.
set -euo pipefail

cd "$(dirname "$0")/.."

KEYSTORE="portalis-release.jks"
PROPERTIES="keystore.properties"
ALIAS="portalis"

if [[ -e "$KEYSTORE" || -e "$PROPERTIES" ]]; then
    echo "A signing key already exists ($KEYSTORE / $PROPERTIES). Not overwriting it." >&2
    exit 1
fi

if ! command -v keytool >/dev/null 2>&1; then
    echo "keytool not found. Install JDK 17 or newer and make sure it is on your PATH." >&2
    exit 1
fi

PASSWORD="$(od -An -tx1 -N24 /dev/urandom | tr -d ' \n')"

# PKCS12 uses one password for the store and the key.
keytool -genkeypair \
    -keystore "$KEYSTORE" -storetype PKCS12 -storepass "$PASSWORD" \
    -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 10000 \
    -dname "CN=Portalis TV self-build" >/dev/null

umask 077
cat >"$PROPERTIES" <<PROPS
storeFile=$KEYSTORE
storePassword=$PASSWORD
keyAlias=$ALIAS
keyPassword=$PASSWORD
PROPS
chmod 600 "$KEYSTORE" "$PROPERTIES"

echo "Created $KEYSTORE and $PROPERTIES."
echo "Back up both files somewhere safe. Never commit or share them."
