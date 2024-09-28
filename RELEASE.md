# Release instructions

There are a number of manual steps involved in a new version release. This list is meant to document them all.

## Pre-release

1. Pull the latest translations from Crowdin by running `mvn crowdin:pull`

     This relies on the [Crowdin Maven Plugin](https://github.com/DigitalMediaServer/crowdin-maven-plugin/)
1. Update [the changelog](./CHANGELOG.md) by changing the `Unreleased` section to the version name (see the next step for which value to use) and populating the sections.

     There are 4 usual sections in a changelog:
   - `General` contains most of our code changes like new features, improvements and bugfixes
   - `Translation updates via Crowdin` contains thanks and progress percentages
   - `Media players` contains changes that are specific to certain media players
   - `Dependencies` is mostly automated by [the changelog GitHub Action](https://github.com/UniversalMediaServer/UniversalMediaServer/blob/47ed539c03f01f5198988a9a2388ae2aafc5a998/.github/workflows/ci.yaml#L258-L276) supported by dangoslen/dependabot-changelog-helper and stefanzweifel/git-auto-commit-action.
      See the previous releases in the file to match existing conventions.

      Make sure you keep the `[Unreleased]` section up the top, because the changelog GitHub Action relies on that.

1. Update the version in [pom.xml](./pom.xml).

     The version number will be dictated by the changes that are included in the release. There are 4 types of releases that can happen:
   - `Major (1.2.3 to 2.0.0)` is for a large feature-set and is usually the result of beta releases to get community feedback
   - `Minor (1.2.3 to 1.3.0)` is for minor features or added media player configs
   - `Patch (1.2.3 to 1.2.4)` is for bugfixes
   - `Hotfix (1.2.3 to 1.2.3.1)` is rare, unplanned and contains a fix for an urgent problem in the last release

     When you know the version number to choose, it should be updated in the `<version>` and the `<project.version.short>` parts of pom.xml. That will include removing `-SNAPSHOT` from `<version>`, which will be added back later.

1. Commit those changes in a commit message with the version name
1. Tag the release with the same version name
1. Push the commit and tag to GitHub

## Release

1. Compile the releases, the instructions are detailed in [the How to build UMS wiki article](https://github.com/UniversalMediaServer/UniversalMediaServer/wiki/How-to-build-UMS).

     I (SubJunk) use 3 machines to compile the releases, mostly due to macOS making things difficult. I compile the Windows and Linux releases on my Windows PC, which is a simple `.bat` file with the commands in it.

     For 2 the 3 types of macOS releases (pre-10.15 and default) I use an older Intel MacBook with x86_64 architecture. This also requires signing with my paid Apple Developer account, which I sign with our forked version of the [Gon](https://github.com/UniversalMediaServer/gon) project since the original project is archived. That requires some setup that should be documented in this file.

     I also do the Docker release from that Intel MacBook, which requires the `linux` directory to be manually created/updated to contain the binaries for Linux.

     The script for the two paragraphs above is [ums-release-macos-intel.sh](./scripts/ums-release-macos-intel.sh), and it contains its own setup instructions to elaborate on this.

     For the remaining macOS release (ARM) I use a newer MacBook Pro with an M1 chip, which also uses Gon for signing/notarizing, and the [ums-release-macos-arm.sh](./scripts/ums-release-macos-arm.sh) script.

1. Upload the releases. 

     After each of the 3 machines are done compiling, I upload the binaries directly to Patreon. The first machine you do it on will create draft post, then you can find the draft post on the remaining machines and add their binaries to that. I hope Patreon will add that ability to their API some day!

1. Write a release post.

     In the text area in Patreon, copy the previous release to get the template, and change the summary of the changes.

1. Add a pre-release on GitHub.

     Copy the previous release text from GitHub to get the template, but replace the summary text with the one you used on Patreon. Customize the Patreon link to point to the new post, and check the checkboxes to create a discussion and that this is a pre-release. There are no files for this release on GitHub yet, because GitHub is always one release behind Patreon.

## Post-release

1. Upload the previous release binaries to GitHub and remove the `This release is available via our Patreon page, and will be available here when the next release happens.` part. Make sure you check the checkbox for `Set as the latest release` because you are now promoting it from pre-release to latest.

1. The automatic updater in UMS downloads from GitHub so now you are ready to point that to the previous release you just promoted. You can do that by updating the versions on the first 14 lines of [latest_version.properties](./src/main/external-resources/update/latest_version.properties#L1-L14). Remember this is the one you just promoted to the first public release, NOT the one on Patreon.

1. Nothing to do for other release repositories like [Chocolatey](https://community.chocolatey.org/packages/ums) and [Homebrew](https://formulae.brew.sh/cask/universal-media-server), and third-party mirrors like [Major Geeks](https://www.majorgeeks.com/files/details/universal_media_server.html) and [VideoHelp](https://www.videohelp.com/software/Universal-Media-Server) because they have their own automations.

1. Add `-SNAPSHOT` to the `<version>` in `pom.xml` to show you are now ahead of the version you just released to Patreon.

1. Commit the changes as `Post-release` and push.

1. Update the website config.php and upload to the web server (this is done manually for now, should be automated). When the file is uploaded there will be up to a 1-hour delay until it is visible on the website, because of a 60-minute cache on the front page.

1. Post about the release

     - X: https://x.com/UMS16
     - Facebook: https://www.facebook.com/UniversalMediaServer
     - Our forum: https://www.universalmediaserver.com/forum/viewforum.php?f=3

     Follow the conventions for the previous posts. The forum announcement will automatically add an entry to the front page and the News section of our website.