# Build UMS binaries

These scripts are only meant for enthusiasts who want to bundle their UMS with
custom built versions of libraries and tools, replacing the standard versions
shipped with the regular UMS distribution.

There are three scripts available: the first for downloading the sources, the
second for building the sources into binaries, and the third for downloading
and building a static ffmpeg build (see
[ffmpeg/README.md](https://github.com/UniversalMediaServer/UniversalMediaServer/tree/master/contrib/ffmpeg)
for more details).

After running the first two scripts the following directory structure is created:

    UniversalMediaServer/
      |
      +-- contrib/
      |     |
      |     +-- binaries-deps-versions
      |     +-- build-pms-binaries.sh
      |     +-- download-pms-binaries-source.sh 
      |     +-- ffmpeg/
      |
      +-- target/
            |
            +-- bin-tools/
                  |
                  +-- build/
                  +-- src/
                  +-- target/
                        |
                        +-- bin/
                        +-- lib/

Search `../target/bin-tools/target/bin/` for compiled binaries and
`../target/bin-tools/target/lib/` for libraries.


## Downloading (and updating) sources
This script downloads the sources for the binaries and libraries:

    download-pms-binaries-source.sh

Run the script and the source archives and directories will be stored in
`../target/bin-tools/src/`.


## Building binaries
This script builds binaries from the sources that were downloaded with the
other script:

    build-pms-binaries.sh


## Cleaning up
To clean binaries built by the three scripts, remove the following directories:

    rm -rf ../target/bin-tools/build/
    rm -rf ../target/bin-tools/target/
    rm -rf ./ffmpeg/build