# Deploying a release

This document contains the steps required when releasing a new version of UMS.

## Preparation:
1.  Be on latest `master` branch
1.  Pull translations from Crowdin and commit them
1.  Update changelog with the changes since the last version, and the release date
1.  Decide which kind of version bump is necessary. Bugfixes or refactoring should bump the third number, others should bump the second one.
1.  Update the versions in the changelog and pom.xml (lines 33 and 66) including removing "-SNAPSHOT" from line 33
1.  Create a tag of the new version (e.g. 6.7.2, not v6.7.2)
1.  Push your commits and new tag

## Build:
1.  Run build.bat on Windows, which generates releases for Windows and Linux
1.  Append version numbers to the files
1.  Switch to macOS
1.  Pull latest master
1.  Run build.sh
1.  Append version number to the generated .dmg file

## Upload:
1.  Upload the 3 release files to spirton.com
1.  Upload to FossHub by pointing the WGET tool to the file URLs
1.  Add the release metadata to FossHub (platform, version)
1.  Toggle visibility of older release files
1.  Upload the files to SourceForge
1.  Set the new files as the default versions in SourceForge

## Visibility:
1.  Update the version/s in universalmediaserver.com/index.php
1.  Create and publish a post in the Announcements section of the forum (use previous release as a template)
1.  Post about it on Facebook (use previous post as a template). It automatically Tweets the Facebook post.
1.  Post about it on Google+ (same post as Facebook)

## Post-release:
1.  Bump the version in `pom.xml`, re-adding "-SNAPSHOT" to line 33
1.  Update the latest version in `latest_version.properties`. When this change is pushed, Windows users will see the change when they look for an update
1.  Push those changes

## Suggestions for improvements:
*   Switch to using FossHub for our automatic updater so we don't have to add to SourceForge anymore
*   Lots of "preparation" steps can be automated in the build scripts, and other parts of other sections, too