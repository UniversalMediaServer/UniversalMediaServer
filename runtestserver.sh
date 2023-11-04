#!/bin/sh

dir="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"

CMD=`readlink -f $0`
DIRNAME=`dirname "$CMD"`

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
cd "./"

# Setup the JVM
if [ -z "$JAVA" ]; then
    if [ -z "$JAVA_HOME" ]; then
        JAVA="java"
    else
        JAVA="$JAVA_HOME/bin/java"
    fi
fi

# Use our JVM if it exists
if [ -f jre17/bin/java ]; then
    JAVA="jre17/bin/java"
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    PMS_HOME=`cygpath --path --windows "$PMS_HOME"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
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
    UMS_MAX_MEMORY=1280M
fi

# Execute the JVM
exec "$JAVA" $JAVA_OPTS -Xmx$UMS_MAX_MEMORY -Xss2048k -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true -Djna.nosys=true -classpath "ums-testserver.jar" net.pms.PMS "$@"
