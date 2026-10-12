#!/usr/bin/env bash
set -euo pipefail

# Helper to generate an Android release signing keystore and output GitHub Secrets / keystore.properties

KEYSTORE_NAME="release.keystore"
ALIAS="workoutplanner"
VALIDITY_DAYS=10000

echo "=== WorkoutPlanner Release Keystore Generator ==="

if [ -f "$KEYSTORE_NAME" ]; then
    echo "Warning: $KEYSTORE_NAME already exists in current directory."
    read -p "Overwrite? (y/N): " -r CONFIRM
    if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 0
    fi
    rm "$KEYSTORE_NAME"
fi

read -sp "Enter Keystore Password (min 6 characters): " PASSWORD
echo ""
read -sp "Confirm Keystore Password: " PASSWORD_CONFIRM
echo ""

if [ "$PASSWORD" != "$PASSWORD_CONFIRM" ]; then
    echo "Error: Passwords do not match!"
    exit 1
fi

echo "Generating keystore with keytool..."
keytool -genkeypair \
    -v \
    -keystore "$KEYSTORE_NAME" \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity "$VALIDITY_DAYS" \
    -storepass "$PASSWORD" \
    -keypass "$PASSWORD" \
    -dname "CN=WorkoutPlanner, OU=Android, O=MawusiKpodo, C=US"

echo ""
echo "✅ Keystore created: $(pwd)/$KEYSTORE_NAME"

# Create local keystore.properties
cat > keystore.properties <<EOF
storeFile=$KEYSTORE_NAME
storePassword=$PASSWORD
keyAlias=$ALIAS
keyPassword=$PASSWORD
EOF

echo "✅ Created local 'keystore.properties' (already in .gitignore, never commit this file!)."
echo ""
echo "================================================================="
echo "  To configure automated GitHub Releases with this keystore:    "
echo "  Add the following Repository Secrets at:                      "
echo "  GitHub -> Repo Settings -> Secrets and variables -> Actions   "
echo "================================================================="
echo ""
echo "1. KEYSTORE_BASE64:"
base64 -w 0 "$KEYSTORE_NAME"
echo ""
echo ""
echo "2. KEYSTORE_PASSWORD:"
echo "$PASSWORD"
echo ""
echo "3. KEY_ALIAS:"
echo "$ALIAS"
echo ""
echo "4. KEY_PASSWORD:"
echo "$PASSWORD"
echo ""
echo "================================================================="
