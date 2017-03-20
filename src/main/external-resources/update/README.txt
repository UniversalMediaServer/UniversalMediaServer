This directory contains one active update file (latest_version.properties).
The other file (update_1.properties) is left in for users of older versions.

The active update file is used to point the Windows automatic updater to the
latest version of UMS.
The file specifies the default version of Java (which is 7 at the moment) for
legacy purposes, but as of UMS 4.1.0 the updater updates to the same version of
Java as the one UMS is running on.

The update file's URL is defined by UPDATE_SERVER_URL in src/main/java/net/pms/configuration/Build.java.
