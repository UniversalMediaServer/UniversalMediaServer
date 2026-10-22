#!/bin/sh
# AppBundler expects Contents/Home, Contents/MacOS and Contents/Info.plist.
# SDKMAN and some CI JDK installations provide only the Home directory.
set -eu

SOURCE_HOME=$1
BUNDLE=$2
VERSION=$("$SOURCE_HOME/bin/java" -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*java.specification.version = //p')
if [ "$VERSION" != "25" ]; then
    echo "macOS packaging requires JDK 25: $SOURCE_HOME" >&2
    exit 1
fi

if [ -f "$SOURCE_HOME/../Info.plist" ] && [ -d "$SOURCE_HOME/../MacOS" ]; then
    printf '%s\n' "$SOURCE_HOME"
    exit 0
fi

if [ ! -f "$SOURCE_HOME/lib/libjli.dylib" ]; then
    echo "The selected JDK is not a macOS runtime: $SOURCE_HOME" >&2
    exit 1
fi

# BUNDLE is a generated directory under Maven's target directory.
rm -rf "$BUNDLE"
mkdir -p "$BUNDLE/Contents/Home" "$BUNDLE/Contents/MacOS"
cp -R "$SOURCE_HOME/." "$BUNDLE/Contents/Home/"
cp -p "$SOURCE_HOME/lib/libjli.dylib" "$BUNDLE/Contents/MacOS/libjli.dylib"
cat > "$BUNDLE/Contents/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
    <key>CFBundleExecutable</key><string>libjli.dylib</string>
    <key>CFBundleIdentifier</key><string>net.pms.java25</string>
    <key>CFBundleInfoDictionaryVersion</key><string>6.0</string>
    <key>CFBundleName</key><string>UMS Java 25 Runtime</string>
    <key>CFBundlePackageType</key><string>BNDL</string>
    <key>CFBundleVersion</key><string>25</string>
    <key>JavaVM</key><dict>
        <key>JVMCapabilities</key><array><string>JNI</string><string>CommandLine</string></array>
        <key>JVMPlatformVersion</key><string>25</string>
        <key>JVMVersion</key><string>25</string>
    </dict>
</dict></plist>
PLIST
printf '%s\n' "$BUNDLE/Contents/Home"
