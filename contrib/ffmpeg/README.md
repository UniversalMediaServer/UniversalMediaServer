# Build FFmpeg for PMS

This directory contains a simple CMake-based FFmpeg static build helper for PS3 Media Server.

It currently works on Linux, OpenBSD, FreeBSD, and Mac OS X. It has been tested most extensively on Linux/x86 (Ubuntu 12.04).
The helper will automatically grab the latest versions of the relevant FFmpeg dependencies. All build files are stored in a
subdirectory of this directory and no system files are read or written to.

## Prerequisites <a name="Prerequisites"></a>

This helper requires:

- a POSIX-compliant system.
- recent versions of [gcc](http://gcc.gnu.org/) and [yasm](http://yasm.tortall.net/) (1.1+).
- a recent version of [CMake](http://www.cmake.org/) (2.8.1+).
- the [autoconf](http://www.gnu.org/software/autoconf/) and [libtool](http://www.gnu.org/software/libtool/) utilities.
- the [pkg-config](http://www.freedesktop.org/wiki/Software/pkg-config) utility.
- [perl](http://www.perl.org/) >= 5.000 is required by the OpenSSL Configure script.

## Usage <a name="Usage"></a>

After [checking out PMS](https://github.com/ps3mediaserver/ps3mediaserver/blob/master/BUILD.md#short-instructions),
just type the following commands at the shell prompt:

    $ cd contrib/ffmpeg
    $ make

Then go grab a coffee (or maybe two). The helper will download and compile all FFmpeg dependencies for you.
Once done, you should get a static system-independent FFmpeg binary in the `build/bin` directory.

    $ ./build/bin/ffmpeg
    ffmpeg version 1.0 (PMS1) for PS3 Media Server Copyright (c) 2000-2012 the FFmpeg developers
      built on Oct 10 2012 09:20:17 with gcc 4.6 (Ubuntu/Linaro 4.6.3-1ubuntu5)
      configuration: http://git.io/ZHdseg
    Hyper fast Audio and Video encoder
    usage: ffmpeg [options] [[infile options] -i infile]... {[outfile options] outfile}...

    $ ldd ./build/bin/ffmpeg
    not a dynamic executable

From there, you may use the binary immediately or build a Debian package for later deployment (see below).

## Packaging <a name="Packaging"></a>

You can optionally build a Debian package by typing the following command at the shell prompt:

    $ make deb

All binaries and support files will be installed by the package in the `/usr/local` directory.

    $ sudo dpkg -i ffmpeg-pms_1.0.0_amd64.deb
    Selecting previously deselected package ffmpeg-pms.
    Unpacking ffmpeg-pms (from ffmpeg-pms_1.0.0_amd64.deb) ...
    Setting up ffmpeg-pms (1.0.0) ...

## Tips <a name="Tips"></a>

To find cross-platform variables for build tools &c. look in build/CMakeCache.txt.

To see build output suppressed by automake:

    make V=1

or:

    ./configure --disable-silent-rules

See [here](https://lists.gnu.org/archive/html/bug-autoconf/2012-01/msg00009.html) for more details.

To make a specific target:

	cd build
	make <target> # eg make nettle

`--enable-static` and `--disable-shared` work with most configure scripts. If `--enable-shared` is not supported,
it's usually ignored (and reported as a warning at the end of the config.log).

Type `./configure --help` to see the configuration options. Removing unused features such as documentation and binaries
can speed up build time, work around bugs, and reduce dependency wrangling.

### Troubleshooting:

Check the package's config.log.

Extract the command lines from build/CMakeFiles/&lt;package&gt;.dir/build.make and try running them manually.

## Credits <a name="Credits"></a>

The helper and this documentation are based on [sffmpeg](https://github.com/pyke369/sffmpeg) by [pyke369](https://github.com/pyke369).

## Version <a name="Version"></a>

PMS4
