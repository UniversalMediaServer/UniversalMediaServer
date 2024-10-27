#!/bin/sh

set -e

export ARCH="$(uname -m)"
export APPIMAGE_EXTRACT_AND_RUN=1
export VERSION=$GITHUB_REF_NAME

APPIMAGETOOL="https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage"
UPINFO="gh-releases-zsync|$GITHUB_REPOSITORY_OWNER|UniversalMediaServer|latest|*$ARCH.AppImage.zsync"

# wget "https://github.com/UniversalMediaServer/UniversalMediaServer/releases/download/14.5.1/UMS-14.5.1-x86_64.tgz"
cd target
tar fx ./*.tgz

cd ./ums*
cp ./web/react-client/icon-256.png ./ums.png
ln -s ./UMS.png ./.DirIcon
echo '[Desktop Entry]
Version=1.0
Name=Universal Media Server
Comment=A DLNA-compliant UPnP Media Server.
Exec=UMS.sh
Icon=ums
Terminal=false
Type=Application
Categories=Java;AudioVideo;' > ums.desktop
ln -s ./UMS.sh ./AppRun
cd ..

wget "$APPIMAGETOOL" -O ./appimagetool

chmod +x ./appimagetool

mv ./ums* ./ums.AppDir

./appimagetool --comp zstd \
	--mksquashfs-opt -Xcompression-level --mksquashfs-opt 22 \
	-n -u "$UPINFO" ./ums.AppDir UMS-"$VERSION"-"$ARCH".AppImage