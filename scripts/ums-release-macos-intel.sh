# This script should run on an Intel Mac.
#
# It builds the "pre-10.15" and default macOS releases. The default release will also be
# signed/notarized so that macOS does not complain about it as much to users.
#
# It also does the Docker Hub release which uses Alpine Linux.
#
# It requires you to copy the gon-config-prebuild.json and gon-config-build-intel.json files from
# the ./dependencies directory into the root (one higher than here) directory, and populate
# them with your Apple Developer credentials.
# You also need to bump the versions in those files before each release. That should probably
# be automated later.
#
# The Docker part requires permission to push to universalmediaserver/ums.

cd ..

# Clear the folder for a clean build
rm -rf target

mvn -P macos package -DskipTests=true

# Uncomment to publish to Docker manually. Currently it happens via GitHub Actions.
# mvn -P docker prepare-package -DskipTests=true
# mvn -P docker package -DskipTests=true
# mvn -P docker install -DskipTests=true

./scripts/dependencies/gon ./gon-config-prebuild.json
./scripts/dependencies/gon ./gon-config-build-intel.json

# Clear the folder for a clean build
rm -rf target

mvn -P macos-pre1015 package -DskipTests=true

hdiutil create -volname "Universal Media Server" -srcfolder target/ums-*-distribution -fs HFS+ UMS-macOS-14.1.0-pre10.15.dmg
