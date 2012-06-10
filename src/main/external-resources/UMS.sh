#!/bin/sh

CMD=`readlink -f $0`
DIRNAME=`dirname $CMD`

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
if [ "x$PMS_HOME" = "x" ]; then
    PMS_HOME=$DIRNAME
fi

export PMS_HOME
# XXX: always cd to the working dir: https://code.google.com/p/ps3mediaserver/issues/detail?id=730
cd $PMS_HOME

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
		JAVA="$JAVA_HOME/bin/java"
    else
		JAVA="java"
    fi
fi

# Setup the classpath
# since we always cd to the working dir, these a) can be unqualified and b) *must*
# be unqualified: https://code.google.com/p/ps3mediaserver/issues/detail?id=1122
PMS_JARS="update.jar:ums.jar"

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    PMS_HOME=`cygpath --path --windows "$PMS_HOME"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
fi

# Execute the JVM
exec "$JAVA" $JAVA_OPTS -Xmx768M -Xss1024k -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true -Djna.nosys=true -classpath "$PMS_JARS" net.pms.PMS "$@"
