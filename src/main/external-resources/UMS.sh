#!/bin/sh

CMD=$(readlink -f "$0")
DIRNAME=$(dirname "$CMD")

# OS specific support (must be 'true' or 'false').
cygwin=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
    [ -n "$PMS_HOME" ] &&
		PMS_HOME=`cygpath --unix "$PMS_HOME"`
    [ -n "$JAVA_HOME" ] &&
		JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

# Setup PMS_HOME
if [ -z "$PMS_HOME" ]; then
    PMS_HOME="$DIRNAME"
fi

export PMS_HOME
# XXX: always cd to the working dir: https://code.google.com/p/ps3mediaserver/issues/detail?id=730
cd "$PMS_HOME" || exit 1

# Setup the JVM
if [ -z "$JAVA" ]; then
    if [ -z "$JAVA_HOME" ]; then
        JAVA="java"
    else
        JAVA="$JAVA_HOME/bin/java"
    fi
fi

# Use our JVM if it exists
if [ -x jre25/bin/java ]; then
    JAVA="jre25/bin/java"
fi

# Setup the classpath
# since we always cd to the working dir, these a) can be unqualified and b) *must*
# be unqualified: https://code.google.com/p/ps3mediaserver/issues/detail?id=1122
PMS_JARS="update.jar:ums.jar"

# Check the selected executable, including JAVA/JAVA_HOME overrides, before starting UMS.
JAVA_VERSION=$("$JAVA" -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*java.specification.version = //p')
if [ "$JAVA_VERSION" != "25" ]; then
    echo "UMS requires Java 25. Selected executable: $JAVA (version: ${JAVA_VERSION:-unknown})." >&2
    echo "Install JDK 25 and set JAVA_HOME, or use the bundled jre25 runtime." >&2
    exit 1
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    PMS_HOME=`cygpath --path --windows "$PMS_HOME"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
    PMS_JARS=$(cygpath --path --windows "$PMS_JARS")
fi

# Configure fontconfig (used by our build of FFmpeg)
if [ -z "$FONTCONFIG_PATH" ]; then
    FONTCONFIG_PATH=/etc/fonts
    export FONTCONFIG_PATH
fi
if [ -z "$FONTCONFIG_FILE" ]; then
    FONTCONFIG_FILE=/etc/fonts/fonts.conf
    export FONTCONFIG_FILE
fi

# Provide a means of setting max memory using an environment variable
if [ -z "$UMS_MAX_MEMORY" ]; then
    UMS_MAX_MEMORY=2048M
fi

# Execute the JVM
exec "$JAVA" $JAVA_OPTS -Xmx$UMS_MAX_MEMORY -Xss2048k --add-modules=jdk.incubator.vector --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true -Djna.nosys=true -classpath "$PMS_JARS" net.pms.PMS "$@"
