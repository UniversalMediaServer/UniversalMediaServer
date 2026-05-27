# This script should run on an ARM Mac.
#
# It builds the "arm" releases. They will also be
# signed/notarized so that macOS does not complain about it as much to users.
#
# It requires you to copy the gon-config-prebuild.json and gon-config-build-arm.json files from
# the ./dependencies directory into the root (one higher than here) directory, and populate
# them with your Apple Developer credentials.
# You also need to bump the versions in those files before each release. That should probably
# be automated later.

cd ..

# Clear the folder for a clean build
rm UMS-macOS-*
rm -rf target

mvn -P macos-arm package -DskipTests=true

./scripts/dependencies/gon ./gon-config-prebuild.json
./scripts/dependencies/gon ./gon-config-build-arm.json
