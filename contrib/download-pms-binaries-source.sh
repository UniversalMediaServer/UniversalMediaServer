#!/bin/bash
#
# download-pms-binaries-source.sh
#
# Version: 1.0
# Last updated: 2012-01-26
# Authors: Happy-Neko
# Based on build-pms-binaries.sh by Patrick Atoon and Happy-Neko
#
#
# DESCRIPTION
# Download sources for binary tools and their dependencies.
#
# REQUIREMENTS
# sed, git, subversion, wget (for Linux) or curl (for Mac OS X)
#
# CONFIGURATION
#
# Set FIXED_REVISIONS to "no" to check out the latest revisions.
# Default is "yes" to check out the last known working revision.
FIXED_REVISIONS="yes"

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
        git)
            if is_osx; then
                cat >&2 << EOM
It seems you are missing "git", which is required to run this script.
Please go to http://code.google.com/p/git-osx-installer/, download git
and install it.

EOM
            else
                cat >&2 << EOM
It seems you are missing "git", which is required to run this script.
You can install Git with following command on Debian based systems (Debian, Ubuntu, etc):

    sudo apt-get install git

Or (for older systems):

    sudo apt-get install git-core

EOM
            fi
            exit;;

        svn)
            if is_osx; then
                cat >&2 << EOM
It seems you are missing Xcode from Apple ("svn"), which is required to run this script.

Please go to http://developer.apple.com/technologies/xcode.html, create a free
Apple developer account and download Xcode and install it.

EOM
            else
                cat >&2 << EOM
It seems you are missing "svn", which is required to run this script.
You can install Subversion with following command on Debian based systems (Debian, Ubuntu, etc):

    sudo apt-get install subversion

EOM
            fi
            exit;;

        wget)
            cat >&2 << EOM
It seems you are missing "wget", which is required to run this script.
You can install Wget with following command on Debian based systems (Debian, Ubuntu, etc):

    sudo apt-get install wget

EOM
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
GIT=`check_binary git`
SED=`check_binary sed`
SVN=`check_binary svn`

if is_osx; then
    CURL=`check_binary curl`
else
    WGET=`check_binary wget`
fi


##########################################
# Create a directory when it does not exist
#
createdir() {
    if [ ! -d $1 ]; then
        mkdir -p $1
    fi
}


##########################################
# Download a file from a URL
#
download() {
    URL=$1
    FILENAME=`echo $URL | $SED "s/.*\///g"`

    if is_osx; then
        $CURL -L $URL > $FILENAME
    else
        $WGET $URL
    fi
}


##########################################
# Exit if the previous command ended with an error status
#
exit_on_error() {
    if [ "$?" != "0" ]; then
        ER=$?
        echo Fatal error occurred, aborting build.
        cd $WORKDIR
        exit $ER
    fi
}


##########################################
# Initialize environment
#
initialize() {
    WORKDIR=`pwd`
    SRC="$WORKDIR/../target/bin-tools/src"
    createdir "$SRC"
}


##########################################
# Download start marker to more easily follow the downloading process
#
start_download() {
    cat << EOM


--------------------------------------------------------------------------------------
Downloading $1
--------------------------------------------------------------------------------------

EOM
}


##########################################
# BZIP2
# http://bzip.org/
#
download_bzip2() {
    start_download bzip2
    cd $SRC

    if [ ! -f bzip2-${VERSION_BZIP2}.tar.gz ]; then
        download http://bzip.org/${VERSION_BZIP2}/bzip2-${VERSION_BZIP2}.tar.gz
        exit_on_error
    fi
}


##########################################
# DCRAW
# http://www.cybercom.net/~dcoffin/dcraw/
#
download_dcraw() {
    start_download dcraw
    cd $SRC

    if [ ! -f dcraw-${VERSION_DCRAW}.tar.gz ]; then
        download http://www.cybercom.net/~dcoffin/dcraw/archive/dcraw-${VERSION_DCRAW}.tar.gz
        exit_on_error
    fi
}


##########################################
# EXPAT
# http://expat.sourceforge.net/
#
download_expat() {
    start_download expat
    cd $SRC

    if [ ! -f expat-${VERSION_EXPAT}.tar.gz ]; then
        download http://downloads.sourceforge.net/project/expat/expat/${VERSION_EXPAT}/expat-${VERSION_EXPAT}.tar.gz
        exit_on_error
    fi
}


##########################################
# FAAD2
# http://www.audiocoding.com/faad2.html
#
download_faad2() {
    start_download faad2
    cd $SRC

    if [ ! -f faad2-${VERSION_FAAD2}.tar.gz ]; then
        download http://downloads.sourceforge.net/project/faac/faad2-src/faad2-${VERSION_FAAD2}/faad2-${VERSION_FAAD2}.tar.gz
        exit_on_error
    fi
}


##########################################
# FFMPEG
# http://www.ffmpeg.org/
#
download_ffmpeg() {
    start_download ffmpeg
    cd $SRC

    if [ -d ffmpeg ]; then
        rm -rf ffmpeg
    fi
    $GIT clone git://git.videolan.org/ffmpeg.git ffmpeg
    exit_on_error
    cd ffmpeg

    if [ "$FIXED_REVISIONS" == "yes" ]; then
        $GIT checkout ${VERSION_FFMPEG}
        exit_on_error
    fi
    rm -rf ./.git
}


##########################################
# FLAC
# http://flac.sourceforge.net/
#
download_flac() {
    start_download flac
    cd $SRC

    if [ ! -f flac-${VERSION_FLAC}.tar.gz ]; then
        download http://downloads.xiph.org/releases/flac/flac-${VERSION_FLAC}.tar.gz
        exit_on_error
    fi
}


##########################################
# FONTCONFIG
# http://fontconfig.org/wiki/
#
download_fontconfig() {
    start_download fontconfig
    cd $SRC

    if [ ! -f fontconfig-${VERSION_FONTCONFIG}.tar.gz ]; then
        download http://www.freedesktop.org/software/fontconfig/release/fontconfig-${VERSION_FONTCONFIG}.tar.gz
        exit_on_error
    fi
}


##########################################
# FREETYPE
# http://www.freetype.org/
#
download_freetype() {
    start_download freetype
    cd $SRC

    if [ ! -f freetype-${VERSION_FREETYPE}.tar.gz ]; then
        download http://download.savannah.gnu.org/releases/freetype/freetype-${VERSION_FREETYPE}.tar.gz
        exit_on_error
    fi
}


##########################################
# FRIBIDI
# http://fribidi.org/
#
download_fribidi() {
    start_download fribidi
    cd $SRC

    if [ ! -f fribidi-${VERSION_FRIBIDI}.tar.gz ]; then
        download http://fribidi.org/download/fribidi-${VERSION_FRIBIDI}.tar.gz
        exit_on_error
    fi
}


##########################################
# GIFLIB
# http://sourceforge.net/projects/giflib/
#
download_giflib() {
    start_download giflib
    cd $SRC

    if [ ! -f giflib-${VERSION_GIFLIB}.tar.bz2 ]; then
        download http://downloads.sourceforge.net/project/giflib/giflib%204.x/giflib-${VERSION_GIFLIB}/giflib-${VERSION_GIFLIB}.tar.bz2
        exit_on_error
    fi
}


##########################################
# ICONV
# http://www.gnu.org/software/libiconv/
#
download_iconv() {
    start_download iconv
    cd $SRC

    if [ ! -f libiconv-${VERSION_ICONV}.tar.gz ]; then
        download http://ftp.gnu.org/pub/gnu/libiconv/libiconv-${VERSION_ICONV}.tar.gz
        exit_on_error
    fi
}


##########################################
# JPEG
# http://www.ijg.org/
#
download_jpeg() {
    start_download jpeg
    cd $SRC

    if [ ! -f jpegsrc.v${VERSION_JPEG}.tar.gz ]; then
        download http://www.ijg.org/files/jpegsrc.v${VERSION_JPEG}.tar.gz
        exit_on_error
    fi
}


##########################################
# LAME
# http://lame.sourceforge.net/
#
download_lame() {
    start_download lame
    cd $SRC

    if [ ! -f lame-${VERSION_LAME}.tar.gz ]; then
        #download http://downloads.sourceforge.net/project/lame/lame/${VERSION_LAME}/lame-${VERSION_LAME}.tar.gz
	# Blah! 3.99.2 resides in the directory 3.99. Hardcoding that for now.
        download http://downloads.sourceforge.net/project/lame/lame/3.99/lame-${VERSION_LAME}.tar.gz
        exit_on_error
    fi
}


##########################################
# LIBBLURAY
# http://www.videolan.org/developers/libbluray.html
#
download_libbluray() {
    start_download libbluray
    cd $SRC

    if [ -d libbluray ]; then
        rm -rf libbluray
    fi
    $GIT clone git://git.videolan.org/libbluray.git libbluray
    exit_on_error
    cd libbluray

    if [ "$FIXED_REVISIONS" == "yes" ]; then
        $GIT checkout ${VERSION_LIBBLURAY}
        exit_on_error
    fi
    rm -rf ./.git
}


##########################################
# LIBDCA
# http://www.videolan.org/developers/libdca.html
#
download_libdca() {
    start_download libdca
    cd $SRC

    if [ ! -f libdca-${VERSION_LIBDCA}.tar.bz2 ]; then
        download http://download.videolan.org/pub/videolan/libdca/${VERSION_LIBDCA}/libdca-${VERSION_LIBDCA}.tar.bz2
        exit_on_error
    fi
}


##########################################
# LIBDV
# http://libdv.sourceforge.net/
#
download_libdv() {
    start_download libdv
    cd $SRC

    if [ ! -f libdv-${VERSION_LIBDV}.tar.gz ]; then
        download http://downloads.sourceforge.net/project/libdv/libdv/${VERSION_LIBDV}/libdv-${VERSION_LIBDV}.tar.gz
        exit_on_error
    fi
}


##########################################
# LIBMAD
# http://www.underbit.com/products/mad/
#
download_libmad() {
    start_download libmad
    cd $SRC

    if [ ! -f libmad-${VERSION_LIBMAD}.tar.gz ]; then
        download ftp://ftp.mars.org/pub/mpeg/libmad-${VERSION_LIBMAD}.tar.gz
        exit_on_error
    fi
}


##########################################
# LIBMEDIAINFO
# http://sourceforge.net/projects/mediainfo/
#
download_libmediainfo() {
    start_download libmediainfo
    cd $SRC

    if [ ! -f libmediainfo_${VERSION_LIBMEDIAINFO}.tar.bz2 ]; then
        download http://downloads.sourceforge.net/project/mediainfo/source/libmediainfo/${VERSION_LIBMEDIAINFO}/libmediainfo_${VERSION_LIBMEDIAINFO}.tar.bz2
        exit_on_error
    fi
}


##########################################
# LIBPNG
# http://www.libpng.org/pub/png/libpng.html
#
download_libpng() {
    start_download libpng
    cd $SRC

    if [ ! -f libpng-${VERSION_LIBPNG}.tar.gz ]; then
        download http://downloads.sourceforge.net/project/libpng/libpng15/older-releases/${VERSION_LIBPNG}/libpng-${VERSION_LIBPNG}.tar.gz
        exit_on_error
    fi
}


##########################################
# LIBOGG
# http://xiph.org/downloads/
#
download_libogg() {
    start_download libogg
    cd $SRC

    if [ ! -f libogg-${VERSION_LIBOGG}.tar.gz ]; then
        download http://downloads.xiph.org/releases/ogg/libogg-${VERSION_LIBOGG}.tar.gz
        exit_on_error
    fi
}


##########################################
# LIBVORBIS
# http://xiph.org/downloads/
#
download_libvorbis() {
    start_download libvorbis
    cd $SRC

    if [ ! -f libvorbis-${VERSION_LIBVORBIS}.tar.gz ]; then
        download http://downloads.xiph.org/releases/vorbis/libvorbis-${VERSION_LIBVORBIS}.tar.gz
        exit_on_error
    fi
}


##########################################
# LIBTHEORA
# http://xiph.org/downloads/
#
download_libtheora() {
    start_download libtheora
    cd $SRC

    if [ ! -f libtheora-${VERSION_LIBTHEORA}.tar.bz2 ]; then
        download http://downloads.xiph.org/releases/theora/libtheora-${VERSION_LIBTHEORA}.tar.bz2
        exit_on_error
    fi
}


##########################################
# LIBZEN
# http://sourceforge.net/projects/zenlib/
#
download_libzen() {
    start_download libzen
    cd $SRC

    if [ ! -f libzen_${VERSION_LIBZEN}.tar.bz2 ]; then
        download http://downloads.sourceforge.net/project/zenlib/ZenLib%20-%20Sources/${VERSION_LIBZEN}/libzen_${VERSION_LIBZEN}.tar.bz2
        exit_on_error
    fi
}


##########################################
# LZO
# http://www.oberhumer.com/opensource/lzo/
#
download_lzo() {
    start_download lzo
    cd $SRC

    if [ ! -f lzo-${VERSION_LZO}.tar.gz ]; then
        download http://www.oberhumer.com/opensource/lzo/download/lzo-${VERSION_LZO}.tar.gz
        exit_on_error
    fi
}


##########################################
# MPLAYER
# http://www.mplayerhq.hu/design7/news.html
#
download_mplayer() {
    start_download mplayer
    cd $SRC

    if [ "$FIXED_REVISIONS" == "yes" ]; then
        REVISION="-r ${VERSION_MPLAYER}"
    else
        REVISION=""
    fi

    if [ -d mplayer ]; then
        rm -rf mplayer
    fi
    $SVN export $REVISION svn://svn.mplayerhq.hu/mplayer/trunk mplayer
    exit_on_error
}


##########################################
# NCURSES
# http://www.gnu.org/software/ncurses/
#
download_ncurses() {
    start_download ncurses
    cd $SRC

    if [ ! -f ncurses-${VERSION_NCURSES}.tar.gz ]; then
        download http://ftp.gnu.org/pub/gnu/ncurses/ncurses-${VERSION_NCURSES}.tar.gz
        exit_on_error
    fi
}


##########################################
# X264
# svn://svn.videolan.org/x264/trunk
#
download_x264() {
    start_download x264
    cd $SRC

    if [ -d x264 ]; then
        rm -rf x264
    fi
    $GIT clone git://git.videolan.org/x264.git x264 -b stable
    exit_on_error
    cd x264

    #if [ "$FIXED_REVISIONS" == "yes" ]; then
    #    $GIT checkout ${VERSION_X264}
    #    exit_on_error
    #fi
    rm -rf ./.git
}


##########################################
# XVID
# http://www.xvid.org/
#
download_xvid() {
    start_download xvid
    cd $SRC

    if [ ! -f xvidcore-${VERSION_XVID}.tar.gz ]; then
        download http://downloads.xvid.org/downloads/xvidcore-${VERSION_XVID}.tar.gz
        exit_on_error
    fi
}


##########################################
# ZLIB
# http://zlib.net/
#
download_zlib() {
    start_download zlib
    cd $SRC

    if [ ! -f zlib-${VERSION_ZLIB}.tar.gz ]; then
        download http://sourceforge.net/projects/libpng/files/zlib/${VERSION_ZLIB}/zlib-${VERSION_ZLIB}.tar.gz
        exit_on_error
    fi
}


##########################################
# ENCA Extremely Naive Charset Analyser
# http://cihar.com/software/enca/
#
download_enca() {
    start_download enca
    cd $SRC

    if [ ! -f enca-${VERSION_ENCA}.tar.gz ]; then
        download http://dl.cihar.com/enca/enca-${VERSION_ENCA}.tar.gz
        exit_on_error
    fi
}


##########################################
# YASM
# http://yasm.tortall.net/
#
download_yasm() {
    start_download yasm
    cd $SRC

    if [ ! -f yasm-${VERSION_YASM}.tar.gz ]; then
        download http://www.tortall.net/projects/yasm/releases/yasm-${VERSION_YASM}.tar.gz
        exit_on_error
    fi
}


##########################################
# TSMUXER
# http://www.smlabs.net/en/products/tsmuxer/
# http://www.videohelp.com/tools/tsMuxeR
# Interesting Open Source followup project in development: https://github.com/kierank/libmpegts
#
download_tsmuxer() {
    start_download tsmuxer
    cd $SRC

    if is_osx; then
        if [ ! -f tsMuxeR-${VERSION_TSMUXER}.dmg ]; then
            $CURL --referer "http://www.videohelp.com/tools/tsMuxeR" -L http://www.videohelp.com/download/tsMuxeR_${VERSION_TSMUXER}.dmg > tsMuxeR_${VERSION_TSMUXER}.dmg
            exit_on_error
        fi
    else
        if [ ! -f tsMuxeR-${VERSION_TSMUXER}.tar.gz ]; then
            $WGET --referer="http://www.videohelp.com/tools/tsMuxeR" http://www.videohelp.com/download/tsMuxeR_${VERSION_TSMUXER}.tar.gz
            exit_on_error
        fi
    fi
}


##########################################
# PS3MEDIASERVER
# https://github.com/ps3mediaserver/ps3mediaserver
#
download_ps3mediaserver() {
    start_download ps3mediaserver
    cd $SRC

    if [ -d ps3mediaserver ]; then
        rm -rf ps3mediaserver
    fi
    $GIT clone git://github.com/ps3mediaserver/ps3mediaserver.git ps3mediaserver
    exit_on_error
    cd ps3mediaserver

    if [ "$FIXED_REVISIONS" == "yes" ]; then
        $GIT checkout ${VERSION_PS3MEDIASERVER}
        exit_on_error
    fi

    # let's clean the source
    rm -rf ./.git
    rm -rf ./src/main/external-resources/third-party/nsis/ ./src/main/external-resources/nsis/
    find . -depth -type f -name "*.dll" -exec rm -f '{}' \;
    find . -type f \( -name "*-sources.jar" -o -name "*-javadoc.jar" -o -name "*-sources.zip" -o -name "*-javadoc.zip" -o -name "*-docs.zip" \) -exec rm -vf '{}' \;
}


##########################################
# Finally, execute the script...
#

initialize

download_yasm
download_zlib
download_bzip2
download_expat
download_faad2
download_freetype
download_iconv
# Note: fontconfig requires freetype and iconv to build
download_fontconfig
download_fribidi
download_giflib
download_jpeg
download_ncurses
download_lame
download_libbluray
download_libdca
download_libdv
download_libmad
download_libzen
# Note: libmediainfo requires libzen to build
download_libmediainfo
download_libpng
download_libogg
download_libvorbis
download_libtheora
download_lzo
download_x264
download_xvid

# Build tools for including with PS3 Media Server
download_flac
download_dcraw
download_enca
download_ffmpeg
download_mplayer
download_tsmuxer
download_ps3mediaserver
