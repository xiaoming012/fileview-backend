#!/bin/bash
set -euo pipefail

echo "======================================"
echo "   FileView Backend Build Script"
echo "======================================"

# ---------------------------------
# Detect CI environment
# ---------------------------------
if [ "${CI:-}" = "true" ]; then
    echo "🚀 Running in CI mode (no sudo)"
    SUDO=""
else
    if [ "$EUID" -eq 0 ]; then
        SUDO=""
    else
        SUDO="sudo"
    fi
fi

WORKSPACE=$(pwd)
DEPLOY_ROOT="$WORKSPACE/.release/opt/fileview"

echo "Workspace: $WORKSPACE"
echo "Deployment directory: $DEPLOY_ROOT"
echo ""

# ---------------------------------
# Clean release directory
# ---------------------------------
if [ -d "$DEPLOY_ROOT" ]; then
    echo "🗑️ Cleaning existing deployment directory..."
    rm -rf "$DEPLOY_ROOT"
fi

mkdir -p "$DEPLOY_ROOT"

# ---------------------------------
# Build stage
# ---------------------------------
echo "🔨 Building project..."

chmod +x ./mvnw

RELEASE_VERSION=${RELEASE_VERSION:-$(cat version.txt)}
echo "Release version: $RELEASE_VERSION"

./mvnw clean package -DskipTests -DreleaseVersion="$RELEASE_VERSION"

echo "✅ Maven build complete"
echo ""

# ---------------------------------
# Validate build outputs
# ---------------------------------
echo "🔍 Checking build outputs..."

ls -lh fileview-preview/target/lib/fileview-preview.jar
ls -lh fileview-convert/target/lib/fileview-convert.jar

echo "Preview config:"
ls -la fileview-preview/target/config/

echo "Convert config:"
ls -la fileview-convert/target/config/

echo "✅ Build artifacts verified"
echo ""

# ---------------------------------
# Create directory structure
# ---------------------------------
echo "📁 Creating directory structure..."

mkdir -p "$DEPLOY_ROOT"/{bin,config,lib,logs,data,resources}
mkdir -p "$DEPLOY_ROOT"/config/{preview,convert}
mkdir -p "$DEPLOY_ROOT"/lib/{preview,convert}

echo "✅ Directory structure ready"
echo ""

# ---------------------------------
# Copy files
# ---------------------------------
echo "📦 Copying preview service..."
cp fileview-preview/target/lib/fileview-preview.jar \
   "$DEPLOY_ROOT/lib/preview/"

cp fileview-preview/target/config/* \
   "$DEPLOY_ROOT/config/preview/"

echo "📦 Copying convert service..."
cp fileview-convert/target/lib/fileview-convert.jar \
   "$DEPLOY_ROOT/lib/convert/"

cp fileview-convert/target/config/* \
   "$DEPLOY_ROOT/config/convert/"

echo "📦 Copying scripts..."
cp start-convert-service.sh "$DEPLOY_ROOT/bin/"
cp start-preview-service.sh "$DEPLOY_ROOT/bin/"
cp stop-preview-service.sh "$DEPLOY_ROOT/bin/"
cp stop-convert-service.sh "$DEPLOY_ROOT/bin/"
cp init-rocketmq-topics.sh "$DEPLOY_ROOT/bin/"

echo "✅ Files copied"
echo ""

# ---------------------------------
# Set permissions
# ---------------------------------
echo "🔐 Setting permissions..."

chmod 644 "$DEPLOY_ROOT"/lib/*/*.jar
chmod 644 "$DEPLOY_ROOT"/config/*/* 2>/dev/null || true
chmod +x "$DEPLOY_ROOT"/bin/*.sh

echo "✅ Permissions set"
echo ""

echo "======================================"
echo "   ✅ Deployment directory ready"
echo "======================================"
echo ""
echo "Location:"
echo "  $DEPLOY_ROOT"
echo ""