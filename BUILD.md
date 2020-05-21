## Table of Contents

- [Build instructions](#build-instructions)
- [Short instructions](#short-instructions)
- [Full instructions](#full-instructions)
	- [1. Download and install the Java JDK](#1-download-and-install-the-java-8-jdk)
	- [2. Download and install Git](#2-download-and-install-git)
	- [3. Download and extract Maven](#3-download-and-extract-maven)
	- [4. Set environment variables](#4-set-environment-variables)
		- [Windows](#windows-3)
		- [Linux](#linux-3)
		- [macOS](#macos-3)
	- [5. Download the UMS source code](#5-download-the-ums-source-code)
	- [6. Resolve and install external libraries](#6-resolve-and-install-external-libraries)
	- [7. Update to the latest source (optional)](#7-update-to-the-latest-source-optional)
	- [8. Compile the latest version of UMS](#8-compile-the-latest-version-of-ums)
	- [Automatic builds](#automatic-builds)
		- [Windows](#windows-4)
		- [Linux, macOS &c.](#linux-macos-&c)
- [Cross-compilation](#cross-compilation)
	- [Building the Windows binaries](#building-the-windows-binaries)
		- [On Linux](#on-linux)
		- [On macOS](#on-macos)
	- [Building the Linux tarball](#building-the-linux-tarball)
		- [On Windows](#on-windows)
		- [On macOS](#on-macos-1)
	- [Building the macOS wizard installer](#building-the-macos-wizard-installer)
- [Quick builds](#quick-builds)

# Build instructions

The latest release of Universal Media Server can be downloaded from: https://www.universalmediaserver.com

This document describes how to build Universal Media Server from the source files.
The following software packages are required:

* The Java JDK (the JRE is not enough)
* Git
* Maven
* External libraries

Read the [Full instructions](#full-instructions) section for a complete explanation of how to
install all required software and how to build UMS for each operating system.

# Short instructions

If all required software packages are installed, the following commands will
download the latest sources and build UMS:

    git clone git://github.com/UniversalMediaServer/UniversalMediaServer.git
    cd UniversalMediaServer
    mvn package

The result will be built in the "target" directory:

* Windows: `UMS-x.xx.x-setup.exe`
* Linux: `ums-x.xx.x-distribution.tar.gz`
* macOS: `ums-x.xx.x-distribution/Universal Media Server.app`

# Full instructions

First all required software has to be installed:

## 1. Download and install the Java 8 JDK

See https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html

## 2. Download and install Git

See https://git-scm.com/

## 3. Download and extract Maven

See http://maven.apache.org/

## 4. Set environment variables

### Windows

Create new variables or append the value if the variable already exists:

* Level: System, variable: `JAVA_HOME`, value: JDK install location
* Level: User, variable `M2_HOME`, value: Maven extract location
* Level: User, variable `M2`, value: `%M2_HOME%\bin`
* Level: User, variable `PATH`, value `%M2%`

### Linux

Nothing to do.

### macOS

Nothing to do.

## 5. Download the UMS source code

    git clone git://github.com/UniversalMediaServer/UniversalMediaServer.git
    cd UniversalMediaServer

## 6. Resolve and install external libraries

These are needed by the build process:

    mvn external:install

At this point all required software packages are present.
UMS is now ready to be built.

## 7. Update to the latest source (optional)

    git pull

## 8. Compile the latest version of UMS

    mvn package

The resulting binaries will be built in the "target" directory:

* Windows: `UMS-setup-full.exe`, `UMS-setup-full-x64.exe` and `UMS-setup-without-jre.exe`
* Linux:   `ums-linux-generic-x.xx.x.tar.gz`
* macOS: `ums-setup-macosx-x.xx.x.tar.gz`

## Automatic builds

These last two commands can easily be automated using a script e.g.:

### Windows

    rem build-ums.bat
    start /D UniversalMediaServer /wait /b git pull
    start /D UniversalMediaServer /wait /b mvn package

### Linux, macOS &c.

    #!/bin/sh
    # build-ums.sh
    cd UniversalMediaServer
    git pull
    mvn package linux-*

where `*` is one of: x86, x86_64, arm64, armel, or armhf

# Cross-compilation

This section explains how it is possible to compile for one system while on another.

## Building the Windows binaries

The Windows installers (`UMS-version.exe`) and Windows executable (`UMS.exe`) can be built on non-Windows platforms.

First of all, you'll need to have the `makensis` binary installed. On Debian/Ubuntu,
this can be done with:

    sudo apt-get install nsis

Then the `NSISDIR` environment needs to be set to the **absolute path** to the
`nsis` directory. This can either be set per-command:

    NSISDIR=$PWD/src/main/external-resources/third-party/nsis mvn ...

\- temporarily in the current shell:

    export NSISDIR=$PWD/src/main/external-resources/third-party/nsis
    mvn ...

\- or permanently:

    # these two commands only need to be run once
    echo "export NSISDIR=$PWD/src/main/external-resources/third-party/nsis" >> ~/.bashrc
    source ~/.bashrc

    mvn...

For the sake of brevity, the following examples assume it has already been set.

The Windows installer can now be built with one of the following commands:

### On Linux and macOS

    mvn package -P system-makensis,windows

## Building a Linux tarball

### On Windows and macOS

    mvn package -P linux-*

where `*` is one of: x86, x86_64, arm64, armel, or armhf

## Building the macOS disk image

### On Windows and Linux

    mvn package -P macos

## Building the macOS wizard installer

1) Build UMS
2) Install http://s.sudre.free.fr/Software/Packages/about.html
3) Set a variable storing the directory path of the build distribution file, e.g.
```
export UMS_DIST_FOLDER="/Users/dev/ums/target/ums-7.3.1-SNAPSHOT-distribution/Universal Media Server.app"
export UMS_LOGO_FILE="/Users/dev/ums/src/main/external-resources/third-party/nsis/Contrib/Graphics/Wizard/win.png"
```
4) Replace desired path inside the  .pkgproj file
```
sed -i '' "s#UMS_DIST_FOLDER#$UMS_DIST_FOLDER#g" src/main/assembly/osx-installer.pkgproj
sed -i '' "s#UMS_LOGO_FILE#$UMS_LOGO_FILE#g" src/main/assembly/osx-installer.pkgproj
```
5) Build .pkg installer. This will output to `/target/Universal Media Server.pkg`
```
/usr/local/bin/packagesbuild src/main/assembly/osx-installer.pkgproj
```

# Quick builds

We have quick build scripts that are recommended during development for fast
iteration. The scripts will compile the Java code, put it in the default install
directory, and run the program, which will close any existing instance of UMS.

It should work for 64-bit Windows and macOS. Can be extended for others easily if desired.

    mvn verify -P quickrun-* -DskipTests

Where `*` is `macos` or `windows`
