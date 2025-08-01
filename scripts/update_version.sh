#!/bin/bash

# Monetai Android SDK Version Update Script
# Usage: ./scripts/update_version.sh <version>

set -e

if [ $# -eq 0 ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 1.0.0"
    exit 1
fi

VERSION=$1
echo "🔄 Updating Monetai Android SDK version to $VERSION..."

# Version format validation
if [[ ! $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+(-beta\.[0-9]+)?$ ]]; then
    echo "❌ Invalid version format: $VERSION"
    echo "Expected format: X.Y.Z or X.Y.Z-beta.N"
    exit 1
fi

# Update .version file
echo "📝 Updating .version file..."
echo "$VERSION" > .version
echo "✅ .version file updated to $VERSION."

# Update build.gradle file
echo "📝 Updating build.gradle file..."
if [ -f "monetai-sdk/build.gradle" ]; then
    # Read current version code from build.gradle and increment it
    CURRENT_VERSION_CODE=$(grep -o "versionCode [0-9]*" monetai-sdk/build.gradle | awk '{print $2}')
    if [ -z "$CURRENT_VERSION_CODE" ]; then
        echo "⚠️  Could not find versionCode in build.gradle, starting with 1"
        NEW_VERSION_CODE=1
    else
        NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
        echo "📊 Current versionCode: $CURRENT_VERSION_CODE → New versionCode: $NEW_VERSION_CODE"
    fi
    
    # Update versionName
    sed -i.bak "s/versionName \".*\"/versionName \"$VERSION\"/" monetai-sdk/build.gradle
    
    # Update versionCode with sequential number
    sed -i.bak "s/versionCode [0-9]*/versionCode $NEW_VERSION_CODE/" monetai-sdk/build.gradle
    
    # Remove backup file
    rm -f monetai-sdk/build.gradle.bak
    
    echo "✅ build.gradle file updated:"
    echo "   - versionName: $VERSION"
    echo "   - versionCode: $NEW_VERSION_CODE"
else
    echo "⚠️  monetai-sdk/build.gradle file not found."
fi

# Update SDKVersion.kt file
echo "📝 Updating SDKVersion.kt file..."
if [ -f "monetai-sdk/src/main/java/com/monetai/sdk/SDKVersion.kt" ]; then
    # Update VERSION constant
    sed -i.bak "s/private const val VERSION = \".*\"/private const val VERSION = \"$VERSION\"/" monetai-sdk/src/main/java/com/monetai/sdk/SDKVersion.kt
    
    # Remove backup file
    rm -f monetai-sdk/src/main/java/com/monetai/sdk/SDKVersion.kt.bak
    
    echo "✅ SDKVersion.kt file updated:"
    echo "   - VERSION constant: $VERSION"
else
    echo "⚠️  SDKVersion.kt file not found."
fi

echo ""
echo "🎉 Version update completed!"
echo "📋 Next steps:"
echo "   1. Review changes: git diff"
echo "   2. Commit changes: git add . && git commit -m 'chore: bump version to $VERSION'"
echo "   3. Create tag: git tag -a 'v$VERSION' -m 'Release version $VERSION'"
echo "   4. Push: git push origin main && git push origin 'v$VERSION'"
echo ""
echo "💡 Tip: Modifying the .version file and pushing to main branch will trigger automatic deployment." 