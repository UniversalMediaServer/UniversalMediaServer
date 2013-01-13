# Build PMS binaries

These scripts are only meant for enthusiasts that want to bundle their PMS with
custom built versions of libraries and tools, replacing the standard versions
shipped with the regular PMS distribution.

There are two scripts available: the first for downloading the sources and the
second for building the sources into binaries.

After running both scripts the following directory structure is created:

    ps3mediaserver/
      |
      +-- contrib/
      |     |
      |     +-- binaries-deps-versions
      |     +-- build-pms-binaries.sh
      |     +-- download-pms-binaries-source.sh 
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
To clean up built binaries, remove the following directories:

    rm -rf ../target/bin-tools/build/
    rm -rf ../target/bin-tools/target/


