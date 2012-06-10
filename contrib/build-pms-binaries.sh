#!/bin/bash
#
# build-pms-osx.sh
#
# Version: 2.1.3
# Last updated: 2012-05-04
# Authors: Patrick Atoon, Happy-Neko
#
#
# DESCRIPTION
#
# Building all libraries and tools for a statically linked PS3 Media
# Server for OSX and Linux is not an easy task and requires advanced
# knowledge of everything involved.
#
#   IF YOU SIMPLY WANT TO BUILD PMS, THIS SCRIPT IS NOT FOR YOU!
#
# This script is only meant for enthusiasts that want to bundle their
# PMS with custom built versions of libraries and tools, replacing
# the standard versions shipped with the regular PMS distribution.
#
# This script will take care of building all that is required to build
# a statically linked PS3 Media Server for OSX or Linux and will bundle
# it with statically linked tools. Note that on OSX true static builds
# are difficult to realize due to Apple only shipping dynamic libraries
# (see https://discussions.apple.com/thread/2252567?start=0&tstart=-1).
#
# For OSX, the script will compile the PS3 Media Server disk image file:
#
#    pms-macosx-arch-*.*.*.dmg
#
# For Linux, it will create a distribution archive:
#
#    pms-generic-linux-unix-*.*.*.tgz
#
# This script is provided as is. If it works for you, good! If it does
# not, try to figure out why and share your findings on the PS3 Media
# Server forums (http://www.ps3mediaserver.org/forum/)
#
#
# REQUIREMENTS
#
# Sources for libraries and tools must be downloaded manually with the
# provided download script. Before running this script, run:
#
#    ./download-pms-binaries-source.sh
# 
# Some Developer tools need to be installed manually. The script detects
# this and provides help.
#
#
# ACKNOWLEDGEMENTS
#
# Many thanks to the PS3 Media Server developers and forum moderators
# for keeping the project alive and the info coming. Special thanks to
# Adrian Stutz for sharing his findings of how to build a statically
# linked MPlayerOSX (http://www.mplayerosx.ch/), without his hard work
# this script would not have been possible. Also thanks to Redlum for
# his assistance in getting this script production ready.
#
#
# TODO
#
# - Clean up dependencies that are not needed for PMS
#
#
# COPYRIGHT
#
# This script is distributed under the Creative Commons Attribution 3.0
# (CC BY) license. This means you are free to copy, distribute, transmit
# and adapt it to suit your needs, as long as you mention the original
# author in your work.
# For more details, see http://creativecommons.org/licenses/by/3.0/
#
#
# CONFIGURATION
#
# Set TARGET_ARCHITECTURE for building binaries. Choose one of the following:
#
# OSX:
#    default: Same as i386
#    native: build for your own computer
#    x86_64: build for 64 bits Intel x86
#    i386: build for Intel 386
#    ppc: build for PowerPC
#
# Linux:
#    default: Same as core2
#    native: build for your own computer
#    core2: Intel Core2 CPU with 64-bit extensions, MMX, SSE, SSE2, SSE3 and SSSE3 instruction set support.   
#    corei7: Intel Core i7 CPU with 64-bit extensions, MMX, SSE, SSE2, SSE3, SSSE3, SSE4.1 and SSE4.2 instruction set support.  
#    For a list of other options see: http://gcc.gnu.org/onlinedocs/gcc/i386-and-x86_002d64-Options.html
#   
TARGET_ARCHITECTURE="default"


# Set optional MINIMUM_ARCHITECTURE for building binaries. Valid only for Linux builds.
# Resulting binaries will require MINIMUM_ARCHITECTURE to run and have extra optimization for TARGET_ARCHITECTURE.
#    generic: Produce code optimized for the most common IA32/AMD64/EM64T processors.
#    native: build for your own computer
#    i686: Same as generic, but when used as march option, PentiumPro instruction set will be used, so the code will run on all i686 family chips. 
#    pentium2: Intel Pentium2 CPU based on PentiumPro core with MMX instruction set support.
#    pentium4: Intel Pentium4 CPU with MMX, SSE and SSE2 instruction set support.   
#    For a list of other options see: http://gcc.gnu.org/onlinedocs/gcc/i386-and-x86_002d64-Options.html
MINIMUM_ARCHITECTURE=""


# Set the amount of threads that are used for compiling everything. This
# should generally be the same as the amount of CPU cores in your computer
# (look for Total Number Of Cores in System Profiler if you don't know it).
THREADS="2"


# It should not be necessary to change anything below this line
##########################################


# binaries deps versions
. binaries-deps-versions


##########################################
# Determine the name of the operating system.
#
UNAME=`which uname`
SYSTEM_NAME=`$UNAME -s | tr "[:upper:]" "[:lower:]"`


##########################################
# Test to see if we are on Mac OSX.
#
is_osx() {
   test "$SYSTEM_NAME" = "darwin"
}


##########################################
# Test to see if we are on Linux.
#
is_linux() {
   # This assumes the world only consist of Linux and OS X. Should be
   # fine for now as the script does not cater for Windows.
   test "$SYSTEM_NAME" != "darwin"
}


##########################################
# Check to see if a binary exists. Displays an instruction on how to
# resolve the problem when a binary cannot be found.
#
check_binary() {
    BINARY=$1
    BINARY_PATH=`command -v $BINARY`
    
    if [ "$BINARY_PATH" == "" ]; then
        case "$BINARY" in
        ant)
            if is_osx; then
                cat >&2 << EOM
It seems you are missing Xcode from Apple ("ant"), which is required to run this script.

Please go to http://developer.apple.com/technologies/xcode.html, create a free
Apple developer account and download Xcode and install it. After installation,
install the "Command Line Tools" under "Preferences... > Downloads > Components".

EOM
            else
                cat >&2 << EOM
It seems you are missing "ant", which is required to run this script.
You can install Ant with following command on Debian based systems (Debian, Ubuntu, etc):

    sudo apt-get install ant

EOM
            fi
            exit;;
        
        gcc|g++|make|patch)
            if is_osx; then
                cat >&2 << EOM
It seems you are missing Xcode from Apple ("$BINARY"), which is required to run this script.

Please go to http://developer.apple.com/technologies/xcode.html, create a free
Apple developer account and download Xcode and install it. After installation,
install the "Command Line Tools" under "Preferences... > Downloads > Components".

EOM
            else
                cat >&2 << EOM
It seems you are missing gcc/g++ or make.
You can install them with following command on Debian based systems (Debian, Ubuntu, etc):

    sudo apt-get install build-essential

EOM
            fi
            exit;;

        autoconf|autoreconf)
            if is_osx; then
                cat >&2 << EOM
It seems you are missing Xcode from Apple ("$BINARY"), which is required to run this script.

Please go to http://developer.apple.com/technologies/xcode.html, create a free
Apple developer account and download Xcode and install it. After installation,
install the "Command Line Tools" under "Preferences... > Downloads > Components".

Note: The Command Line Tools for Xcode 4.3 are missing essential build tools.
You can download and install $BINARY with the following commands:

    curl -O http://mirrors.kernel.org/gnu/autoconf/autoconf-2.68.tar.gz
    tar xzvf autoconf-2.68.tar.gz 
    cd autoconf-2.68
    ./configure --prefix=/usr/local
    make
    sudo make install

EOM
            else
                cat >&2 << EOM
It seems you are missing autoconf.
You can install it with following command on Debian based systems (Debian, Ubuntu, etc):

    sudo apt-get install autoconf

EOM
            fi
            exit;;

        automake)
            if is_osx; then
                cat >&2 << EOM
It seems you are missing Xcode from Apple ("$BINARY"), which is required to run this script.

Please go to http://developer.apple.com/technologies/xcode.html, create a free
Apple developer account and download Xcode and install it. After installation,
install the "Command Line Tools" under "Preferences... > Downloads > Components".

Note: The Command Line Tools for Xcode 4.3 are missing essential build tools.
You can download and install $BINARY with the following commands:

    curl -O http://mirrors.kernel.org/gnu/automake/automake-1.11.tar.gz
    tar xzvf automake-1.11.tar.gz 
    cd automake-1.11
    ./configure --prefix=/usr/local
    make
    sudo make install

EOM
            else
                cat >&2 << EOM
It seems you are missing gcc/g++, make, automake or autoconf.
You can install them with following command on Debian based systems (Debian, Ubuntu, etc):

    sudo apt-get install build-essential autoconf automake

EOM
            fi
            exit;;

        libtool)
            if is_osx; then
                cat >&2 << EOM
It seems you are missing Xcode from Apple ("$BINARY"), which is required to run this script.

Please go to http://developer.apple.com/technologies/xcode.html, create a free
Apple developer account and download Xcode and install it. After installation,
install the "Command Line Tools" under "Preferences... > Downloads > Components".

Note: The Command Line Tools for Xcode 4.3 are missing essential build tools.
You can download and install $BINARY with the following commands:

    curl -O http://mirrors.kernel.org/gnu/libtool/libtool-2.4.tar.gz
    tar xzvf libtool-2.4.tar.gz
    cd libtool-2.4
    ./configure --prefix=/usr/local
    make
    sudo make install
    sudo ln -s /usr/local/bin/libtoolize /usr/local/bin/glibtoolize

EOM
            else
                cat >&2 << EOM
It seems you are missing libtool.
You can install libtool with following command on Debian based systems (Debian, Ubuntu, etc):

    sudo apt-get install libtool

EOM
            fi
            exit;;

        mvn)
            if is_osx; then
                cat >&2 << EOM
It seems you are missing Xcode from Apple ("mvn"), which is required to run this script.

Please go to http://developer.apple.com/technologies/xcode.html, create a free
Apple developer account and download Xcode and install it. After installation,
install the "Command Line Tools" under "Preferences... > Downloads > Components".

EOM
            else
                cat >&2 << EOM
It seems you are missing "mvn", which is required to run this script.
You can install Maven with following command on Debian based systems (Debian, Ubuntu, etc):

    sudo apt-get install maven3

EOM
            fi
            exit;;

        javac)
            if is_osx; then
                cat >&2 << EOM
It seems you are missing Xcode from Apple ("javac"), which is required to run this script.

Please go to http://developer.apple.com/technologies/xcode.html, create a free
Apple developer account and download Xcode and install it. After installation,
install the "Command Line Tools" under "Preferences... > Downloads > Components".

EOM
            else
                cat >&2 << EOM
It seems you are missing JDK, which is required to run this script.
You can install Sun JDK with following command on Debian based systems (Debian, Ubuntu, etc):

    sudo apt-get install sun-java6-jdk

EOM
            fi
            exit;;

        strip)
            cat >&2 << EOM
It seems you are missing "strip", which is required to run this script.
You can install strip with following command on Debian based systems (Debian, Ubuntu, etc):

    sudo apt-get install binutils

EOM
            exit;;

        pkg-config)
            cat >&2 << EOM
It seems you are missing "pkg-config", which is required to run this script.
You can install strip with following command on Debian based systems (Debian, Ubuntu, etc):

    sudo apt-get install pkg-config

EOM
            exit;;

        yasm)
            if is_osx; then
                cat >&2 << EOM
It seems you are missing "yasm", which is required to run this script.
Please run the following commands to install "yasm":

    svn checkout http://www.tortall.net/svn/yasm/trunk/yasm yasm
    cd yasm
    ./autogen.sh
    ./configure
    make
    sudo make install
    cd ..

EOM
            else
                cat >&2 << EOM
It seems you are missing "yasm", which is required to run this script.
You can install yasm with following command on Debian based systems (Debian, Ubuntu, etc):

    sudo apt-get install yasm

EOM
            fi
            exit;;
               
        *)
            cat >&2 << EOM
It seems you are missing "$BINARY", which is required to run this script.
Please make sure the binary is installed, executable and available in the PATH
("which $BINARY" should print a path when you execute it).
EOM
            exit;;
        esac
    fi
    
    # If we didn't exit by now, we're fine.
    echo $BINARY_PATH
}


# Binaries
ANT=`check_binary ant`
GCC=`check_binary gcc`
GPP=`check_binary g++`
JAVAC=`check_binary javac`
AUTOMAKE=`check_binary automake`
AUTOCONF=`check_binary autoconf`
LIBTOOL=`check_binary libtool`
MAKE=`check_binary make`
MVN=`check_binary mvn`
PATCH=`check_binary patch`
SED=`check_binary sed`
TAR=`check_binary tar`
YASM=`check_binary yasm`
UNZIP=`check_binary unzip`

if is_osx; then
    GCC2=$GCC
    HDID=`check_binary hdid`
    HDIUTIL=`check_binary hdiutil`
else
    GCC2=$GCC
    STRIP=`check_binary strip`
    PKG_CONFIG=`check_binary pkg-config`
fi


##########################################
# Check for jarbundler
#
check_jarbundler() {
    # See if the jar file exists
    ls /usr/share/ant/lib/jarbundler-*.jar > /dev/null 2>&1

    if [ "$?" == "1" ]; then
        cat << EOM
It seems you are missing "jarbundler", which is required to run this script.
Please go to http://www.informagen.com/JarBundler/ , download the jarbundler
and install it.
EOM
        exit;
    fi
}


##########################################
# Create a directory when it does not exist
#
createdir() {
    if [ ! -d $1 ]; then
        mkdir -p $1
    fi
}


##########################################
# Exit if the previous command ended with an error status
#
exit_on_error() {
    if [ "$?" != "0" ]; then
        echo Fatal error occurred, aborting build.
        cd $WORKDIR
        exit 1
    fi
}


##########################################
# Initialize building environment
#
initialize() {
    WORKDIR=`pwd`

    # Avoid having ".." in the path reference to avoid problems with symlinks
    cd ..
    PARENTDIR=`pwd`

    # Directories for statically compiled libraries
    BUILD="$PARENTDIR/target/bin-tools/build"
    SRC="$PARENTDIR/target/bin-tools/src"
    TARGET="$PARENTDIR/target/bin-tools/target"
    createdir "$BUILD"
    createdir "$SRC"
    createdir "$TARGET"

    if is_linux; then    
      THREADS=$(cat /proc/cpuinfo | grep processor | wc -l)
    fi
    
    if [ "$TARGET_ARCHITECTURE" == "native" ]; then
      if is_osx; then
        ARCHITECTURE="i386"
      else
        ARCHITECTURE="native"
      fi
    else
      if [ "$TARGET_ARCHITECTURE" == "default" ]; then
        if is_osx; then
          ARCHITECTURE="i386"
        else
          ARCHITECTURE="core2"
        fi
      else
        ARCHITECTURE=$TARGET_ARCHITECTURE
      fi
    fi

    # Set default compiler flags
    set_flags

    # Reset paths for compiling
    if is_osx; then
        export PATH="$TARGET/bin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:/usr/X11/bin"
    else
        export PATH="$TARGET/bin:/usr/bin:/bin:/usr/sbin:/sbin"
    fi
    
    export PKG_CONFIG_PATH=""
    export PKG_CONFIG_LIBDIR=$TARGET/lib/pkgconfig
}


##########################################
# Set the compiler flags to determine the architecture to compile for
# Optional parameter: architecture flags string to replace the default,
# e.g. "-arch ppc -faltivec -mcpu=7450"
#
set_flags() {
    CFLAGS=""
    CXXFLAGS=""
    LDFLAGS=""

    if is_osx; then
        # Minimum OSX version as target
        OSX_VERSION=10.6

        if [ -d /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs ]; then
            # Xcode 4.3+
            SDK_PATH="/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs"
        else
            # Xcode < 4.2
            if [ -d /Developer/SDKs ]; then
                SDK_PATH="/Developer/SDKs"
            else
                cat << EOM
It seems you are missing Xcode.

Please go to http://developer.apple.com/technologies/xcode.html, create a free
Apple developer account and download Xcode and install it. After installation,
install the "Command Line Tools" under "Preferences... > Downloads > Components".

EOM
                exit;
            fi
	fi

        CFLAGS="$CFLAGS -mmacosx-version-min=${OSX_VERSION}"
        CFLAGS="$CFLAGS -isystem ${SDK_PATH}/MacOSX${OSX_VERSION}.sdk"
        CXXFLAGS="$CXXFLAGS -mmacosx-version-min=${OSX_VERSION}"
        CXXFLAGS="$CXXFLAGS -isysroot ${SDK_PATH}/MacOSX${OSX_VERSION}.sdk"
        LDFLAGS="$LDFLAGS -mmacosx-version-min=${OSX_VERSION}"
        LDFLAGS="$LDFLAGS -isysroot ${SDK_PATH}/MacOSX${OSX_VERSION}.sdk"
        LDFLAGS="$LDFLAGS -Wl,-syslibroot,${SDK_PATH}/MacOSX${OSX_VERSION}.sdk"
    fi

    if [ "$1" != "" ]; then
        # Use the supplied parameter string for architecture flags
        CFLAGS="$CFLAGS $1"
        LDFLAGS="$LDFLAGS $1"
        CXXFLAGS="$CXXFLAGS $1"
    else
        # Use the architecture defined in this script
        if is_osx; then
            CFLAGS="$CFLAGS -arch $ARCHITECTURE"
            LDFLAGS="$LDFLAGS -arch $ARCHITECTURE"
            CXXFLAGS="$CXXFLAGS -arch $ARCHITECTURE"
        else
            if [ "$MINIMUM_ARCHITECTURE" == "" ]; then
              CFLAGS="$CFLAGS -mtune=$ARCHITECTURE"
              CXXFLAGS="$CXXFLAGS -mtune=$ARCHITECTURE"
              LDFLAGS="$LDFLAGS -mtune=$ARCHITECTURE"
            else
              CFLAGS="$CFLAGS -march=$MINIMUM_ARCHITECTURE -mtune=$ARCHITECTURE"
              CXXFLAGS="$CXXFLAGS -march=$MINIMUM_ARCHITECTURE -mtune=$ARCHITECTURE"
              LDFLAGS="$LDFLAGS -march=$MINIMUM_ARCHITECTURE -mtune=$ARCHITECTURE"
            fi
        fi
    fi

    # Paths of the build environment
    CFLAGS="$CFLAGS -I$TARGET/include"
    CXXFLAGS="$CXXFLAGS -I$TARGET/include"
    
    if is_osx; then
        LDFLAGS="$LDFLAGS -L$TARGET/lib -Wl,-search_paths_first"
    else
        LDFLAGS="$LDFLAGS -L$TARGET/lib -Wl,-rpath -Wl,$TARGET/lib"
    fi
    
    export CFLAGS CXXFLAGS LDFLAGS
}


##########################################
# Building start marker to more easily follow the build process
#
start_build() {
    cat << EOM


--------------------------------------------------------------------------------------
Building $1
--------------------------------------------------------------------------------------

EOM
}


##########################################
# BZIP2
# http://bzip.org/
#
build_bzip2() {
    start_build bzip2
    cd $BUILD

    if [ ! -d bzip2-$VERSION_BZIP2 ]; then
        $TAR zxf $SRC/bzip2-$VERSION_BZIP2.tar.gz
        exit_on_error
    fi

    cd bzip2-$VERSION_BZIP2
    set_flags
    
    # Overrule LDFLAGS and CFLAGS in the Makefile by uncommenting them
    $SED -i -e "s/^LDFLAGS/#LDFLAGS/" Makefile
    $SED -i -e "s/^CFLAGS/#CFLAGS/" Makefile
    CFLAGS="$CFLAGS -D_FILE_OFFSET_BITS=64"

    $MAKE -j$THREADS
    exit_on_error
    $MAKE install PREFIX=$TARGET

    cd $WORKDIR
}


##########################################
# DCRAW
# http://www.cybercom.net/~dcoffin/dcraw/
#
build_dcraw() {
    start_build dcraw
    cd $BUILD

    if [ ! -d dcraw-$VERSION_DCRAW ]; then
        $TAR zxf $SRC/dcraw-$VERSION_DCRAW.tar.gz
        exit_on_error
        mv ./dcraw ./dcraw-$VERSION_DCRAW
    fi

    cd dcraw-$VERSION_DCRAW
    set_flags
    if is_osx; then
      $GCC -O4 -o dcraw dcraw.c -lm -ljpeg -DNO_LCMS $CFLAGS -L$TARGET/lib
    else
      $GCC -O4 -static -Wl,--strip-all -o dcraw dcraw.c -lm -ljpeg -DNO_LCMS $CFLAGS -L$TARGET/lib
    fi    
    exit_on_error
    createdir $TARGET/bin
    cp dcraw $TARGET/bin
    cd $WORKDIR
}


##########################################
# ENCA Extremely Naive Charset Analyser
# http://cihar.com/software/enca/
#
build_enca() {
    start_build enca
    cd $BUILD

    if [ ! -d enca-$VERSION_ENCA ]; then
        $TAR zxf $SRC/enca-$VERSION_ENCA.tar.gz
        exit_on_error
    fi

    cd enca-$VERSION_ENCA
    set_flags
    ./configure --disable-shared --enable-static --disable-dependency-tracking --prefix=$TARGET
    exit_on_error
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# EXPAT
# http://expat.sourceforge.net/
#
build_expat() {
    start_build expat
    cd $BUILD

    if [ ! -d expat-$VERSION_EXPAT ]; then
        $TAR zxf $SRC/expat-$VERSION_EXPAT.tar.gz
        exit_on_error
    fi

    cd expat-$VERSION_EXPAT
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# FAAD2
# http://www.audiocoding.com/faad2.html
#
build_faad2() {
    start_build faad2
    cd $BUILD

    if [ ! -d faad2-$VERSION_FAAD2 ]; then
        $TAR zxf $SRC/faad2-$VERSION_FAAD2.tar.gz
        exit_on_error
    fi

    cd faad2-$VERSION_FAAD2
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# FFMPEG
# http://www.ffmpeg.org/
#
build_ffmpeg() {
    start_build ffmpeg
    cd $BUILD
    
    cp -af $SRC/ffmpeg ./
    exit_on_error
    cd ffmpeg
    exit_on_error

    if is_osx; then
        set_flags

        # VDA disabled for mplayer, also disabled here to avoid build errors
        ./configure --enable-gpl --enable-version3 --enable-nonfree --disable-doc --disable-debug \
              --enable-libmp3lame --enable-libx264 --enable-libxvid --enable-libfreetype \
              --cc=$GCC2 --disable-vda \
              --disable-devices --disable-ffplay --disable-ffserver --disable-ffprobe \
              --disable-shared --enable-static --prefix=$TARGET
    else
        set_flags

        # libvorbis disabled for mplayer, also disabled here to avoid build errors
        ./configure --enable-gpl --enable-version3 --enable-nonfree --disable-doc --disable-debug \
              --enable-libmp3lame --enable-libx264 --enable-libxvid --enable-libfreetype \
              --enable-runtime-cpudetect \
              --extra-libs=-static \
              --disable-devices --disable-ffplay --disable-ffserver --disable-ffprobe \
              --disable-vdpau --disable-dxva2 --disable-avisynth \
              --disable-libtheora --disable-libvorbis \
              --disable-shared --enable-static --prefix=$TARGET
    fi

    # Apply SB patch that was used for the Windows version
    $PATCH -p1 < $WORKDIR/mplayer-ffmpeg.patch
    exit_on_error

    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# FLAC
# http://flac.sourceforge.net/
#
build_flac() {
    start_build flac
    cd $BUILD

    if [ ! -d flac-$VERSION_FLAC ]; then
        $TAR zxf $SRC/flac-$VERSION_FLAC.tar.gz
        exit_on_error
    fi

    cd flac-$VERSION_FLAC
    set_flags
    
    if is_osx; then
        if [ "$ARCHITECTURE" == "x86_64" ]; then
            ./configure --disable-shared --disable-dependency-tracking --host=x86-apple-darwin10 --prefix=$TARGET
        else
            ./configure --disable-shared --disable-dependency-tracking --disable-asm-optimizations --prefix=$TARGET
        fi
    else
        $SED --in-place 's/#include <stdlib.h>/#include <stdlib.h>\n#include <cstring>/g' ./examples/cpp/encode/file/main.cpp
        $SED --in-place 's/#include <stdlib.h>/#include <stdlib.h>\n#include <cstring>/g' ./examples/cpp/decode/file/main.cpp

        ./configure --disable-shared --enable-static --disable-dependency-tracking --prefix=$TARGET
    fi

    $MAKE -j$THREADS
    exit_on_error

    if is_linux; then
      # compile statically linked flac binary
      cd src/flac
      $GCC -static -Wl,--strip-all -I$TARGET/include -O3 -funroll-loops -finline-functions -Wall -W -Winline $CFLAGS $LDFLAGS  -o flac analyze.o decode.o encode.o foreign_metadata.o main.o local_string_utils.o utils.o vorbiscomment.o  -L$TARGET/lib ../../src/share/grabbag/.libs/libgrabbag.a ../../src/share/getopt/libgetopt.a ../../src/share/replaygain_analysis/.libs/libreplaygain_analysis.a ../../src/share/replaygain_synthesis/.libs/libreplaygain_synthesis.a ../../src/shar$SRClibs/libutf8.a ../../src/libFLAC/.libs/libFLAC.a -L$TARGET/lib $TARGET/lib/libogg.a $TARGET/lib/libiconv.a -lm
      exit_on_error
      cd ../..
    fi
    $MAKE install
    cd $WORKDIR
}


##########################################
# FONTCONFIG
# http://fontconfig.org/wiki/
#
build_fontconfig() {
    start_build fontconfig
    cd $BUILD

    if [ ! -d fontconfig-$VERSION_FONTCONFIG ]; then
        $TAR zxf $SRC/fontconfig-$VERSION_FONTCONFIG.tar.gz
        exit_on_error
    fi

    cd fontconfig-$VERSION_FONTCONFIG
    set_flags
    if is_linux; then
        ./configure --sysconfdir=/etc --localstatedir=/var --enable-static --disable-shared --disable-dependency-tracking --prefix=$TARGET
    else
        ./configure --with-confdir=./fonts --enable-static --disable-shared --disable-dependency-tracking --prefix=$TARGET
    fi
    $MAKE -j$THREADS
    exit_on_error

    if is_linux; then
        $MAKE install-exec && $MAKE install-pkgconfigDATA
        exit_on_error
        # copy freetype headers
        mkdir $TARGET/include/fontconfig
        cp ./fontconfig/*.h $TARGET/include/fontconfig/
        # freetype depends on bzip2
        $SED -i -e "s/^Libs\.private.*$/Libs.private: -lexpat -lfreetype -lz -liconv -lbz2/" $TARGET/lib/pkgconfig/fontconfig.pc
    else
        $MAKE install
    fi
    exit_on_error
    
    cd $WORKDIR
}


##########################################
# FREETYPE
# http://www.freetype.org/
#
build_freetype() {
    start_build freetype
    cd $BUILD

    if [ ! -d freetype-$VERSION_FREETYPE ]; then
        $TAR zxf $SRC/freetype-$VERSION_FREETYPE.tar.gz
        exit_on_error
    fi

    cd freetype-$VERSION_FREETYPE
    set_flags
    
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# FRIBIDI
# http://fribidi.org/
#
build_fribidi() {
    start_build fribidi
    cd $BUILD

    if [ ! -d fribidi-$VERSION_FRIBIDI ]; then
        $TAR zxf $SRC/fribidi-$VERSION_FRIBIDI.tar.gz
        exit_on_error
    fi

    cd fribidi-$VERSION_FRIBIDI
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# GIFLIB
# http://sourceforge.net/projects/giflib/
#
build_giflib() {
    start_build giflib
    cd $BUILD

    if [ ! -d giflib-$VERSION_GIFLIB ]; then
        $TAR xjf $SRC/giflib-$VERSION_GIFLIB.tar.bz2
        exit_on_error
    fi

    cd giflib-$VERSION_GIFLIB
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# ICONV
# http://www.gnu.org/software/libiconv/
#
build_iconv() {
    start_build iconv
    cd $BUILD

    if [ ! -d libiconv-$VERSION_ICONV ]; then
        $TAR zxf $SRC/libiconv-$VERSION_ICONV.tar.gz
        exit_on_error
    fi

    cd libiconv-$VERSION_ICONV
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# JPEG
# http://www.ijg.org/
#
build_jpeg() {
    start_build jpeg
    cd $BUILD

    if [ ! -d jpeg-$VERSION_JPEG ]; then
        $TAR zxf $SRC/jpegsrc.v$VERSION_JPEG.tar.gz
        exit_on_error
    fi

    cd jpeg-$VERSION_JPEG
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LAME
# http://lame.sourceforge.net/
#
build_lame() {
    start_build lame
    cd $BUILD

    if [ ! -d lame-$VERSION_LAME ]; then
        $TAR zxf $SRC/lame-$VERSION_LAME.tar.gz
        exit_on_error
    fi

    cd lame-$VERSION_LAME
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBBLURAY
# http://www.videolan.org/developers/libbluray.html
#
build_libbluray() {
    start_build libbluray
    cd $BUILD

    cp -a $SRC/libbluray ./
    exit_on_error
    cd libbluray
    exit_on_error

    ./bootstrap
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBDCA
# http://www.videolan.org/developers/libdca.html
#
build_libdca() {
    start_build libdca
    cd $BUILD

    if [ ! -d libdca-$VERSION_LIBDCA ]; then
        $TAR xjf $SRC/libdca-$VERSION_LIBDCA.tar.bz2
        exit_on_error
    fi

    cd libdca-$VERSION_LIBDCA
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBDV
# http://libdv.sourceforge.net/
#
build_libdv() {
    start_build libdv
    cd $BUILD

    if [ ! -d libdv-$VERSION_LIBDV ]; then
        $TAR zxf $SRC/libdv-$VERSION_LIBDV.tar.gz
        exit_on_error
    fi

    cd libdv-$VERSION_LIBDV
    set_flags
    
    if is_osx; then
        export LDFLAGS="$LDFLAGS -flat_namespace -undefined suppress"
        ./configure --disable-shared --disable-dependency-tracking --disable-xv \
            --disable-gtk --disable-sdl --disable-asm --prefix=$TARGET
    else
        ./configure --disable-shared --disable-dependency-tracking --disable-xv \
            --disable-gtk --disable-sdl --prefix=$TARGET
    fi
    
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBMAD
# http://www.underbit.com/products/mad/
#
build_libmad() {
    start_build libmad
    cd $BUILD

    if [ ! -d libmad-$VERSION_LIBMAD ]; then
        $TAR zxf $SRC/libmad-$VERSION_LIBMAD.tar.gz
        exit_on_error
    fi

    cd libmad-$VERSION_LIBMAD
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBMEDIAINFO
# http://sourceforge.net/projects/mediainfo/
#
build_libmediainfo() {
    start_build libmediainfo
    cd $BUILD

    if [ ! -d libmediainfo_$VERSION_LIBMEDIAINFO ]; then
        if is_osx; then
          $TAR xjf $SRC/libmediainfo_$VERSION_LIBMEDIAINFO.tar.bz2 -s /MediaInfoLib/libmediainfo_$VERSION_LIBMEDIAINFO/
          exit_on_error
        else
          $TAR xjf $SRC/libmediainfo_$VERSION_LIBMEDIAINFO.tar.bz2
          exit_on_error
          mv ./MediaInfoLib/ ./libmediainfo_$VERSION_LIBMEDIAINFO
        fi        
    fi

    cd libmediainfo_$VERSION_LIBMEDIAINFO
    cd Project/GNU/Library

    set_flags

    if is_osx; then
        # Fix for broken compilation because of unknown token PKG_CHECK_MODULES
        $SED -i -e "s/PKG_CHECK_MODULES/#PKG_CHECK_MODULES/" configure.ac
    fi

    # Note: libmediainfo requires libzen source to compile
    ./autogen
    ./configure --enable-shared --disable-dependency-tracking --enable-staticlibs --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBPNG
# http://www.libpng.org/pub/png/libpng.html
#
build_libpng() {
    start_build libpng
    cd $BUILD

    if [ ! -d libpng-$VERSION_LIBPNG ]; then
        $TAR zxf $SRC/libpng-$VERSION_LIBPNG.tar.gz
        exit_on_error
    fi

    cd libpng-$VERSION_LIBPNG
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBOGG
# http://xiph.org/downloads/
#
build_libogg() {
    start_build libogg
    cd $BUILD

    if [ ! -d libogg-$VERSION_LIBOGG ]; then
        $TAR zxf $SRC/libogg-$VERSION_LIBOGG.tar.gz
        exit_on_error
    fi

    cd libogg-$VERSION_LIBOGG
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBVORBIS
# http://xiph.org/downloads/
#
build_libvorbis() {
    start_build libvorbis
    cd $BUILD

    if [ ! -d libvorbis-$VERSION_LIBVORBIS ]; then
        $TAR zxf $SRC/libvorbis-$VERSION_LIBVORBIS.tar.gz
        exit_on_error
    fi

    cd libvorbis-$VERSION_LIBVORBIS
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --with-ogg=$TARGET --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBTHEORA
# http://xiph.org/downloads/
#
build_libtheora() {
    start_build libtheora
    cd $BUILD

    if [ ! -d libtheora-$VERSION_LIBTHEORA ]; then
        $TAR xjf $SRC/libtheora-$VERSION_LIBTHEORA.tar.bz2
        exit_on_error
    fi

    cd libtheora-$VERSION_LIBTHEORA
    set_flags
    ./configure --disable-shared --disable-dependency-tracking --with-ogg=$TARGET \
        --with-vorbis=$TARGET --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LIBZEN
# http://sourceforge.net/projects/zenlib/
#
build_libzen() {
    start_build libzen
    cd $BUILD

    if [ ! -d libzen_$VERSION_LIBZEN ]; then
        $TAR xjf $SRC/libzen_$VERSION_LIBZEN.tar.bz2
        exit_on_error

        # For consistency and the check above have the same directory name as the .bz2 file available
        ln -s ZenLib libzen_$VERSION_LIBZEN 
    fi

    cd libzen_$VERSION_LIBZEN
    cd Project/GNU/Library

    set_flags

    ./autogen
    if is_osx; then
        ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    fi
    if is_linux; then
        ./configure --enable-shared --disable-dependency-tracking --prefix=$TARGET
    fi
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# LZO
# http://www.oberhumer.com/opensource/lzo/
#
build_lzo() {
    start_build lzo
    cd $BUILD

    if [ ! -d lzo-$VERSION_LZO ]; then
        $TAR zxf $SRC/lzo-$VERSION_LZO.tar.gz
        exit_on_error
    fi

    cd lzo-$VERSION_LZO
    set_flags

    if is_osx; then
      if [ "$ARCHITECTURE" == "PPC" ]; then
        ./configure --disable-shared --disable-dependency-tracking --disable-asm --prefix=$TARGET
      else
        ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
      fi
    else
      ./configure --disable-shared --disable-dependency-tracking --prefix=$TARGET
    fi      

    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# MPLAYER
# http://www.mplayerhq.hu/design7/news.html
#
build_mplayer() {
    start_build mplayer
    cd $BUILD

    cp -a $SRC/mplayer ./
    exit_on_error
    cd mplayer
    exit_on_error

    # Copy ffmpeg source to avoid making another git clone by configure
    rm -rf ffmpeg
    cp -rf $BUILD/ffmpeg .

    if is_osx; then
        # OSX

        # Flags for compiling mplayer
        export CFLAGS="-O4 -fomit-frame-pointer -pipe -mmacosx-version-min=${OSX_VERSION} -arch $ARCHITECTURE -I$TARGET/include"
        export CXXFLAGS="-O4 -fomit-frame-pointer -pipe mmacosx-version-min=${OSX_VERSION} -arch $ARCHITECTURE -I$TARGET/include"
        export LDFLAGS="-mmacosx-version-min=${OSX_VERSION} -arch $ARCHITECTURE -L$TARGET/lib"

        # /usr/bin/gcc gives compile errors for MPlayer on OSX Lion.
        # See https://svn.macports.org/ticket/30279

        # Apply SB patch that was used for the Windows version
        $PATCH -p0 < $WORKDIR/mplayer.patch
        exit_on_error

        # Theora and vorbis support seems broken in this revision, disable it for now
        ./configure --cc=$GCC2 --disable-x11 --disable-gl --disable-qtx \
              --with-freetype-config=$TARGET/bin/freetype-config --prefix=$TARGET

        # Somehow -I/usr/X11/include still made it into the config.mak, regardless of the --disable-x11
        $SED -i -e "s/-I\/usr\/X11\/include//g" config.mak

        # Fix fribidi regression (http://lists.mplayerhq.hu/pipermail/mplayer-users/2011-May/082649.html)
        $SED -i -e "s/#ifdef CONFIG_FRIBIDI/#if defined(CONFIG_FRIBIDI) \&\& \!defined(CODECS2HTML)/g" sub/subreader.h
    else
        # Linux
        set_flags
        export CFLAGS="$CFLAGS -O4 -fomit-frame-pointer -pipe"
        export LDFLAGS="$LDFLAGS -O4 -fomit-frame-pointer -pipe"

        # Apply SB patch that was used for the Windows version
        $PATCH -p0 < $WORKDIR/mplayer.patch
        exit_on_error

        # mplayer configure patch
        $PATCH -p0 < $WORKDIR/mplayer-configure.patch
        exit_on_error

        # libvorbis support seems broken in this revision, disable it for now
        ./configure --enable-static --enable-runtime-cpudetection \
                --disable-libvorbis --disable-qtx --disable-svga --disable-sdl \
                --disable-speex --disable-libgsm \
                --disable-alsa --disable-ossaudio --disable-pulse \
                --disable-tv --disable-tv-v4l1 --disable-tv-v4l2 --disable-tv-bsdbt848 --disable-pvr \
                --disable-radio --disable-radio-capture --disable-radio-v4l2 --disable-radio-bsdbt848 \
                --disable-esd --disable-jack --disable-openal --disable-apple-ir --disable-vm  --disable-nas \
                --disable-directfb --disable-caca --disable-aa --disable-v4l2 --disable-fbdev \
                --disable-matrixview --disable-gl   --disable-x11 --disable-vidix \
                --disable-maemo --disable-inet6 --disable-dvb --disable-vstream --disable-live \
                --disable-win32dll --disable-smb --disable-ladspa --disable-librtmp  \
                --disable-gif --disable-tga --disable-md5sum --disable-pnm \
                --with-freetype-config=$TARGET/bin/freetype-config \
                --prefix=$TARGET
    fi

    # Remove the ffmpeg directory and copy the compiled ffmpeg again to avoid "make" rebuilding it
    rm -rf ffmpeg
    cp -rf $BUILD/ffmpeg .

    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# NCURSES
# http://www.gnu.org/software/ncurses/
#
build_ncurses() {
    start_build ncurses
    cd $BUILD

    if [ ! -d ncurses-$VERSION_NCURSES ]; then
        $TAR zxf $SRC/ncurses-$VERSION_NCURSES.tar.gz
        exit_on_error
    fi

    cd ncurses-$VERSION_NCURSES
    set_flags
    ./configure --without-shared --disable-shared --disable-dependency-tracking --prefix=$TARGET
    $MAKE libs
    exit_on_error
    $MAKE install.libs
    cd $WORKDIR
}


##########################################
# PS3MEDIASERVER
# https://github.com/ps3mediaserver/ps3mediaserver
#
build_ps3mediaserver() {
    start_build ps3mediaserver
    cd $BUILD

    cp -a $SRC/ps3mediaserver ./
    exit_on_error
    cd ps3mediaserver
    exit_on_error

    if is_osx; then
        # OSX
        mkdir -p ./target/bin/osx

        # Overwrite with the home built tools
        cp $TARGET/bin/dcraw ./target/bin/osx
        cp $TARGET/bin/ffmpeg ./target/bin/osx
        cp $TARGET/bin/flac ./target/bin/osx
        cp $TARGET/bin/mplayer ./target/bin/osx
        cp $TARGET/bin/mencoder ./target/bin/osx
        cp $TARGET/bin/tsMuxeR ./target/bin/osx

        $MVN package
        exit_on_error

        # Add the architecture name to the final file
        PMS_FILENAME_ORIG=`ls pms-macosx-*.dmg | head -1`
        PMS_FILENAME_NEW=`echo $PMS_FILENAME_ORIG | $SED -e "s/-macosx-/-macosx-$ARCHITECTURE-/"`
        mv -f $PMS_FILENAME_ORIG $PMS_FILENAME_NEW
        cp $PMS_FILENAME_NEW $WORKDIR
    else
        # Linux
        mkdir -p ./target/bin/linux

        # Overwrite with the home built tools
        cp $TARGET/bin/dcraw ./target/bin/linux
        cp $TARGET/bin/ffmpeg ./target/bin/linux
        #cp $TARGET/bin/flac ./target/bin/linux
        cp $TARGET/bin/mplayer ./target/bin/linux
        cp $TARGET/bin/mencoder ./target/bin/linux
        cp $TARGET/bin/tsMuxeR ./target/bin/linux
        cp $TARGET/lib/libmediainfo.so.0.0.0 ./target/bin/linux/libmediainfo.so
        $STRIP --strip-unneeded ./target/bin/linux/libmediainfo.so

        $MVN package
        exit_on_error

        cd target
        PMS_FILENAME=`ls pms-generic-linux-*.tgz | head -1`
        mv $PMS_FILENAME $WORKDIR
    fi

    cd $WORKDIR
}


##########################################
# TSMUXER
# http://www.smlabs.net/en/products/tsmuxer/
# http://www.videohelp.com/tools/tsMuxeR
# Interesting Open Source followup project in development: https://github.com/kierank/libmpegts
#
build_tsmuxer() {
    start_build tsmuxer
    cd $BUILD

    if is_osx; then
        if [ ! -d tsMuxeR_${VERSION_TSMUXER} ]; then
            createdir tsMuxeR_${VERSION_TSMUXER}
            # Nothing to build. Just open the disk image, copy the binary and detach the disk image
            $HDID $SRC/tsMuxeR_${VERSION_TSMUXER}.dmg
            exit_on_error

            cp -f /Volumes/tsMuxeR/tsMuxerGUI.app/Contents/MacOS/tsMuxeR tsMuxeR_${VERSION_TSMUXER}/tsMuxeR
            exit_on_error
            $HDIUTIL detach /Volumes/tsMuxeR
        fi
    else
        if [ ! -d tsMuxeR_$VERSION_TSMUXER ]; then
            createdir tsMuxeR_$VERSION_TSMUXER
            cd tsMuxeR_$VERSION_TSMUXER
            $TAR xzf $SRC/tsMuxeR_${VERSION_TSMUXER}.tar.gz
            exit_on_error
            cd ..
        fi
    fi

    cp -f tsMuxeR_$VERSION_TSMUXER/tsMuxeR $TARGET/bin
    cd $WORKDIR
}


##########################################
# X264
# svn://svn.videolan.org/x264/trunk
#
build_x264() {
    start_build x264
    cd $BUILD

    cp -a $SRC/x264 ./
    exit_on_error
    cd x264
    exit_on_error

    set_flags

    if is_osx; then
      # There is a strange cyclic dependency here; FFmpeg uses x264 and
      # x264 used libav* from FFmpeg. Delete pre-existing libraries for
      # consistent builds; x264 can be built without them.
      rm -f $TARGET/lib/libav*

      if [ "$ARCHITECTURE" == "i386" ]; then
       ./configure --prefix=$TARGET --host=i386-apple-darwin10 --disable-asm
      else
        ./configure --prefix=$TARGET
      fi
    else
      ./configure --prefix=$TARGET
    fi  

    $MAKE -j$THREADS
    exit_on_error
    $MAKE install install-lib-static

    cd $WORKDIR
}


##########################################
# XVID
# http://www.xvid.org/
#
build_xvid() {
    start_build xvid
    cd $BUILD

    if [ ! -d xvidcore-$VERSION_XVID ]; then
        $TAR zxf $SRC/xvidcore-$VERSION_XVID.tar.gz
        exit_on_error
        mv xvidcore xvidcore-$VERSION_XVID
    fi

    cd xvidcore-$VERSION_XVID/build/generic
    set_flags

    if is_osx; then
      ./configure --prefix=$TARGET --host=x86-apple-darwin10
    else
      ./configure --prefix=$TARGET
    fi

    $MAKE -j$THREADS
    exit_on_error
    $MAKE install

    # Remove dynamic libraries
    if is_osx; then
        rm -f $TARGET/lib/libxvidcore*.dylib
    else
        rm -f $TARGET/lib/libxvidcore.so*
    fi

    cd $WORKDIR
}


##########################################
# ZLIB
# http://zlib.net/
#
build_zlib() {
    start_build zlib
    cd $BUILD

    if [ ! -d zlib-$VERSION_ZLIB ]; then
        $TAR xzf $SRC/zlib-$VERSION_ZLIB.tar.gz
        exit_on_error
    fi

    cd zlib-$VERSION_ZLIB
    set_flags
    if is_linux; then
        export CFLAGS="-fPIC"
        export CXXFLAGS="-fPIC"
    fi
    ./configure --prefix=$TARGET
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install

    # Remove dynamic libraries
    if is_osx; then
        rm -f $TARGET/lib/libz*.dylib
    else
        rm -f $TARGET/lib/libz.so*
    fi

    cd $WORKDIR
}


##########################################
# YASM
# http://yasm.tortall.net/
#
build_yasm() {
    start_build yasm
    cd $BUILD

    if [ ! -d yasm-$VERSION_YASM ]; then
        $TAR zxf $SRC/yasm-$VERSION_YASM.tar.gz
        exit_on_error
    fi

    cd yasm-$VERSION_YASM
    set_flags
    ./configure --prefix=$TARGET
    exit_on_error
    $MAKE -j$THREADS
    exit_on_error
    $MAKE install
    cd $WORKDIR
}


##########################################
# Finally, execute the script...
#

if is_osx; then
    check_jarbundler
fi

# Initialize variables for compiling
initialize

# Build static libraries to link against
#build_yasm # for systems where YASM version is below 1.0.0
build_zlib
build_bzip2
build_expat
build_faad2
build_freetype
build_iconv
# Note: fontconfig requires freetype and iconv to build
build_fontconfig
build_fribidi
build_giflib
build_jpeg
build_ncurses
build_lame
build_libbluray
build_libdca
build_libdv
build_libmad
build_libzen
# Note: libmediainfo requires libzen to build
build_libmediainfo
build_libpng
build_libogg
build_libvorbis
build_libtheora
build_lzo
build_x264
build_xvid

# Build tools for including with PS3 Media Server
if is_osx; then
    build_flac
fi
build_dcraw
build_tsmuxer
build_enca
build_ffmpeg
build_mplayer

# Build PS3 Media Server itself
# build_ps3mediaserver
