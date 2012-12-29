This directory contains update files. An update is only made available if it is compatible with the current release. A release is compatible if it doesn't break too many things. Given a MAJOR.MINOR.REVISION version (e.g. 1.30.1):

    Current versioning (flagging breaking changes by incrementing MINOR):

        changes in MAJOR are always incompatible
        changes in MINOR are compatible (e.g. 1.60.0 -> 1.61.0) unless the most-significant digit is incremented (e.g. 1.60.0 -> 1.70.0)
        changes in REVISION are always compatible

    Future versioning (semantic versioning):

        changes in MAJOR are always incompatible
        changes in MINOR or REVISION are always compatible

Prior to 1.5.0, many different files were used to manage versioning as the logic was effectively implemented in these files rather than in the code. The code is now smart enough to figure out for itself if an update is compatible, so the current file - latest_version.properties - should be the last one that needs to be created.

The code that determines whether the current version of PMS is compatible with the latest version is implemented in Version.isPmsCompatible. If this logic changes (e.g. if MAJOR rather than MINOR is the "breaking" number), this code (and its tests) will need to be updated.

Notes:

The updater gracefully handles missing files and network errors.

Old update files should not be removed without discussion. It may make sense to remove obsolete files in a future release.

The update file's URL is defined by UPDATE_SERVER_URL in src/main/java/net/pms/configuration/Build.java.
