# Changelog

## [13.8.1](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/13.8.0...13.8.1) (2023-12-08)

### General
- Fixed transcoding on renderers with MediaInfo=false
- Added ability to add end-to-end web browser tests on Chrome, Firefox and Safari via Playwright
- Improved logging detail and test coverage (thanks, Priyanka Ghosh Dastidar!)
- Made server tests not run when only front-end code has changed

### Translation updates via Crowdin
- English (UK) (49%) (thanks, Pete Russell!)
- Estonian (13%) (thanks, Avernys!)
- French (100%) (thanks, Fredo1650!)
- Romanian (48%) (thanks, Bogdan Ungureanu!)
- Spanish (61%) (thanks, Diego León Giraldo Gómez!)

### Dependencies
- Bump FFmpeg to 6.1
- Bump Java to 17.0.9+11
- Bump MediaInfo to 23.10
- lock file maintenance
- update dependency @testing-library/jest-dom to v6.1.5
- update dependency @testing-library/react to v14.1.2
- update dependency @types/jest to v29.5.11
- update dependency @types/lodash to v4.14.202
- update dependency @types/node to v18.18.13
- update dependency @types/react to v18.2.40
- update dependency @types/react-color to v3.0.10
- update dependency @types/videojs-hls-quality-selector to v1.1.3
- update dependency axios to v1.6.2
- update dependency ch.qos.logback:logback-classic to v1.4.12
- update dependency com.drewnoakes:metadata-extractor to v2.19.0
- update dependency com.fasterxml.jackson.core:jackson-databind to v2.16.0
- update dependency com.github.eirslett:frontend-maven-plugin to v1.14.2
- update dependency com.github.oshi:oshi-core to v6.4.8
- update dependency com.ibm.icu:icu4j to v74
- update dependency com.puppycrawl.tools:checkstyle to v10.12.5
- update dependency com.sun.xml.messaging.saaj:saaj-impl to v3.0.3
- update dependency com.zaxxer:hikaricp to v5.1.0
- update dependency commons-io:commons-io to v2.15.1
- update dependency eslint to v8.54.0
- update dependency org.apache.commons:commons-lang3 to v3.14.0
- update dependency org.apache.commons:commons-text to v1.11.0
- update dependency org.apache.maven.plugins:maven-checkstyle-plugin to v3.3.1
- update dependency org.apache.maven.plugins:maven-surefire-plugin to v3.2.2
- update dependency org.codehaus.mojo:exec-maven-plugin to v3.1.1
- update dependency react-router-dom to v6.20.1
- update junit5 monorepo to v5.10.1
- update react monorepo
- update twelvemonkeys-imageio-version to v3.10.1
- update typescript-eslint monorepo to v6.12.0
- update yarn to v3.7.0

## [13.8.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/13.7.0...13.8.0) (2023-10-21)

### General
- System tray icon indicates when an update is available (thanks, Ty Lovejoy!)
- Faster playback start and seeking via tsMuxeR
- Fixed broken language parsing for locale-specific variants, like "en (US)" or "fr (CA)"
- Fixed incorrect DLNA.ORG_PN values for transcoded video
- Fixed deferring to tsMuxeR when video codec is not supported
- Fixed settings changes not saving
- Fixed using MPEG-2 DLNA.ORG_PN value for H.265 videos
- Fixed some Android-based renderers being incorrectly detected as Pigasus
- Fixed order of results in Media Library when sorted by date (thanks for reporting, stnnzp!)
- Fixed migration of IP Filter settings from v12 (thanks, dytlzl!)
- Fixed UPnP search responses
- Fixed Live Subtitles folder showing in front page of web player (#3669) (thanks, Priyanka Ghosh Dastidar!)

### Renderers
- Fixed video support on some Samsung TVs (thanks, Fredo1650!)

### Dependencies
- lock file maintenance
- update actions/checkout action to v4
- update dependency @testing-library/jest-dom to v6.1.4
- update dependency @testing-library/user-event to v14.5.1
- update dependency @types/jest to v29.5.6
- update dependency @types/lodash to v4.14.200
- update dependency @types/node to v18.18.6
- update dependency @types/react to v18.2.22
- update dependency @types/react-color to v3.0.9
- update dependency @types/videojs-hls-quality-selector to v1.1.2
- update dependency axios to v1.5.1
- update dependency com.fasterxml.jackson.core:jackson-databind to v2.15.3
- update dependency com.github.eirslett:frontend-maven-plugin to v1.14.0
- update dependency com.github.oshi:oshi-core to v6.4.6
- update dependency com.puppycrawl.tools:checkstyle to v10.12.4
- update dependency com.sun.xml.bind:jaxb-impl to v4.0.4
- update dependency commons-io:commons-io to v2.14.0
- update dependency eslint to v8.51.0
- update dependency org.apache.maven.plugins:maven-enforcer-plugin to v3.4.1
- update dependency org.slf4j:slf4j-api to v2.0.9
- update dependency react-router-dom to v6.16.0 
- update mantine monorepo packages to v6.0.21
- update react monorepo
- Update tsMuxeR to 2023-09-20-01-52-31
- update typescript-eslint monorepo to v6.7.5
- update yarn to v3.6.4

## [13.7.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/13.6.0...13.7.0) (2023-09-06)

### General
- Fixed security vulnerabilities
- Fixed unnecessary transcoding in some situations
- Fixed sending incorrect colorDepth for some videos
- Fixed check for external subtitles format support
- Fixed Windows 11 sleep delay time
- Fixed Fully Played feature not working on certain devices (#3947) (thanks, Fredo1650!)
- Fixed bugs with video playback on web player

### Renderers
- Fixed EAC3 (Dolby Digital Plus) support on some Samsung TVs (#4039) (thanks, Fredo1650!)

### Translation updates via Crowdin
- Catalan (57%) (thanks, Ramon Fonts Oliveras!)
- English (United Kingdom) (47%) (thanks, Pete Russell!)
- German (100%) (thanks, Da Ma and Marco Kubitza (Kubi)!)
- Hungarian (57%) (thanks, Zan1456!)
- Italian (67%) (thanks, sisar4!)
- Norwegian (40%) (thanks, Fredrik Sk!)
- Turkish (100%) (thanks, Burak Yavuz!)

### Dependencies
- Bump JRE to 17.0.8.1+1
- lock file maintenance
- update dependency @testing-library/jest-dom to v6
- update dependency @types/jest to v29.5.4
- update dependency @types/lodash to v4.14.197
- update dependency @types/node to v18.17.13
- update dependency @types/react to v18.2.21
- update dependency axios to v1.5.0
- update dependency com.github.oshi:oshi-core to v6.4.5
- update dependency com.puppycrawl.tools:checkstyle to v10.12.3
- update dependency eslint to v8.48.0
- update dependency eslint-plugin-react to v7.33.2
- update dependency org.apache.maven.plugins:maven-enforcer-plugin to v3.4.0
- update dependency react-router-dom to v6.15.0
- update dependency typescript to v5.2.2
- update dependency video.js to v8.5.2
- update logback-version to v1.4.11
- update mantine monorepo packages to v6.0.19
- update typescript-eslint monorepo to v6.5.0
- update yarn to v3.6.3

## [13.6.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/13.5.0...13.6.0) (2023-07-29)

### General
- Added support for automatic muxing of HDR streams for better video quality (e.g. Dolby Vision in MKV on LG TVs)
- Improved parsing of SDTV filenames
- Improved FFmpeg engine performance
- Improved support for video playback on Safari
- Fixed H.265 muxing via tsMuxeR
- Fixed support for latest MediaInfo versions
- Fixed recognition of renderers by UUID
- Fixed name of playing file not showing up on old status tab
- Fixed security vulnerabilities
- Fixed thumbnail generation bugs (thanks, Chris Kroells!)
- Fixed deferring to tsMuxeR when FFmpeg can mux the video
- Fixed frozen media browsing
- Fixed RTL language support in web settings
- Added logging of subtitles default and forced details

### Renderers
- Added support for Dolby Vision in MKV on LG TVs
- Fixed detection of VLC for macOS
- Fixed native Opus audio support on LG TVs
- Fixed native AVI/XviD support on Fetch TV, LG TVs, Panasonic DMR, and Sony TVs

### Translation updates via Crowdin
- Removed inconsistent trailing dot (thanks, Paul Furlet!)
- Afrikaans (13%) (thanks, HencoSmit!)
- Bulgarian (49%) (thanks, Dremski!)
- Catalan (57%) (thanks, Antoni Grau i Quellos!)
- Czech (82%)
- Danish (64%) (thanks, GurliGebis!)
- Finnish (64%) (thanks, Esko Gardner!)
- Korean (63%) (thanks, VenusGirl!)
- Polish (62%) (thanks, Karol Szastok!)
- Portuguese, Brazilian (66%) (thanks, Mauro.A and Vyctor Oliveira!)
- Russian (77%) (thanks, Олег Лойко!)
- Turkish (87%) (thanks, Burak Yavuz!)
- Ukrainian (20%) (thanks, Paul Furlet!)

### Dependencies
- lock file maintenance
- Bump FFmpeg and tsMuxeR to latest
- Bump MediaInfo to 23.06
- Bump semver from 6.3.0 to 6.3.1
- update dependency @testing-library/jest-dom to v5.17.0
- update dependency @types/jest to v29.5.3
- update dependency @types/lodash to v4.14.196
- update dependency @types/node to v18.17.1
- update dependency @types/react to v18.2.17
- update dependency com.github.eirslett:frontend-maven-plugin to v1.13.4
- update dependency com.github.junrar:junrar to v7.5.5
- update dependency com.github.oshi:oshi-core to v6.4.4
- update dependency eslint to v8.46.0
- update dependency eslint-plugin-react to v7.33.0
- update dependency org.apache.commons:commons-lang3 to v3.13.0
- update dependency org.jupnp:org.jupnp to v2.7.1
- update dependency react-router-dom to v6.14.2
- update dependency web-vitals to v3.4.0
- update junit5 monorepo to v5.10.0
- update mantine monorepo packages to v6.0.17
- update react monorepo
- update typescript-eslint monorepo to v6.2.0
- update dependency video.js to v8.5.1
- update yarn to v3.6.1

## [13.5.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/13.4.1...13.5.0) (2023-07-04)

### General
- Added support for default and forced flags on subtitles on web player
- Improve handling of web interface when server is offline
- Fixed metadata not displaying when Media Library folder is disabled
- Fixed subtitles without titles not working on web player
- Formatting (thanks, RichardIcecube!)
- Logging improvements

### Renderers
- Fix H.265 support on Freebox (thanks, ouaklafoud!)

### Dependencies
- lock file maintenance
- update dependency @emotion/react to v11.11.1
- update dependency @types/node to v18.16.18
- update dependency @types/react to v18.2.14
- update dependency com.ibm.icu:icu4j to v73.2
- update dependency com.puppycrawl.tools:checkstyle to v10.12.1
- update dependency com.sun.xml.bind-version to v4.0.3
- update dependency commons-io:commons-io to v2.13.0
- update dependency eslint to v8.43.0
- update dependency frontend-maven-plugin to 1.13.3
- update dependency hls.js to v1.4.6
- update dependency mantine to v6.0.15
- update dependency net.coobird:thumbnailator to v0.4.20
- update dependency react-router-dom to v6.14.1
- update dependency stylis to v4.3.0
- update dependency typescript to v5.1.6
- update logback-version to v1.4.8
- update typescript-eslint monorepo to v5.60.1
- update video.js to 8.5.0
- update videojs-contrib-quality-levels to 4.0.0
- update yarn to v3.6.0

## [13.4.1](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/13.4.0...13.4.1) (2023-06-07)

### General

- Fixed broken SystemUpdateId update
- Fixed broken filename parsing with some HDR files
- Fixed web player video playback bugs
- Fixed security vulnerabilities
- Fixed muxing Dolby Vision with tsMuxeR
- Updated Javadocs (thanks, RichardIcecube!)

### Translation updates via Crowdin
- Bulgarian (47%) (thanks, STOYAN STOYANOV!)
- Catalan (57%) (thanks, Antoni Grau i Quellos!)
- Danish (64%) (thanks, GurliGebis!)
- Japanese (59%) (thanks, was0914!)
- Swedish (51%) (thanks, Erik Karlsson!)

### Dependencies
- lock file maintenance
- update dependency @emotion/react to v11.11.0 
- update dependency @types/jest to v29.5.2
- update dependency @types/lodash to v4.14.195
- update dependency @types/node to v18.16.16
- update dependency @types/react to v18.2.8
- update dependency @types/react-dom to v18.2.4
- update dependency @types/video.js to v7.3.52
- update dependency axios to v1.4.0
- update dependency com.fasterxml.jackson.core:jackson-databind to v2.15.2
- update dependency com.github.oshi:oshi-core to v6.4.3
- update dependency com.puppycrawl.tools:checkstyle to v10.12.0
- update dependency commons-io:commons-io to v2.12.0
- update dependency eslint to v8.42.0
- update dependency hls.js to v1.4.5
- update dependency org.apache.maven.plugins:maven-assembly-plugin to v3.6.0 
- update dependency org.apache.maven.plugins:maven-checkstyle-plugin to v3.3.0
- update dependency org.apache.maven.plugins:maven-surefire-plugin to v3.1.2
- update dependency react-router-dom to v6.11.2
- update dependency stylis to v4.2.0
- update dependency web-vitals to v3.3.2
- Update JRE to 17.0.7+7
- update junit5 monorepo to v5.9.3
- update logback-version to v1.4.7
- update react monorepo to v18.2.0
- Update tsMuxeR to 2023-04-13-02-05-26
- update typescript-eslint monorepo to v5.59.9
- update yarn to v3.5.1

## [13.4.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/13.3.0...13.4.0) (2023-04-19)

### General:
- Added Composer and Conductor to UPnP results, for better handling of classical music
- Fixed Windows 11 going to sleep too soon (#3883)
- Fixed files being marked as fully played on playback failure or media parsing requests (#1479 and #3683)
- Fixed metadata API results for some files
- Faster CI (thanks, optimizing-ci-builds!)
- Fixed crash when audio file has no audio track
- Fixed bugs with UPnP searching

### Translation updates via Crowdin
- Afrikaans (22%) (thanks, John Botes and stefan ivanov!)
- Bulgarian (79%) (thanks, stefan ivanov!)
- Chinese (Traditional) (78%) (thanks, RX78!)
- French (100%) (thanks, Archaos and Vincent Panel!)
- Japanese (94%) (thanks, elepro!)
- Ukrainian (28%) (thanks, Paul Furlet!)

### Dependencies
- lock file maintenance
- update dependency @types/jest to v29.5.0
- update dependency @types/lodash to v4.14.194
- update dependency @types/node to v18.15.11
- update dependency @types/react to v18.0.37
- update dependency axios to v1.3.5
- update dependency com.auth0:java-jwt to v4.4.0
- update dependency com.ibm.icu:icu4j to v73
- update dependency com.github.oshi:oshi-core to v6.4.1
- update dependency com.puppycrawl.tools:checkstyle to v10.9.3
- update dependency com.sun.xml.messaging.saaj:saaj-impl to v3.0.1
- update dependency eslint to v8.38.0
- update dependency hls.js to v1.4.0
- update dependency org.apache.maven.plugins:maven-enforcer-plugin to v3.3.0
- update dependency org.apache.maven.plugins:maven-resources-plugin to v3.3.1
- update dependency org.slf4j:slf4j-api to v2.0.7
- update dependency react-country-flag to v3.1.0 
- update dependency react-router-dom to v6.10.0
- update dependency typescript to v5
- update dependency web-vitals to v3.3.1
- update typescript-eslint monorepo to v5.59.0
- update yarn to v3.5.0

## [13.3.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/13.2.1...13.3.0) (2023-03-16)

### General:
- Added HDR video muxing to FFmpeg video engine
- Added support for UPnP searching music by genre
- Use ENTRYPOINT instead of CMD to launch on Docker (thanks, Alfonso Montero!)
- Fixed support for HLG HDR files
- Fixed compatibility for certain Dolby Vision and HDR10+ HDR profiles
- Fixed server startup error if API is enabled and down (thanks, Iridias!)
- Fixed server startup error on Linux sometimes
- Fixed detection of VLC on some non-English systems (thanks, LMS!)
- Fixed the music Artist field being set to the Performer

### Renderers:
- Fixed detection of Sony Xperia devices

### Translation updates via Crowdin
- Chinese Simplified (100%) (thanks, QI wolong!)
- Chinese Traditional (78%) (thanks, Simon Lee!)
- French (100%) (thanks, Ydrana!)
- Japanese (93%) (thanks, elepro!)
- Serbian (Cyrillic) (79%) (thanks, Bojan Maksimovic!)

### Dependencies:
- Bump all subdependencies
- update dependency @emotion/react to v11.10.6
- update dependency @testing-library/react to v14
- update dependency @types/jest to v29.4.4
- update dependency @types/node to v18.15.3
- update dependency @types/react to v18.0.28
- update dependency @types/react-dom to v18.0.11
- update dependency at.favre.lib:bcrypt to v0.10.2
- update dependency axios to v1.3.4
- update dependency com.auth0:java-jwt to v4.3.0
- update dependency com.puppycrawl.tools:checkstyle to v10.8.1
- update dependency com.rometools:rome to v2.1.0
- update dependency eslint to v8.36.0
- Update dependency hls.js to v1.3.4
- update mantine monorepo packages to v5.10.5
- update dependency org.apache.maven.plugins:maven-assembly-plugin to v3.5.0
- update dependency org.apache.maven.plugins:maven-compiler-plugin to v3.11.0
- update dependency org.apache.maven.plugins:maven-surefire-plugin to v3.0.0
- update dependency org.jupnp:org.jupnp to v2.7.0
- update dependency org.jupnp:org.jupnp.support to v2.7.0
- update dependency react-router-dom to v6.9.0
- update dependency video.js to v7.21.4
- update dependency web-vitals to v3.3.0
- update logback-version to v1.4.6
- update typescript-eslint monorepo to v5.55.0

## [13.2.1](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/13.2.0...13.2.1) (2023-02-06)

### General:
- Fixed the Show the Media Library Folder, Audio Priority, Subtitles Priority, and Show the Live Subtitles Folder settings
- Fixed web player download permissions
- Fixed unnecessary transcoding
- Fixed MEncoder defer to tsMuxeR setting
- Fixed startup error on non-semver operating systems
- Fixed the appearance of broken settings in unauthenticated mode
- Allow directories to be unselected/cleared
- Fixed broken thumbnails in Docker
- Fixed exception when folders are populated on startup
- Fixed invalid GUI package name
- Removed unused plugins for faster first build time
- Improved documentation about developer workflow

### Translation updates via Crowdin
- English (United Kingdom) (65%) (thanks, Pete Russell!)
- German (100%) (thanks, pipin!)
- Russian (100%) (thanks, Олег Лойко!)
- Ukrainian (27%) (thanks, Roman Malkov!)

### Dependencies:
- Bump all subdependencies
- Bump eslint-plugin-react to 7.32.2
- Bump http-cache-semantics from 4.1.0 to 4.1.1
- Bump MediaInfo on Windows to 22.12
- Bump Node.js to 18.14.0
- Update dependency @types/jest to v29.4.0
- Update dependency @types/node to v18.11.19
- Update dependency @types/react to v18.0.27
- Update dependency @types/video.js to v7.3.51
- Update dependency axios to v1.3.2
- Update dependency com.fasterxml.jackson.core:jackson-databind to v2.14.2
- Update dependency com.puppycrawl.tools:checkstyle to v10.7.0
- Update dependency eslint to v8.33.0
- Update dependency org.apache.maven.plugins:maven-enforcer-plugin to v3.2.1
- Update dependency typescript to v4.9.5
- Update dependency react-router-dom to v6.8.0
- Update dependency video.js to v7.21.2
- Update Mantine monorepo packages to v5.10.3
- Update typescript-eslint monorepo to v5.50.0
- Update Yarn to v3.4.1

## [13.2.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/13.1.0...13.2.0) (2023-01-15)

### General:
- Improved motion compensation with 2D to 3D conversion, for full details see https://iwantaholodeck.com/algorithmic-tuning-motion-and-alignment/ (thanks, threedguru!)
- Added support for KeepAspectRatio settings in 2D-to-3D converted videos (thanks, threedguru!)
- Database scanning and cleanups are triggered from shared content updates
- Fixed login screen being shown more than once in web settings (#3751) (thanks for reporting, BitEater21 and Richardk2n!)
- Fixed broken database upgrade (#3756) (thanks for reporting, mykeehu!)
- Fixed sharing of network drives (#3750) (thanks for reporting, BitEater21 and OldMan100!)
- Fixed moving and marking as fully played setting in UK English
- Fixed error causing failed startup
- Fixed unshared content not being cleaned up after scan

### Translation updates via Crowdin
- English (United Kingdom) (15%)
- Japanese (83%) (thanks, Kazunori Hamada!)
- Russian (100%) (thanks, Олег Лойко!)
- Slovak (83%) (thanks, Dušan!)
- Turkish (100%) (thanks, Burak Yavuz!)

### Dependencies:
- Updated all subdependencies
- Update dependency com.auth0:java-jwt to v4.2.2
- Update dependency com.google.code.gson:gson to v2.10.1
- Update dependency eslint to v8.32.0
- Update dependency org.apache.maven.plugins:maven-checkstyle-plugin to v3.2.1
- Update dependency org.apache.maven.plugins:maven-pmd-plugin to v3.20.0
- Update dependency org.apache.maven.plugins:maven-project-info-reports-plugin to v3.4.2
- Update dependency react-router-dom to v6.6.2
- Update dependency web-vitals to v3.1.1
- Update jna-version to v5.13.0
- Update junit5 monorepo to v5.9.2
- Update Mantine monorepo packages to v5.10.0
- Update surefire-version to v3.0.0-M8
- Update typescript-eslint monorepo to v5.48.1

## [13.1.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/13.0.1...13.1.0) (2023-01-01)

### General:
- Added language support for API metadata
- Added Tagline, Rated, Year Started, and Total Seasons on web player
- Added button to TMDB and IMDb on web player
- Improved quality of 2D-to-3D conversion (thanks, threedguru!)
- Made H.265 transcoding over 2x faster
- Improved compatibility of H.264 transcoded stream via MEncoder
- Removed minimize Java GUI setting from web interface
- Fixed adding folders via old interface (#3726) (Thanks for reporting, infectormp!)
- Fixed web player server failure to start
- Fixed database metrics on close
- Fixed admin account when no auth is enabled
- Fixed ordering of TV series episodes in Media Library
- Fixed React player logos and posters
- Fixed some thumbnail bugs
- Stop using poster for Resume thumbnail
- Fixed changing language via browser
- Fixed not muxing H.264 via MEncoder when possible
- Fixed audio not playing in web player (#3130)
- Fixed web player use via proxy
- Fixed setting monitored and active states for shared content via old settings
- Fixed bugs with tsMuxeR handling H.265
- Fixed tsMuxeR deferral for certain files
- Fixed quickrun scripts not copying some files for development
- Made tests run faster on GitHub Actions
- General performance improvements

### Translation updates via Crowdin
- Chinese Traditional (77%) (thanks, Gene Wu!)
- Finnish (100%) (thanks, Esko Gardner!)
- French (97%)
- Italian (93%) (thanks, nonlosao!)
- Portuguese (Brazilian) (100%) (thanks, Mauro.A!)
- Russian (77%) (thanks, shecof!)
- Spanish (100%) (thanks, Yllelder!)

### Renderers:
- Added HDR to Supported lines in renderer configs
- Improved detection of SkyBox VR Player (thanks, threedguru!)
- Improved support for Mirascreen (thanks, Dušan Kazik!)
- Improved support for VLC for desktop and iOS (thanks, Kevin Abel!)
- Improved MP4 support on LG TVs and VLC
- Improved Dolby Vision support on LG TVs (thanks, narae0.kim from LG!)
- Fixed marking files as played when LG TVs are browsing
- Fixed Caliber support

### Dependencies:
- Update all Node.js subdependencies
- Update dependency @types/jest to v29.2.5
- Update dependency @types/node to v18.11.18
- Update dependency @types/react-dom to v18.0.10
- Update dependency axios to v1.2.2
- Update dependency com.puppycrawl.tools:checkstyle to v10.6.0
- Update dependency eslint to v8.31.0
- Update dependency mantine to 5.9.6
- Update dependency net.coobird:thumbnailator to v0.4.19
- Update dependency org.apache.httpcomponents:httpclient to v4.5.14
- Update dependency react-router-dom to v6.6.1
- Update FFmpeg to latest nightly
- Update MediaInfo to 22.12
- Update tsMuxeR to latest nightly
- Update typescript-eslint monorepo to v5.47.1
- Update Yarn to v3.3.1

## [13.0.1](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/13.0.0...13.0.1) (2022-12-17)

### General:
- Increased default max memory on Windows
- Fixed insertion of API metadata
- Fixed being unable to disable toggles in web settings (#3689)
- Fixed duplicate entries added to shared content on save when entries were removed
- Fixed shared content updates not saving (#3697)
- Fixed running via VS Code

### Translation updates via Crowdin
- Catalan (89%) (thanks, Toni Grau i Quellos!)
- Czech (100%)
- Korean (100%) (thanks, VenusGirl!)
- Polish (100%) (thanks, Karol Szastok!)
- Portuguese (Brazilian) (96%) (thanks, Matias Dos Reis!)
- Slovak (82%) (thanks, Filip Hanes!)
- Turkish (100%) (thanks, Burak Yavuz!)

### Dependencies:
- Updated all Node.js subdependencies
- Update dependency @types/node to v18.11.15
- Update dependency org.slf4j:slf4j-api to v2.0.6
- Update dependency react-router-dom to v6.5.0
- Update typescript-eslint monorepo to v5.46.1

## [13.0.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/12.0.1...13.0.0) (2022-12-11)

### General:
- Added 2D to 3D conversion for virtual reality, for full details see https://iwantaholodeck.com/on-the-fly-2d-to-3d-video-conversion-with-universal-media-server-ums/
- Fixed reload button is disabled after a restart
- Fixed sometimes mixed renderer found
- Implements LINN iOS app search request for attribute upnp:artist@role=composer
- Handle UPNP:AlbumArtist
- Escape iOS Smart Punctuation apostrophe
- Fixed case-insensitive UPnP search
- Double-click on tray icon opens web settings
- Made language more clear on the right-click tray menu
- Fixed API metadata countries, plot, ratings, rated (classification), start year, tagline, total seasons, and votes
- Fixed API poster/cover images sometimes not being set
- Fixed duplicate API fetches for failed results
- Fixed star rating updates not immediately updating
- Fixed resume files with AviSynth transcoding

### Translation updates via Crowdin
- Czech (94%)
- Danish (99%) (thanks, NCAA!)
- Finnish (99%) (thanks, Esko Gardner!)
- French (94%) (thanks, SurfaceS!)
- German (93%) (thanks, jaba82!)
- Korean (94%) (thanks, VenusGirl!)
- Portuguese (100%) (thanks, mariopinto!)
- Portuguese (Brazilian) (94%) (thanks, Sandro Almeida!)
- Spanish (88%) (thanks, edwardalvarez2011!)
- Turkish (100%) (thanks, Burak Yavuz!)
- Ukrainian (26%) (thanks, Paul Furlet!)

### Renderers:
- Let LG TVs use their built-in resume only
- Fixed support for DTS on LG OLED models newer than 2019
- Avoid re-encoding x265 needlessly on LG OLEDs
- Detect more versions of VLC

### Dependencies:
- Updated all Node.js subdependencies
- Update dependency @types/jest to v29.2.4
- Update dependency @types/lodash to v4.14.191
- Update dependency @types/node to v18.11.13
- Update dependency @types/react to v18.0.26
- Update dependency @types/video.js to v7.3.50
- Update dependency axios to v1.2.1
- Update dependency com.github.oshi:oshi-core to v6.4.0
- Update dependency com.puppycrawl.tools:checkstyle to v10.5.0
- Update dependency eslint to v8.29.0
- Update dependency hls.js to v1.2.9
- Update dependency react-router-dom to v6.4.5
- Update dependency tabler-icons-react to v1.56.0
- Update dependency typescript to v4.9.4
- Update Mantine monorepo packages to v5.8.4
- Update typescript-eslint monorepo to v5.46.0

## [12.0.1](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/12.0.0...12.0.1) (2022-11-27)

### General:
- Fixed freeze on startup
- Fixed broken translations on startup language selection
- Fixed can't add folders via Java GUI in v12 on macOS
- Fixed not detecting Playlist additions/changes during scans
- Fixed database update/drop
- Fixed macOS repeatedly prompting for permissions
- Fixed react player logout error prevent to browse to login

### Translation updates via Crowdin
- Slovak (81%) (thanks, Dušan!)
- Spanish (92%) (thanks, Pablo Camacho!)

### Dependencies
- Update dependency @types/jest to v29.2.3
- Update dependency @types/lodash to v4.14.190
- Update dependency @types/react-dom to v18.0.9
- Update dependency axios to v1.2.0
- Update dependency com.fasterxml.jackson.core:jackson-databind to v2.14.1
- Update dependency com.github.oshi:oshi-core to v6.3.2
- Update dependency eslint to v8.28.0
- Update logback-version to v1.4.5
- Update Mantine monorepo packages to v5.8.3
- Update dependency org.slf4j:slf4j-api to v2.0.5
- Update dependency web-vitals to v3.1.0
- Update twelvemonkeys-imageio-version to v3.9.4
- Update dependency typescript to v4.9.3
- Update typescript-eslint monorepo to v5.44.0
- Update dependency video.js to v7.21.1
- Update dependency videojs-contrib-quality-levels to v2.2.1

## [12.0.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/11.6.0...12.0.0) (2022-11-15)

### General:
- Added a new web settings interface, which is now the supported way to add content and change settings
- Added a new web player interface, for a faster, more responsive and accessible web player
- Added shutdown computer option to the Server Settings folder
- Added user auth to web interface
- Added user groups to web interface
- Unified the Shared Content area in both Java and web interfaces
- Local database speed improvements
- Improved UPnP/DLNA support
- Consolidated configuration files, with automatic migration to the new format
- Fixed TV series never being found locally by IMDb ID
- Fixed duplicate API requests
- Fixed profile support on Docker
- Hundreds of bugfixes and performance tweaks

### Renderers:
- Support Sony Network Speakers (thanks, scanf!)

### Translation updates via Crowdin
- Afrikaans (14%) (thanks, Eugene Trumpelmann!)
- Bulgarian (83%) (thanks, ruraru!)
- Catalan (88%) (thanks, Toni Grau i Quellos!)
- Czech (100%)
- Danish (99%) (thanks, GurliGebis and NCAA!)
- German (93%) (thanks, pipin!)
- Finnish (99%) (thanks, Esko Gardner!)
- Italian (96%) (thanks, Oscar Zambotti!)
- Korean (100%) (thanks, VenusGirl!)
- Polish (100%) (thanks, Karol Szastok!)
- Portuguese (99%) (thanks, mariopinto!)
- Portuguese (Brazilian) (99%) (thanks, Mauro.A!)
- Russian (82%) (thanks, Максим Мухачев!)
- Turkish (100%) (thanks, Burak Yavuz!)

### Dependencies
- Bump Java to 17.0.5
- Bump logback-version from 1.4.3 to 1.4.4
- Update Apache Commons Collections to 4.4
- Update com.sun.xml.messaging.saaj to 3.0.0
- Update dependency com.fasterxml.jackson.core:jackson-databind to v2.13.4.2
- Update dependency com.github.junrar:junrar to v7.5.4
- Update dependency com.github.oshi:oshi-core to v6.3.1
- Update dependency com.github.spotbugs:spotbugs-maven-plugin to v4.7.3.0
- Update dependency com.google.code.gson:gson to v2.10
- Update dependency com.ibm.icu:icu4j to v72
- Update dependency com.puppycrawl.tools:checkstyle to v10.4
- Update dependency net.coobird:thumbnailator to v0.4.18
- Update dependency pako to v2.1.0
- Update MediaInfo to 22.09
- Update twelvemonkeys-imageio-version to v3.9.3
- Removed assertj-core

## [11.6.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/11.5.0...11.6.0) (2022-10-05)

### General:
- Added support for reparsing modified files
- Improved filename prettifying/matching
- Improved performance
- Fixed failed parsing of files being moved/copied
- Fixed restart program reliability
- Fixed security hole
- Stop extracting metadata or doing API lookups for Live Photos in iPhone backups on macOS
- Updated all tests to junit 5

### Translation updates via Crowdin
- Spanish (97%) (thanks, Sergio Varela!)

### Dependencies
- Bump commons-text from 1.9 to 1.10.0
- Bump junit5.version from 5.9.0 to 5.9.1
- Bump logback-version from 1.4.1 to 1.4.3
- Update com.sun.xml.bind-version to v4.0.1
- Update dependency com.puppycrawl.tools:checkstyle to v10.3.4
- Update dependency org.slf4j:slf4j-api to v2.0.3

## [11.5.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/11.4.1...11.5.0) (2022-09-17)

### General:
- Improved video quality on Wi-Fi networks
- Persist max memory setting in Windows installer
- Fixed web player status communication
- Fixed resource leaks

### Renderers:
- Added support for Caliber radio devices (thanks, Bart Jourquin!)
- Allow seeking while transcoding on Roku devices
- Fixed sound cutting out on Panasonic TVs when transcoding

### Translation updates via Crowdin
- German (100%) (thanks, pipin!)
- Japanese (100%) (thanks, KEINOS!)
- Persian (100%) (thanks, Sadra Imam!)
- Spanish (97%) (thanks, Luis Alberto García Díaz!)
- Ukrainian (31%) (thanks, Alexandr Opara!)

### Dependencies
- Updated checkstyle to v10.3.3
- Bump FFmpeg to latest
- Bump jackson-databind from 2.13.3 to 2.13.4
- Bump logback-version from 1.2.11 to 1.4.1
- Bump maven-pmd-plugin from 3.18.0 to 3.19.0
- Bump slf4j-api from 1.7.36 to 2.0.1
- Bump spotbugs-maven-plugin from 4.7.1.1 to 4.7.2.0

### [11.4.1](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/11.4.0...11.4.1) (2022-08-26)

### General:
- Improved support for split episodes (e.g. S01E02a, S01E02.5)
- Improved anime episode matching
- Fixed dc:date not sending to renderers for videos (#3215)
- Fixed shutdown consistency
- Performance and stability improvements
- Improved local build speed during development
- Improved GitHub Actions build speed

### Translation updates via Crowdin
- French (100%) (thanks, Archaos!)
- Persian (84%) (thanks, Sadra Imam!)
- Ukrainian (30%) (thanks, Василь «CVI» Чефранов!)

### Dependencies
- Updated checkstyle to v10.3.2
- Updated gson to v2.9.1
- Updated JRE to 17.0.4.1`
- Updated junrar to v7.5.3
- Updated maven-checkstyle-plugin to 3.2.0
- Updated maven-javadoc-plugin to v3.4.1
- Updated maven-pmd-plugin from 3.17.0 to 3.18.0
- Updated maven-project-info-reports-plugin to v3.4.1
- Updated maven-site-plugin to v3.12.1
- Updated twelvemonkeys-imageio-version to v3.8.3

## [11.4.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/11.3.0...11.4.0) (2022-07-29)

### General:
- Enabled UPnP searching, with the ability to disable for problematic renderers
- Improved performance when scanning and browsing by up to 3,563%
- Improved performance when handling API metadata
- Improved performance with many audio files
- Fixed broken translations
- Fixed some API metadata handling bugs
- Fixed macOS startup crash
- Fixed running as a service on x64 Windows

### Translation updates via Crowdin
- Catalan (99%) (thanks, Toni Grau i Quellos!)
- Czech (100%)
- Danish (100%) (thanks, NCAA!)
- Finnish (100%) (thanks, Esko Gardner!)
- German (99%) (thanks, pipin!)
- Italian (100%) (thanks, Oscar Zambotti!)
- Korean (100%) (thanks, VenusGirl!)
- Spanish (96%) (thanks, edwardalvarez2011!)
- Persian (84%) (thanks, Sadra Imam!)
- Polish (100%) (thanks, Karol Szastok!)
- Portuguese (100%) (thanks, mariopinto!)
- Portuguese (Brazilian) (100%) (thanks, Mauro.A!)
- Turkish (100%) (thanks, Burak Yavuz!)

### Dependencies
- Updated JRE to 17.0.4
- Updated junit5 to 5.9.0
- Updated maven-assembly-plugin to 3.4.2
- Updated maven-project-info-reports-plugin 3.4.0
- Updated maven-resources-plugin to 3.3.0
- Updated oshi-core to 6.2.2
- Updated spotbugs-maven-plugin to 4.7.1.1

## [11.3.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/11.2.0...11.3.0) (2022-07-17)

### General
- Sign and notarize macOS releases, for easier installation
- Adding/removing shared folders updates instantly
- Fixed autoupdater on Apple ARM build
- Renamed all translation keys for more readable code
- Fixed broken sleep prevention during playback on macOS
- Fixed macOS version compatibility warning in log viewer
- Fixed web browser video/audio playback on some systems and browsers

### Renderers
- Send date metadata for audio on nextcp/2

### Translation updates via Crowdin
- Bulgarian (93%) (thanks, Иво Иванов and Dremski!)
- Catalan (98%) (thanks, Toni Grau i Quellos!)
- Czech (100%)
- Danish (100%) (thanks, GurliGebis and NCAA!)
- Dutch (91%) (thanks, DJ_eMPe!)
- English (United Kingdom) (17%) (thanks, DJ_eMPe!)
- Finnish (100%) (thanks, Esko Gardner!)
- French (100%) (thanks, Archaos!)
- German (99%) (thanks, pipin!)
- Hebrew (23%) (thanks, shayh!)
- Hungarian (91%) (thanks, promi!)
- Italian (100%) (thanks, Oscar Zambotti!)
- Korean (100%) (thanks, VenusGirl!)
- Polish (100%) (thanks, Karol Szastok!)
- Portuguese (100%) (thanks, mariopinto and RPargana!)
- Portuguese (Brazilian) (100%) (thanks, Mauro.A!)
- Serbian (Cyrillic) (92%) (thanks, silevb!)
- Spanish (96%) (thanks, edwardalvarez2011!)
- Swedish (99%) (thanks, Erik Karlsson and Lorien aka the First One!)
- Turkish (100%) (thanks, Burak Yavuz!)
- Ukrainian (29%) (thanks, Paul Furlet!)

### Dependencies
- Updated appbundler to 5946207
- Updated exec-maven-plugin to v3.1.0
- Updated maven-assembly-plugin to v3.4.1
- Updated moment.js to v2.29.4
- Updated spotbugs-maven-plugin to v4.7.1.0

## [11.2.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/11.1.1.1...11.2.0) (2022-07-03)

### General
- Added a release for Apple ARM (Apple Silicon, M1/M2) processors, for 3x faster transcoding
- Added renderer option SendDateMetadataYearForAudioTags
- Added netbeans git exclusions for developers
- Media Library Movies folders ignore files with durations under 40 minutes
- Fixed broken browsing on some renderers
- Fixed audio playback on web interface
- Cleanup old transcoding engines
- Removed dead code and language terms

### Dependencies
- Update assertj to v3.23.1
- Update checkstyle to v10.3.1
- Update h2database to v2.1.214
- Update jna to v5.12.1
- Update JRE to 17.0.3.1
- Update maven-assembly-plugin to v3.4.0
- Update oshi to v6.2.1
- Update pako to v2

#### [11.1.1.1 - since 11.1.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/11.1.0...11.1.1.1) (2022-06-15)

### General
- Fixed failed database update for some users (#3051) (thanks, vrtlspd!)
- Fixed Docker startup crash (#3050) (thanks, vrtlspd!)
- Document HLS-MPEGTS-H264-AAC transcoding option

### Translations via Crowdin
- Added Estonian (23%) (thanks, Junk Knuj and Tanel K!)

### Dependencies
- Update dependency maven-enforcer-plugin to v3.1.0


## [11.1.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/11.0.0...11.1.0) (2022-06-10)
### General
- Improved caching of web interface files
- Added getServerPlaylists to playlist API
- Fixed broken thumbnail-url for playlist folder resolved by DbIdResourceLocator
- Added support for more language characters on web interface
- Updated and removed old links on About tab
- Fixed database fields truncating to less than the limit
- Fixed rare and unpredictable bugs

### Translation updates via Crowdin
- Catalan (99%) (thanks, Toni Grau i Quellos!)
- French (100%) (thanks, Archaos!)
- Polish (100%) (thanks, Karol Szastok!)

### Dependencies
- Update dependency com.puppycrawl.tools:checkstyle to v10.3
- Update dependency org.apache.maven.plugins:maven-pmd-plugin to v3.17.0
- Update surefire-version to v3.0.0-M7
- Bump com.sun.xml.bind-version from 3.0.2 to 4.0.0
- Bump junrar from 7.5.1 to 7.5.2

## [11.0.0 - since 10.21.1](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/10.21.1...11.0.0) (2022-05-27)
### General
- Uses image backgrounds and logos for movies, TV series and episodes on the web interface
- Web interface switches between dark and light themes in Media Library based on background images
- Automatic quality adjustment on web interface, with optional manual settings
- Improved video forward-backward positioning in videos via the web video player
- Improved web interface design
- Improved web video player design
- Improved API metadata matches for TV series
- Added HLS video option which may improve support for transcoding via some renderers like Philips TVs
- Log splitting/zipping (#2608)
- Implemented network state scanner
- Show the addresses of servers on the Status tab (#2372)
- Allow to set the log level of FFmpeg in its engine settings in the desktop UI (#2677)
- Increased maximum memory limit on macOS to match Windows
- Fixed Restart server when network interface becomes available (#2615)
- Fixed Network Interface not found (#1485)
- Fixed GetProtocolInfo not being recieved (#2338)
- Fixed Font scaling issues on some elements of the GUI for some users (#2549)
- Fixed UMS doesn't free DLNA ports after closing (#739)
- Fixed Service wrapper for Windows doesn't work with 64 bit JVM (#767)
- Fixed Deinstallation does not remove autostart entry for wrapper.exe (#2343)
- Fixed Attempt to remove medias.lock on install/startup (#2838)
- Fixed Media Library no longer populated and startup scan slowness (#2826)
- Fixed Cound not open the default web browser: The BROWSE action is not supported on the current platform! (#2725)
- Added editable server-side playlist support
- Prevent users from enabling startup scanning while the cache is disabled
- Removed second toolbar on the web interface video page
- Increased speed of API lookups
- Improved video and TV series posters
- Fixed some API metadata not updating
- Fixed Resume videos on the web interface
- Fixed API response handling
- Backgrounds for TV series, episodes and movies fade in
- Image backgrounds in Media Library without text are prioritized over ones with text
- Match more TV series in API
- Use markdown in changelog
- Use main branch instead of master
- Fixed audio files detected as video files

### Renderers
- Samsung 2021 TVs use HLS transcoding

### Translation updates via Crowdin
- Catalan (100%) (thanks, Toni Grau i Quellos!)
- Chinese (Simplified) (97%) (thanks, wwj402_github!)
- Czech (100%)
- Danish (99%) (thanks, GurliGebis and NCAA!)
- English (UK) (99%) (thanks, Pete Russell!)
- Finnish (100%) (thanks, Esko Gardner!)
- French (100%) (thanks, Archaos and Philippe P!)
- Italian (100%) (thanks, Oscar Zambotti!)
- Korean (100%) (thanks, VenusGirl!)
- Persian (34%) (thanks, Behzad Najafizad!)
- Polish (100%) (thanks, Karol Szastok!)
- Portuguese (100%) (thanks, mariopinto!)
- Portuguese (Brazilian) (100%) (thanks, Mauro.A!)
- Swedish (99%) (thanks, Lorien aka the First One (The1stOne)!)
- Turkish (100%) (thanks, Burak Yavuz!)

### Dependencies
- Started using Renovate for dependency tracking
- Added x64 windows service wrapper
- Replaced Cling with JUPnP
- Update actions/cache action to v3
- Update actions/checkout action to v3
- Update dependency com.github.spotbugs:spotbugs-maven-plugin to v4.7.0.0
- Update dependency moment.js to v2.29.3
- Update dependency pako to v1.0.11
- Bump checkstyle from 9.3 to 10.2
- Bump Jackson from 2.13.1 to 2.13.2.2
- Bump JRE from 8u332 to 17.0.3
- Bump metadata-extractor from 2.17.0 to 2.18.0
- Bump Video.js from 7.13.3 to 7.19.2
- Fixed support for latest Maven versions

## [11.0.0 - since 11.0.0-a2](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/11.0.0-a2...11.0.0) (2022-05-27)
### General
- Added editable server-side playlist support
- Prevent users from enabling startup scanning while the cache is disabled
- Removed second toolbar on the web interface video page
- Increased speed of API lookups
- Improved video and TV series posters
- Fixed some API metadata not updating
- Fixed Resume videos on the web interface
- Fixed API response handling
- Use markdown in changelog
- Use main branch instead of master
- Fixed audio files detected as video files

### Translation updates via Crowdin
- Catalan (100%) (thanks, Toni Grau i Quellos!)
- Chinese (Simplified) (97%) (thanks, wwj402_github!)
- Czech (100%)
- Danish (99%) (thanks, GurliGebis and NCAA!)
- English (UK) (99%) (thanks, Pete Russell!)
- Finnish (100%) (thanks, Esko Gardner!)
- French (100%) (thanks, Archaos and Philippe P!)
- Italian (100%) (thanks, Oscar Zambotti!)
- Korean (100%) (thanks, VenusGirl!)
- Persian (34%) (thanks, Behzad Najafizad!)
- Polish (100%) (thanks, Karol Szastok!)
- Portuguese (100%) (thanks, mariopinto!)
- Portuguese (Brazilian) (100%) (thanks, Mauro.A!)
- Swedish (99%) (thanks, Lorien aka the First One (The1stOne)!)
- Turkish (100%) (thanks, Burak Yavuz!)

### Dependencies
- Started using Renovate for dependency tracking
- Update actions/cache action to v3
- Update actions/checkout action to v3
- Update dependency com.fasterxml.jackson.core:jackson-databind to v2.13.3
- Update dependency com.github.spotbugs:spotbugs-maven-plugin to v4.7.0.0
- Update dependency moment.js to v2.29.3
- Update dependency org.jupnp:org.jupnp to v2.6.1
- Update dependency org.jupnp:org.jupnp.support to v2.6.1
- Update dependency pako to v1.0.11
- Bump Jackson from 2.13.1 to 2.13.2.2
- Bump metadata-extractor from 2.17.0 to 2.18.0

### [11.0.0-a2](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/11.0.0-a1...11.0.0-a2) (2022-05-05)
### General
- Backgrounds for TV series, episodes and movies fade in
- Image backgrounds in Media Library without text are prioritized over ones with text
- Match more TV series in API
- All v10 changes up to 10.21.1

### [10.21.1](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/10.21.0.1...10.21.1) (2022-05-04)
### General
- Improved parsing of miniseries
- Fixed crash for users with cache disabled
- Fixed macOS build not installing for some users
- Reduced macOS build size

### Translation updates via Crowdin
- English (United Kingdom) (45%) (thanks, Sudeep James!)
- Slovenian (55%) (thanks, Blaž Kozlevčar!)
- Turkish (100%) (thanks, Burak Yavuz!)

### Dependencies
- Bump junrar from 7.5.0 to 7.5.1
- Bump maven-project-info-reports-plugin from 3.2.2 to 3.3.0

#### [10.21.0.1](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/10.21.0...10.21.0.1) (2022-04-26)

### General
- Fixed duplicate TV series entries in Media Library

### [11.0.0-a1](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/10.21.0...11.0.0-a1) (2022-04-25)

### General
- Uses image backgrounds and logos for movies, TV series and episodes on the web interface
- Web interface switches between dark and light themes in Media Library based on background images
- Automatic quality adjustment on web interface, with optional manual settings
- Improved video forward-backward positioning in videos via the web video player
- Improved web interface design
- Improved web video player design
- Improved API metadata matches for TV series
- Added HLS video option which may improve support for transcoding via some renderers like Philips TVs
- Log splitting/zipping (#2608)
- Implemented network state scanner
- Show the addresses of servers on the Status tab (#2372)
- Allow to set the log level of FFmpeg in its engine settings in the desktop UI (#2677)
- Increased maximum memory limit on macOS to match Windows
- Fixed Restart server when network interface becomes available (#2615)
- Fixed Network Interface not found (#1485)
- Fixed GetProtocolInfo not being recieved (#2338)
- Fixed Font scaling issues on some elements of the GUI for some users (#2549)
- Fixed UMS doesn't free DLNA ports after closing (#739)
- Fixed Service wrapper for Windows doesn't work with 64 bit JVM (#767)
- Fixed Deinstallation does not remove autostart entry for wrapper.exe (#2343)
- Fixed Attempt to remove medias.lock on install/startup (#2838)
- Fixed Media Library no longer populated and startup scan slowness (#2826)
- Fixed Cound not open the default web browser: The BROWSE action is not supported on the current platform! (#2725)
### Renderers
- Samsung 2021 TVs use HLS transcoding
### Dependencies
- Added x64 windows service wrapper
- Fixed support for latest Maven versions
- Replaced Cling with JUPnP
- Bump checkstyle from 9.3 to 10.2
- Bump JRE from 8u332 to 17.0.3
- Bump Video.js from 7.13.3 to 7.19.2

## [10.21.0](https://github.com/UniversalMediaServer/UniversalMediaServer/compare/10.20.0...10.21.0) (2022-04-24)

### General
- Improved API matches for TV series
- Language improvements (thanks, NCAA!)
- Fixed database not recovering from serialization changes (#2874)
- Fixed custom renderer configurations not being loaded (#2917)
- Fixed MEncoder and MPlayer on Docker (thanks, Jille Timmermans!) (#2922)
- Fixed config being overwritten when UMS closes (#2875)
- Fixed setting directory as fully played when folder names are similar
- Fixed peakaboo in status bar during scans
- Fixed database upgrade bugs
### Translation updates via Crowdin
- German (100%) (thanks, pipin!)
- Portuguese (100%) (thanks, RPargana!)
### Dependencies
- Bump FFmpeg to latest
- Bump h2 from 2.1.210 to 2.1.212
- Bump icu4j from 70.1 to 71.1
- Bump JRE from 8u322 to 8u332
- Bump maven-antrun-plugin from 3.0.0 to 3.1.0
- Bump maven-javadoc-plugin from 3.3.2 to 3.4.0
- Bump maven-site-plugin from 3.11.0 to 3.12.0
- Bump MediaInfo from 21.09 to 22.03
- Bump oshi-core from 6.1.5 to 6.1.6
- Bump surefire-version from 3.0.0-M5 to 3.0.0-M6

10.20.0 - 2022-03-30

	General:
		Added ability to update audio ID3 tags via MusicBrainz, disabled by default
		Added ability to rate/like music via API
		Added "My Albums" folder to the Media Library
		Added support for UPnP sortOrder requests when using UPnP searching
		Added dc:date UPnP attribute for renderers to optionally use
		Allow API strings to be translated
		Updated default podcasts and YouTube channels
		Fixed slow browsing of the root and web folders
		Improved responsiveness during startup scans
	Translation updates via Crowdin:
		Catalan (100%) (thanks, Toni Grau i Quellos!)
		Chinese (Traditional) (95%) (thanks, Gene Wu!)
		Danish (100%) (thanks, NCAA!)
		Finnish (100%) (thanks, Esko Gardner!)
		French (100%) (thanks, Archaos!)
		Italian (100%) (thanks, Oscar Zambotti!)
		Japanese (99%) (thanks, was0914!)
		Korean (100%) (thanks, VenusGirl!)
		Polish (100%) (thanks, Karol Szastok!)
		Portuguese (100%) (thanks, mariopinto!)
		Portuguese (Brazilian) (100%) (thanks, Mauro.A!)
		Spanish (100%) (thanks, Sergio Varela!)
		Swedish (99%) (thanks, Lorien aka the First One!)
		Turkish (100%) (thanks, Burak Yavuz!)
	Dependencies:
		Bump jna from 5.10.0 to 5.11.0
		Bump junrar from 7.4.1 to 7.5.0
		Bump metadata-extractor from 2.16.0 to 2.17.0
		Bump oshi-core from 6.1.4 to 6.1.5
		Bump spotbugs-maven-plugin from 4.5.3.0 to 4.6.0.0

10.19.0 - 2022-03-14

	General:
		Improved speed of video metadata lookups
		Improved speed and drive use when browsing/scanning folders
		Match more years in filenames for more accurate video metadata lookups
		Fixed junk data at the end of transcoded videos (#2867)
		Fixed the failed lookups table upgrade/creation (#2871)
		Fixed TV series data versioning
		Fixed redundant TV series lookups
		Fixed response caching
		Fixed failed video metadata caching
		Fixed hanging API requests
	Dependencies:
		Bump logback-version from 1.2.10 to 1.2.11

10.18.0 - 2022-03-06

	General:
		Fixed video metadata not writing to database
		Fixed shared folder added via wizard not being saved
		Fixed overwriting WEB.conf with defaults if UMS.conf is missing
		Fixed initial shared folder not being monitored
		Improved performance of database writes
	Renderers:
		Added recognition of more LG Blu-ray players (thanks, OlliL!)
	Translation updates via Crowdin:
		Icelandic (18%) (thanks, Diðrik Valur Diðriksson!)
	Dependencies:
		Bump oshi-core from 6.1.3 to 6.1.4

10.17.1 - 2022-02-27

	General:
		Fixed not removing Windows service on uninstall (#2343)
		Fixed broken database upgrade (#2838, #2826)
	Renderers:
		Improved support for Samsung 2021 TVs
		Speed up video playback on Kodi (thanks, Dušan Kazik!)
	Translation updates via Crowdin:
		Ukrainian (30%) (thanks, Roman Malkov and rnglad!)
	Dependencies:
		Bump gson from 2.8.9 to 2.9.0
		Bump maven-compiler-plugin from 3.9.0 to 3.10.0
		Bump maven-javadoc-plugin from 3.3.1 to 3.3.2
		Bump maven-pmd-plugin from 3.15.0 to 3.16.0
		Bump maven-project-info-reports-plugin from 3.2.1 to 3.2.2
		Bump maven-site-plugin from 3.10.0 to 3.11.0
		Bump oshi-core from 5.8.6 to 6.1.3
		Bump slf4j from 1.7.21 to 1.7.36
		Bump thumbnailator from 0.4.16 to 0.4.17
		Bump twelvemonkeys-imageio-version from 3.8.1 to 3.8.2

10.17.0 - 2022-02-12

	General:
		Added a new, experimental HTTP server (disabled by default)
		Fixed occasional problems with playing TV stations via Tvheadend, and other web content (thanks, Premik!)
		Web interface uses SSE instead of polling for greater efficiency
		Added support for UPnP searching by MusicBrainz albums
		Fixed broken UPnP searching
		Performance improvements
		Minor bug fixes
		Significant refactoring
	Renderers:
		Improved support for Samsung 5300 Series
	Translation updates via Crowdin:
		Japanese (99%) (thanks, yanote!)
		Swedish (99%) (thanks, Andy Catalui!)
		Ukrainian (23%) (thanks, Pavlo Kuznietsov!)
	Dependencies:
		Bump checkstyle from 9.2.1 to 9.3
		Bump h2 from 2.0.206 to 2.1.210
		Bump jaudiotagger from 2.2.5 to 3.0.1
		Bump JRE from 8u312 to 8u322
		Bump junrar from 7.4.0 to 7.4.1
		Bump maven-project-info-reports-plugin from 3.1.2 to 3.2.1

10.16.0 - 2022-01-15

	General:
		Improved detection of TV episodes and movies
		Improved cache performance and resource use
		Made shutdown more reliable
		Made Resume files appear after the original file entry instead of before
		Fixed filtering folder names (#2697)
		Fixed frozen scanning when files can't be parsed (#1879)
		Tweaked audio track sorting heuristics 
		Added configuration flag sort_audio_tracks_by_album_position
		Significant code improvements (thanks, SurfaceS!)
	Translation updates via Crowdin:
		Catalan (100%) (thanks, Toni Grau i Quellos!)
		Spanish (100%) (thanks, Carlos Suárez!)
		Ukrainian (21%) (thanks, Mykola Petrivs'kyi!)
	Dependencies:
		Bump assertj-core from 3.21.0 to 3.22.0
		Bump h2 from 2.0.204 to 2.0.206
		Bump maven-compiler-plugin from 3.8.1 to 3.9.0
		Bump spotbugs-maven-plugin from 4.5.2.0 to 4.5.3.0
		Bump thumbnailator from 0.4.15 to 0.4.16
		Bump tsMuxeR from 2021-11-14 to 2022-01-13

10.15.0 - 2022-01-03

	General:
		Vast speed improvements
		Added ability to overlay text under the renderer icon (thanks, SurfaceS!)
		Web interface initially loads 3 seconds faster
		Fixed network speed detection on Docker
		Improved communicating changes with devices (like fully played status)
		Fixed broken file playback, especially with large collections
		Fixed undefined "Rated" field with some videos on web interface
		Fixed media not marked as fully played if the file action failed
		Fixed resume files not being created on the web interface
		Fixed files marked as stopped when they are still playing on the web interface (#2766)
		Fixed "not playable" message on web interface (#2258)
	Renderers:
		Added detection of more Roku TVs
		Added detection of Samsung Soundbars
		Added detection of Sony X Series TVs
		Added detection of TCL TVs
	Translation updates via Crowdin:
		German (100%) (thanks, pipin!)
		Italian (100%) (thanks, Oscar Zambotti!)
	Dependencies:
		Bump checkstyle from 9.2 to 9.2.1
		Bump h2database from 1.4.197 to 2.0.204 (thanks, SurfaceS!)
		Bump icu4j from 69.1 to 70.1
		Bump logback-version from 1.2.9 to 1.2.10
		Bump maven-site-plugin from 3.9.1 to 3.10.0
		Bump rome from 1.16.0 to 1.18.0
		Bump twelvemonkeys-imageio-version from 3.8.0 to 3.8.1

10.14.1 - 2021-12-24

	General:
		Fixed broken logging characters with some languages (thanks, SurfaceS!)
	Translation updates via Crowdin:
		Danish (100%) (thanks, NCAA!)
		Finnish (100%) (thanks, Esko Gardner!)
		French (100%) (thanks, Archaos!)
		Korean (100%) (thanks, VenusGirl!)
		Polish (100%) (thanks, Karol Szastok!)
		Portuguese (100%) (thanks, mariopinto!)
		Portuguese (Brazilian) (100%) (thanks, Mauro.A!)
		Slovak (93%) (thanks, Lukáš Praznovec!)
		Swedish (97%) (thanks, Lorien aka the First One)
		Turkish (100%) (thanks, Burak Yavuz!)
	Dependencies:
		Bump logback from 1.2.8 to 1.2.9
		Bump spotbugs-maven-plugin from 4.5.0.0 to 4.5.2.0

10.14.0 - 2021-12-19

	General:
		Added the Contributor Covenant Code of Conduct to reflect and inspire our ideals and practices (thanks, Shubham Rauthan!)
		Added "Browse by album" feature to UPnP person/artist searches (thanks, ik666!)
		Added a debug configuration for VS Code (thanks, A2va!)
		Allow users to have no shared content (both remote or local), instead of setting defaults (thanks, Andy Griffiths!)
		Fixed tsMuxeR not working on macOS, again (thanks, DetectiveKenobi!)
	Renderers:
		Fixed support for H.264 level 5.x video on Panasonic VT60 TVs
	Translation updates via Crowdin:
		Bulgarian (96%) (thanks, Radoslav Ignatov!)
		Hungarian (96%) (thanks, Poliziotto!)
		Persian (34%) (thanks, Aydin Noori!)
	Dependencies:
		Bump checkstyle from 9.1 to 9.2
		Bump doxia-module-docbook-simple from 1.10 to 1.11.1
		Bump FFmpeg from 2021-07-22 to 2021-12-16
		Bump JNA from 5.9.0 to 5.10.0
		Bump junit5 from 5.8.1 to 5.8.2
		Bump logback from 1.2.7 to 1.2.8
		Bump oshi-core from 5.8.5 to 5.8.6
		Bump spotbugs-maven-plugin from 4.4.2.2 to 4.5.0.0
		Bump thumbnailator from 0.4.14 to 0.4.15
		Bump tsMuxeR from 2021-09-02-01-59-46 to 2021-11-14-02-03-07
		Bump twelvemonkeys-imageio-version from 3.7.0 to 3.8.0
		Bump youtube-dl from 2021.06.06 to 2021.12.17

10.13.0 - 2021-11-16

	General:
		Improved matching and prettifying of TV episodes, especially anime and TV episodes
		Allow successful API matches to be updated via versioning, for more accurate results
		If a folder contains music tracks, music tracks will be sorted by track number (thanks, ik666!)
		Re-implemented UPnP searching (thanks, ik666!)
		Compact database on shutdown (thanks, ik666!)
		Fixed status line glitches while scanning
		Improved default network interface selection
		Fixed playlist folders not updating (thanks, ik666!)
		Fixed sorting when API result starts with a different letter than filename
		Parse files that have the short naming convention, where the parent directory is the real filename
		Fixed parsing of TV episodes in season 0
		Fixed crash when bumping database version
		Fixed memory leak after failed API lookup
		Fixed hanging process due to filewatcher not closing before shutdown
		Various web interface code improvements (thanks, Amir, Florian, and Kurtis Hanson)
		Code readability improvements (thanks, Christian Baer!)
		Protect against uncaught exceptions
		Fixed memory leaks
		Fixed database thread-safety bugs
		Increased performance of some database queries
		Increased performance of filename prettifying
		Fixed sending API requests when external network is disabled
		Fixed process hanging if filesystem activity happens in a watched folder during shutdown
	Renderers:
		Improved matching and transcoding on Samsung 4K+ TVs
	Translation updates via Crowdin:
		Bengali (1%) (thanks, Bipul Dey!)
		Chinese (Simplified) (100%) (thanks, wwj402_github!)
		Chinese (Traditional) (96%) (thanks, Gene Wu!)
		Czech (100%)
		Danish (100%) (thanks, GurliGebis!)
		Hungarian (96%) (thanks, Hild György!)
		Japanese (100%) (thanks, Katsuhide.M!)
		Romanian (100%) (thanks, Bogdan!)
	Dependencies:
		Bump assertj-core from 3.20.2 to 3.21.0
		Bump checkstyle from 9.0 to 9.1
		Bump gson from 2.8.8 to 2.8.9
		Bump junit5.version from 5.8.0 to 5.8.1
		Bump JRE from 8u302 to 8u312
		Bump logback from 1.2.5 to 1.2.7
		Bump maven-enforcer-plugin from 3.0.0-M3 to 3.0.0
		Bump MediaInfo from 21.03 to 21.09
		Bump oshi-core from 5.8.2 to 5.8.3
		Bump spotbugs-maven-plugin from 4.3.0 to 4.4.2.2

10.12.0 - 2021-09-17

	General:
		Increased performance and reduced memory use of startup scan
		Renderer configurations now have MediaInfo enabled by default
		Fixed files locking during or just after download
		Fixed process hanging on shutdown
		Made building the project faster
		Started using DeepSource for static code analysis
	Translation updates via Crowdin:
		French (100%) (thanks, Archaos!)
		German (100%) (thanks, pipin!)
		Italian (100%) (thanks, Marco F. V. Baljet!)
		Portuguese (Brazilian) (thanks, Mauro.A!)
		Russian (96%) (thanks, Andrey Zhernovoy!)
	Dependencies:
		Revert h2database from 1.4.199 to 1.4.197
		Bump JRE to 8u302+8 on macOS
		Bump junit5.version from 5.7.2 to 5.8.0
		Bump maven-javadoc-plugin from 3.3.0 to 3.3.1
		Bump maven-pmd-plugin from 3.14.0 to 3.15.0
		Bump oshi-core from 5.8.1 to 5.8.2
		Fixed using x64 JRE on x86 (i586) Linux

10.11.0 - 2021-09-04

	General:
		Added link to changelog in auto updater (thanks, Patrick!)
		Fixed Dolby Vision video being detected as Digital Video (dv)
		Fixed browsing on some renderers
		Fixed tsMuxeR not working on macOS (thanks, justdan96!)
	Renderers:
		Improved support for subtitles on LG OLED TVs
	Translation updates via Crowdin:
		Chinese (Traditional) (95%) (thanks, Gene Wu!)
		Czech (99%)
		Danish (100%) (thanks, NCAA!)
		Dutch (97%) (thanks, Jos van der logt!)
		Finnish (100%) (thanks, Esko Gardner!)
		Korean (100%) (thanks, VenusGirl!)
		Polish (100%) (thanks, Karol Szastok!)
		Portuguese (99%) (thanks, mariopinto!)
		Spanish (100%) (thanks, edwardalvarez2011!)
		Turkish (100%) (thanks, Burak Yavuz and Erdin Koc!)
		Ukrainian (19%) (thanks, Paul Furlet!)
	Dependencies:
		Bump checkstyle from 8.45.1 to 9.0
		Bump JNA from 5.8.0 to 5.9.0
		Bump oshi-core from 5.8.0 to 5.8.1
		Bump tsMuxeR to 2021-09-02-01-59-46

10.10.1 - 2021-08-23

	General:
		Up to 4x browsing speed improvement (thanks, ik666!)
		Disabled UPnP search while we fix problems with it
		Fixed sending MusicBrainz info for non-audio files
		Fixed the older HTTP engine
	Renderers:
		Improved support for HDR and high bit depths on LG OLED TVs
		Fixed support for OGG formats on LG EG910V TV
	Dependencies:
		Bump gson from 2.8.7 to 2.8.8
		Bump JRE to 8u302+8
		Bump logback-version from 1.2.3 to 1.2.5

10.10.0 - 2021-08-18

	General:
		Added fully played overlay to TV series thumbnails in Media Library if all episodes are played
		Fixes some renderers not reflecting content updates like fully played thumbnail changes
		Fixed broken API matching for episodes from files with years in the series name
		Fixed files locking during or just after download
		Ignore incomplete files
	Translation updates via Crowdin:
		Romanian (100%) (thanks, FlorinT!)
		Russian (90%) (thanks, Sergei Kotlar!)

10.9.0 - 2021-08-09

	General:
		Added option to download folder as playlist on web interface
		Added option to download media as playlist on web interface
		Changed macOS dock icon to match newer Apple design guidelines for Big Sur+ (thanks, Alex Gurr!)
		Made more room for media titles on web interface
		Added recognition of MP1
		Improved support for Blu-ray (PGS), HDMV, DVB, WebVTT and EIA-608 subtitles
		Fixed crash on SUSE Linux
		Fixed chapter folders showing blank names
	Renderers:
		Improved support for Google Chromecast Ultra
		Improved support for MXPlayer on Google Android
		Improved support for Windows Media Player
		Fixed support for OGG formats on Naim-Mu-So, Roku and Samsung devices
	Translation updates via Crowdin:
		Chinese (Traditional) (100%) (thanks, Gene Wu!)
		Danish (100%) (thanks, NCAA!)
		English (United Kingdom) (43%) (thanks, MrSmithr!)
		Finnish (100%) (thanks, Esko Gardner!)
		French (100%) (thanks, Maxence Vary!)
		German (100%) (thanks, Peter Ollig and pipin!)
		Korean (100%) (thanks, VenusGirl!)
		Polish (100%) (thanks, Karol Szastok!)
		Portuguese (Brazilian) (100%) (thanks, Mauro.A!)
		Russian (89%) (thanks, Artem4ik!)
		Spanish (100%) (thanks, Diego Guerrero, edwardalvarez2011 and Gerardo Ruiz!)
		Thai (3%) (thanks, Surasak Namnongor!)
		Turkish (100%) (thanks, Burak Yavuz!)
	Dependencies:
		Bump checkstyle from 8.44 to 8.45.1
		Bump com.sun.xml.bind-version from 3.0.1 to 3.0.2
		Bump git-commit-id-plugin from 4.0.5 to 4.9.10

10.8.2 - 2021-07-25

	General:
		Fixed a crash on headless environments like Docker
	Translation updates via Crowdin:
		Polish (100%) (thanks, Karol Szastok!)

10.8.1 - 2021-07-24

	General:
		Fixed a crash on headless environments like Docker
	Translation updates via Crowdin:
		Chinese (Simplified) (100%) (thanks, 無情天!)
		English (United Kingdom) (43%) (thanks, MrSmithr!)
		French (100%) (thanks, Archaos!)
		Portuguese (Brazilian) (100%) (thanks, jaccoud!)
		Ukrainian (18%) (thanks, Paul Furlet!)

10.8.0 - 2021-07-22

	General:
		Added youtube-dl engine which fixes support for YouTube channel feeds
		Added automatic names for web feeds in the user interface, and the ability to add/edit manual names for web streams
		Added sample web radio stream
		Added support for pasting YouTube channels directly, instead of needing the feed URLs
		Improved performance and logging for unmonitored files
		Fixed several web content user interface bugs
		Fixed sample web feeds and streams not being loaded on macOS
		Fixed broken sample web feeds
		Fixed/updated readme links
		Fixed web bump interface (thanks, Pikarobbi!)
		Fixed broken thumbnail caching on macOS
		Fixed support for mp3 radio streams on the web interface
		Fixed support for radio streams without thumbnails on the web interface
	Translation updates via Crowdin:
		Danish (100%) (thanks, NCAA!)
		English (United Kingdom) (36%) (thanks, Pete Russell!)
		Finnish (100%) (thanks, Esko Gardner!)
		Italian (99%) (thanks, Dimitri Graffi!)
		Korean (100%) (thanks, VenusGirl!)
		Portuguese (99%) (thanks, mariopinto!)
		Portuguese (Brazilian) (96%) (thanks, Mauro.A!)
		Turkish (100%) (thanks, Burak Yavuz!)
	Dependencies:
		Bump commons-io from 2.10.0 to 2.11.0
		Bump FFmpeg to latest Git versions
		Bump oshi-core from 5.7.5 to 5.8.0
		Bump rome from 1.15.0 to 1.16.0
		Bump spotbugs-maven-plugin from 4.2.3 to 4.3.0
		Bump Video.js from 7.10.1 to 7.13.3

10.7.0 - 2021-07-04

	General:
		Improved detection of movies and anime episodes
		Increased default max memory on Windows machines with 8+ GB RAM
		Removed filename from entries inside the TRANSCODE folder
		Fixed upnp:class in UPnP search results (thanks, ik666!)
		Fixed the "Reset the cache" button not resetting API metadata too
		Fixed failed database upgrade for fully played statuses
		Fixed FFmpeg crash
		Fixed marking a directory as fully played not changing cached thumbnails
	Renderers:
		Improved support for VLC on iPhone
	Translation updates via Crowdin:
		Czech (100%)
		Danish (100%) (thanks, NCAA!)
		Finnish (100%) (thanks, Esko Gardner!)
		French (100%) (thanks, Archaos!)
		Hungarian (94%) (thanks, Zatamon!)
		Korean (100%) (thanks, VenusGirl!)
		Polish (100%) (thanks, Karol Szastok!)
		Portuguese (99%) (thanks, mariopinto!)
		Portuguese (Brazilian) (96%) (thanks, Mauro.A!)
		Romanian (100%) (thanks, FlorinT!)
		Russian (89%) (thanks, Dmitry Lavrentev!)
		Turkish (100%) (thanks, Burak Yavuz!)
	Dependencies:
		Bump assertj-core from 3.19.0 to 3.20.2
		Bump checkstyle from 8.43 to 8.44
		Bump commons-io from 2.9.0 to 2.10.0
		Bump doxia-module-docbook-simple from 1.9.1 to 1.10
		Bump git-commit-id-plugin from 4.0.4 to 4.0.5
		Bump jaudiotagger from 2.2.5 to 2.2.7
		Bump maven-idea-plugin from 2.2.1 to 2.3-atlassian-10
		Bump oshi-core from 5.7.4 to 5.7.5

10.6.0 - 2021-06-03

	General:
		Added support for UPnP searching (thanks, sf666!)
		Removed broken New Media folder, as that functionality exists in the Media Library
		Improved episode detection and prettifying
		Fixed failed database connections with hanging processes
		Fixed description of media library/cache settings
	Renderers:
		Improved support for Sony XBR OLED TVs
	Translation updates via Crowdin:
		Chinese (Simplified) (98%) (thanks, QI wolong!)
		Danish (100%) (thanks, NCAA and GurliGebis!)
		Finnish (100%) (thanks, Esko Gardner!)
		French (100%) (thanks, Archaos!)
		German (98%) (thanks, pipin!)
		Korean (100%) (thanks, VenusGirl!)
		Polish (100%) (thanks, Karol Szastok!)
		Portuguese (99%) (thanks, mariopinto!)
		Portuguese (Brazilian) (96%) (thanks, Mauro.A!)
		Serbian (Cyrillic) (96%) (thanks, Slobodan Simić (Слободан Симић)!)
		Spanish (100%) (thanks, edwardalvarez2011 and fafranco82!)
		Turkish (100%) (thanks, Burak Yavuz!)
		Ukrainian (18%) (thanks, Paul Furlet!)
	Dependencies:
		Bump checkstyle from 8.42 to 8.43
		Bump commons-io from 2.8.0 to 2.9.0
		Bump FLAC on macOS to 1.3.3
		Bump tsMuxeR (justdan96 release) on macOS to 2021-04-30-02-13-20
		Bump gson from 2.8.6 to 2.8.7
		Bump jna-version from 5.7.0 to 5.8.0
		Bump junit5.version from 5.7.1 to 5.7.2
		Bump maven-javadoc-plugin from 3.2.0 to 3.3.0
		Bump oshi-core from 5.5.0 to 5.7.4

10.5.0 - 2021-05-08

	General:
		Added new option to mark a fully played video after moving it to a new folder
		Implemented external API to allow users to perform actions on their UMS instance from an authorized external source, please see docs at https://support.universalmediaserver.com/books/configuration/page/external-api (thanks, ikrahne!)
		Fixed TV series metadata not saving for some series
		Improved episode detection from filenames
		Optimized network use and handling of unimplemented UPnP actions for some renderers
		Fixed audio cover art extraction (thanks, ik666!)
		Fixed database upgrade for some users
		Fixed recognition of some renderers
	Renderers:
		Improved support for H.264 on Panasonic VT60
	Translation updates via Crowdin:
		Danish (99%) (thanks, NCAA and GurliGebis!)
		Finnish (99%) (thanks, Esko Gardner!)
		French (100%) (thanks, Archaos!)
		Italian (98%) (thanks, tiwi90!)
		Korean (99%) (thanks, VenusGirl!)
		Polish (100%) (thanks, Karol Szastok!)
		Portuguese (99%) (thanks, mariopinto!)
		Portuguese (Brazilian) (96%) (thanks, Mauro.A!)
		Serbian (97%) (thanks, Slobodan Simić (Слободан Симић)!)
		Spanish (100%) (thanks, Gerardo Ruiz, fafranco82, Eduardo Martin, and manuel fernandez!)
		Turkish (100%) (thanks, Burak Yavuz!)
		Ukrainian (17%) (thanks, Paul Furlet!)
	Dependencies:
		Bump checkstyle from 8.41 to 8.42
		Bump com.sun.xml.bind-version from 3.0.0 to 3.0.1
		Bump commons-lang3 from 3.11 to 3.12.0
		Bump icu4j from 68.2 to 69.1
		Bump JRE from 15 to 8u292
		Bump maven-project-info-reports-plugin from 3.1.1 to 3.1.2
		Bump MediaInfo from 20.09 to 21.03
		Bump metadata-extractor from 2.15.0 to 2.16.0
		Bump spotbugs-maven-plugin from 4.2.2 to 4.2.3
		Bump twelvemonkeys-imageio-version from 3.6.4 to 3.7.0

10.4.1 - 2021-03-26

	General:
		Fixed broken transcoding for some users
		Fixed file scanner freezing on some files with external subtitles
		Fixed support for HEVC (H.265) via tsMuxeR
	Renderers:
		Fixed detection of some Samsung 4K (UHD) TVs (thanks, jkl16!)
	Translation updates via Crowdin:
		Dutch (100%) (thanks, johnnygood!)
		German (100%) (thanks, pipin!)
		Korean (99%) (thanks, VenusGirl!)
		Norwegian (85%) (thanks, nhanssen88!)
		Spanish (100%) (thanks, Julio Alberto García!)
	Dependencies:
		Bump spotbugs-maven-plugin from 4.2.0 to 4.2.2

10.4.0 - 2021-03-19

	General:
		Added renderer config setting DisableUmsResume for renderers with their own resume support, like Samsung TVs
		Improved speed of transcoding to H.264 by up to 3x
		Fixed MEncoder crashing when trying to downmix 7.1 AAC to 5.1 AC-3
		Fixed renderer SupportedVideoBitDepths setting
		Improved memory use and speed when resume is disabled
		Fixed renderer resolution and bitrate limiting
		Fixed broken transcoding for some users since the previous release
	Renderers:
		Improved support for VC1 codec on Sony Blu-ray UBP-X800M2 (thanks, thechrisgregory!)
		Improved support for many filetypes on Kodi (thanks, thechrisgregory!)
		Improved support for 12-bit video on VLC
	Translation updates via Crowdin:
		Estonian (24%) (thanks, Tanel K!)
		Japanese (97%) (thanks, Katsuhide.M!)
		Russian (91%) (thanks, Dmitry Lavrentev!)
	Dependencies:
		Bump git-commit-id-plugin from 4.0.3 to 4.0.4
		Rolled back FFmpeg

10.3.0 - 2021-03-13

	General:
		Improved automatic maximum bandwidth setting and enabled it by default
		Improved performance of browsing folders
		Fixed being able to click the web interface button before it is browsable
		Fixed matching and prettifying some TV episodes
		Fixed binding to virtual network interfaces by default (can still be forced)
	Renderers:
		Added support for foobar2000 mobile (thanks, jps92!)
		Improved support for Apple TV 4K
		Fixed audio support on Bravia EX 500
	Translation updates via Crowdin:
		Arabic (96%) (thanks, خليل مراطلة !)
		Chinese (Traditional) (thanks, Austin Zhang!)
		Czech (100%)
		Danish (100%) (thanks, NCAA!)
		Finnish (100%) (thanks, Esko Gardner!)
		Italian (96%) (thanks, Roberto crescia and tiwi90!)
		Korean (99%) (thanks, VenusGirl!)
		Polish (100%) (thanks, Karol Szastok!)
		Portuguese (100%) (thanks, mariopinto!)
		Russian (91%) (thanks, Artem4ik and Dmitry Lavrentev!)
		Slovak (96%) (thanks, Lukáš Praznovec!)
		Swedish (100%) (thanks, Erik Karlsson!)
		Turkish (100%) (thanks, Burak Yavuz!)
	Dependency updates:
		Bump checkstyle from 8.40 to 8.41
		Bump FFmpeg to latest
		Bump thumbnailator from 0.4.13 to 0.4.14
		Bump twelvemonkeys-imageio-version from 3.6.2 to 3.6.4

10.2.0 - 2021-02-28

	General:
		Added option to skip the first-run wizard (thanks, Ahmed Adan!)
		Improved speed of API lookups by up to 3x
		Improved support for MusicBrainz (thanks, ik666!)
		Fixed support for some languages, e.g. Arabic
		Fixed and secured some URLs in the code and docs
	Renderers:
		Added support for MediaPlayer by PeteManchester
		Added support for nextCP/2
		Fix auto loading for Sony UBP-X800
	Translation updates via Crowdin:
		Arabic (100%) (thanks, خليل مراطلة !)
		Catalan (100%) (thanks, nikodeimos!)
		French (100%) (thanks, Archaos!)
		Hungarian (100%) (thanks, Viktor Kozma!)
		Slovak (99%) (thanks, Dušan!)
		Swedish (100%) (thanks, Erik Karlsson!)
	Dependency updates:
		Bump CheckStyle to 8.40

10.1.0 - 2021-02-16

	General:
		Added lastPlaybackPosition, lastPlaybackDate, and playbackCount to UPnP responses
		Added bitsPerSample to UPnP responses (thanks, sf666!)
		Fixed VLC transcoding bugs
		Fixed SkipExtensions setting not supporting spaces (thanks, ik666!)
		Fixed tsMuxeR on 64-bit Linux (including Docker) and macOS systems
		Fixed MPlayer/MEncoder on macOS 10.15+
	Translation updates via Crowdin:
		Arabic (100%) (thanks,خليل مراطلة (meratkha)!)
		Catalan (93%) (thanks, nikodeimos!)
		Dutch (100%) (thanks, DJ_eMPe!)
		Korean (99%) (thanks, VenusGirl!)
		Serbian (Cyrillic) (100%) (thanks, Slobodan Simić (Слободан Симић) (slsimic)!)
	Dependency updates:
		Bump junit5.version from 5.7.0 to 5.7.1
		Bump maven-checkstyle-plugin from 3.1.1 to 3.1.2
		Bump MPlayer/MEncoder for macOS to SB67

10.0.1 - 2021-01-29

	General:
		Added some UPnP properties for TV episodes
		Added renderer config option SendDLNAOrgFlags
		Reduced network traffic
		Fixed error when parsing episode filenames without series titles
		Fixed not recognizing TV episodes past episode 99
		Fixed finding metadata for some TV episodes, especially anime
		Fixed not showing some metadata for movies and episodes on the web interface
	Renderers:
		Improved detection of LG OLED TVs
		Improved support for Panasonic HZ1500
		Improved support for Roku Ultra devices
		Improved support for Sony Bravia XH Series TVs (thanks, SimpleUser!)
	Translation updates via Crowdin:
		Arabic (57%) (thanks, meratkha!)
		German (100%) (thanks, pipin!)
		Korean (99%) (thanks, VenusGirl!)
		Norwegian (88%) (thanks, lexmerlin!)
		Romanian (100%) (thanks, FlorinT!)
	Dependency updates:
		Bump assertj-core from 3.18.1 to 3.19.0
		Bump JRE from 15.0.1 to 15.0.2
		Bump oshi-core from 5.3.7 to 5.4.1
		Bump twelvemonkeys-imageio-version from 3.6.1 to 3.6.2

10.0.0 - 2021-01-12 - Changes since 10.0.0-a1

	General:
		Retry media lookups that failed due to transient errors
		Fixed automatic file watching
	Renderers:
		Improved support for Sony Bluray UBP-X800M2 (thanks, thechrisgregory!)
		Fixed UPnP pushing via Panasonic TVs and Samsung Q9 TVs
	Translation updates via Crowdin:
		Czech (100%)
		Danish (100%)
		English (United Kingdom) (32%)
		Finnish (100%)
		Greek (91%)
		French (100%)
		Japanese (99%)
		Polish (100%)
		Portuguese (100%)
		Portuguese (Brazilian) (100%)
		Romanian (99%)
		Serbian (99%)
		Turkish (100%)
	Dependency updates:
		Bump spotbugs-maven-plugin from 4.1.4 to 4.2.0

10.0.0 - 2021-01-12 - Changes since 9.8.3

	DLNA browsing:
		When browsing a TV show in the Media Library, all videos across all seasons are visible.
		There are two new folders within the Movies and TV Shows folders - Filter by Progress and Filter by Information.
		Filter by Progress contains the Watched and Unwatched folders which used to sit within the Videos folder in the Media Library
		Filter by Information lets you filter the videos by a new rich metadata set (actors, genres, country, director, genre, IMDb rating, release date).
		Alongside that is a similar folder Filter by Information which lets you filter the videos by API metadata (actors, genres, etc.)
		New folders Recently Added, Recently Played, In Progress and Most Played are in the Media Library
	Web interface:
		Added breadcrumbs at the top of each page
		When in the TV Shows area of the Media Library, the TV shows themselves are shown as thumbnails, with covers from the API
		When browsing a TV show, a movie, or a TV episode, any API metadata is displayed along with a large cover image, including actors, awards, classification, country, directors, genres, plot, ratings, year, and total seasons.
		On those pages above, the colors on the pages are based on the cover image
		Clicking on an individual actor, country, director, genre, or start year, takes you to a list of other media that matches that metadata
		The last few items in the Recently Added, Recently Played, In Progress and Most Played folders are displayed on the front page
		Each TV show and movie has an IMDb icon and direct link if one is found
		Folder and media covers are shown
		Fixed bugs with the Back button
		Version has moved to the settings menu
		Minor design updates
	General:
		API is enabled even without filename prettifying
		Failed API lookups are debounced by 1 week to avoid network spam
		Changed prettified season/episode number formatting from Series - 101 - Episode to Series S01E01 - Episode
		Fixed some bugs with prettifying to support more files
		Added support for audio playlist thumbnails (thanks, sf666!)
		Playlist folders are correctly identified via UPnP (thanks, sf666!)
		Fixed automatic file watching
	Renderers:
		Improved support for Sony Bluray UBP-X800M2 (thanks, thechrisgregory!)
		Fixed UPnP pushing via Panasonic TVs and Samsung Q9 TVs
	Translation updates via Crowdin:
		Czech (100%)
		Danish (100%)
		English (United Kingdom) (32%)
		Finnish (100%)
		Greek (91%)
		French (100%)
		Japanese (99%)
		Polish (100%)
		Portuguese (100%)
		Portuguese (Brazilian) (100%)
		Romanian (99%)
		Serbian (99%)
		Turkish (100%)
	Dependency updates:
		Bump jQuery from 1.12.0 to 3.5.1
		Bump spotbugs-maven-plugin from 4.1.4 to 4.2.0
		Bump video.js from 7.2.3 to 7.10.1

10.0.0-a1 - 2020-12-22

	DLNA browsing:
		When browsing a TV show in the Media Library, all videos across all seasons are visible.
		There are two new folders within the Movies and TV Shows folders - Filter by Progress and Filter by Information.
		Filter by Progress contains the Watched and Unwatched folders which used to sit within the Videos folder in the Media Library
		Filter by Information lets you filter the videos by a new rich metadata set (actors, genres, country, director, genre, IMDb rating, release date).
		Alongside that is a similar folder Filter by Information which lets you filter the videos by API metadata (actors, genres, etc.)
		New folders Recently Added, Recently Played, In Progress and Most Played are in the Media Library
	Web interface:
		Added breadcrumbs at the top of each page
		When in the TV Shows area of the Media Library, the TV shows themselves are shown as thumbnails, with covers from the API
		When browsing a TV show, a movie, or a TV episode, any API metadata is displayed along with a large cover image, including actors, awards, classification, country, directors, genres, plot, ratings, year, and total seasons.
		On those pages above, the colors on the pages are based on the cover image
		Clicking on an individual actor, country, director, genre, or start year, takes you to a list of other media that matches that metadata
		The last few items in the Recently Added, Recently Played, In Progress and Most Played folders are displayed on the front page
		Each TV show and movie has an IMDb icon and direct link if one is found
		Folder and media covers are shown
		Fixed bugs with the Back button
		Version has moved to the settings menu
		Minor design updates
	General:
		API is enabled even without filename prettifying
		Failed API lookups are debounced by 1 week to avoid network spam
		Changed prettified season/episode number formatting from Series - 101 - Episode to Series S01E01 - Episode
		Fixed some bugs with prettifying to support more files
		Added support for audio playlist thumbnails (thanks, sf666!)
		Playlist folders are correctly identified via UPnP (thanks, sf666!)
	Dependency updates:
		Bump jQuery from 1.12.0 to 3.5.1
		Bump video.js from 7.2.3 to 7.10.1

9.8.3 - 2020-12-22

	General:
		Added option to allow symlinked files to be treat as their real target file (thanks, SurfaceS!)
		Fixed handling of web streams while transcoding with VLC (thanks, fu2x!)
		Fixed support for subtitles with some user and renderer config combinations
		Fixed renderer configuration change-detection not always working (thanks, fu2x!)
		Fixed various errors on the web interface (thanks, SurfaceS!)
		Fixed all code lint and enforce it in GitHub Actions
		Fixed sending empty MusicBrainz requests
		Fixed sending unnecessary network requests
	Renderers:
		Improved support for Sony Bravia XF series
		Improved support for Yamaha A/V receivers
		Improved detection of VLC for desktop
	Translation updates via Crowdin:
		Chinese (Traditional) (100%)
		Czech (100%)
	Dependencies:
		Bump assertj-core from 3.18.0 to 3.18.1
		Bump com.sun.xml.bind-version from 3.0.0-M5 to 3.0.0
		Bump icu4j from 68.1 to 68.2
		Bump maven-pmd-plugin from 3.13.0 to 3.14.0
		Bump MediaInfo to 20.09
		Bump oshi-core from 5.3.4 to 5.3.7
		Bump saaj-impl from 1.5.2 to 2.0.0
		Bump twelvemonkeys-imageio-version from 3.6 to 3.6.1

9.8.2 - 2020-11-08

	General:
		Improved filename prettifying for XviD and x265 videos
		Improved support for WebP images
		Fixed memory leaks
		Fixed duration of resume files via DLNA
		Fixed incorrect resolution metadata if the renderer uses KeepAspectRatioTranscoding
		Fixed not removing old JRE folders on Windows install
		Updated links in documentation (thanks, xaitax!)
		Fixed date on zip logs filename (thanks, Midhun R Nair!)
		Save logs to desktop by default (thanks, dotslash21!)
	Renderers:
		Improved support for Blu-ray and DVD subtitles on VLC for desktop
		Improved detection of Microsoft Edge
		Updated logo for Microsoft Edge
	Translation updates via Crowdin:
		Chinese (Simplified) (100%)
		Chinese (Traditional) (100%)
		Dutch (92%)
		Italian (100%)
		Romanian (100%)
		Serbian (Cyrillic) (86%)
	Dependencies:
		Bump AdoptOpenJDK from 14.0.2 to 15.0.1+9
		Bump assertj-core from 3.17.1 to 3.18.0
		Bump commons-io from 2.7 to 2.8.0
		Bump commons-lang3 from 3.7 to 3.9
		Bump commons-text from 1.3 to 1.9
		Bump git-commit-id-plugin from 4.0.2 to 4.0.3
		Bump icu4j from 67.1 to 68.1
		Bump junit5.version from 5.6.2 to 5.7.0
		Bump junrar from 7.3.0 to 7.4.0
		Bump maven-project-info-reports-plugin from 3.1.0 to 3.1.1
		Bump metadata-extractor from 2.14.0 to 2.15.0
		Bump oshi-core from 5.2.5 to 5.3.4
		Bump spotbugs-maven-plugin from 4.0.4 to 4.1.4

9.8.1 - 2020-09-05

	General:
		Improved speed of renderers and UMS recognizing each other
		Reduced network and CPU loads associated with renderer discovery
		Improved loading of external libraries
		Fixed older macOS auto-updating to UMS for newer macOS
		Fixed quickrun scripts for developers
		Fixed not removing the Windows service firewall rule on uninstall
	Translation updates via Crowdin:
		English (United Kingdom) (33%)
		German (98%)
		Slovak (98%)
	Dependencies:
		Bump assertj-core from 3.16.1 to 3.17.1
		Bump git-commit-id-plugin from 4.0.1 to 4.0.2
		Bump junrar from 6.0.1 to 7.3.0
		Bump maven-resources-plugin from 3.1.0 to 3.2.0
		Bump MediaInfo from 18.12 to 20.08
		Bump oshi-core from 5.2.2 to 5.2.5

9.8.0 - 2020-07-26

	General:
		Started releasing two macOS builds which fixed MEncoder not working on 10.15 (Catalina)
		Improved support for ASF, MKV, MP4, MPEG-PS, MPEG-TS, and WMV videos by adding and improving accuracy of DLNA.ORG_PN values (thanks for testing, carlosvsilva!)
		Reduced CPU use and improved video/audio quality by muxing some supported streams with FFmpeg instead of transcoding
		Other DLNA compatibility fixes, including sending correct framerates, color depths, and audio channel counts
		Improved load time on web interface with many files by 10x or more (thanks, outnos!)
		Fixed forced network interface not always persisting
		Fixed Safari login prompts with HTTPS on web interface (thanks, outnos!)
		Fixed not displaying the Minimize on startup option in GUI on macOS
		Fixed Windows installer not adding a Windows Firewall exception for the service
	Renderers:
		Improved support for Philips PUS 6500 Series TVs
		Improved support for AVI files on Panasonic Viera VT60 TVs
		Improved support for Samsung Q7 Series TVs
		Fixed detection of Panasonic Viera DX Series TVs
		Fixed detection of Samsung Q9 TVs
		Fixed detection of Samsung Soundbar MS750
		Fixed false-detection of XBMC
	Translation updates via Crowdin:
		Danish (100%)
		Korean (34%)
	Dependencies:
		Bump AdoptOpenJDK to 14.0.2
		Bump git-commit-id-plugin from 4.0.0 to 4.0.1
		Bump jna-version from 5.5.0 to 5.6.0
		Bump junrar from 4.0.0 to 6.0.1
		Bump MPlayer/MEncoder for macOS to SB66
		Bump oshi-core from 5.2.1 to 5.2.2
		Bump rome from 1.14.1 to 1.15.0
		Bump twelvemonkeys-imageio-version from 3.5 to 3.6

9.7.2 - 2020-07-11

	General:
		Fixed HTTPS and authentication in the web interface (thanks, outnos!)
		Fixed Windows service
		Fixed auto updater getting stuck on program startup
		Fixed high CPU usage for some users
		Fixed fontconfig warning
	Renderers:
		Added support for Sony STR-DN1080 AV Receiver (thanks, GurliGebis!)
		Fixed detection of newer versions of VLC on Apple TV (thanks, ajkessel!)
	Translation updates via Crowdin:
		Bulgarian (100%)
		French (100%)
		Hungarian (100%)

9.7.1 - 2020-07-05

	General:
		Implemented the minimize on startup option on macOS
		Added dates to the debug log zip (thanks, midhun1998!)
		Added profiling logging for the database
		Windows installer stops the existing service if it is running
		Windows installer starts the service if the checkbox is enabled at the end and the service is installed, instead of starting the GUI
		Windows installer does not try to start the GUI on computer startup if the service is installed
		GUI warns about using it as a GUI when it is already installed as a service
		The service uses our bundled Java instead of the system one
		Changed the default AC-3 transcoding bitrate to 448 for better transcoding compatibility
	Translation updates via Crowdin:
		Czech (100%)
		Danish (99%)
		English (United Kingdom) (25%)
		Finnish (100%)
		Polish (100%)
		Portuguese (100%)
		Portuguese (Brazilian) (100%)
		Russian (93%)
		Slovak (98%)
		Spanish (99%)
		Turkish (100%)
		Ukrainian (17%)
	Dependency updates:
		Bump maven-site-plugin from 3.9.0 to 3.9.1
		Bump oshi-core from 5.1.2 to 5.2.0
		Bump rome from 1.13.1 to 1.14.1
		Bump spotbugs-maven-plugin from 4.0.0 to 4.0.4

9.7.0 - 2020-06-21

	General:
		Improved browsing and scanning speed and stability
		Optimized database storage space
		Fixed aspect ratio comparisons
		Fixed the "Check for updates" button not finding updates
		Fixed support for Eclipse development
		Fixed freezes while browsing and scanning a folder at the same time
	Renderers:
		Improved support for high bit-depth videos on Samsung and Sony TVs, and VLC
		Fixed transcoding support in some cases on DirecTV, Panasonic, Samsung, Sony and Vizio TVs, and Android phones
	Translation updates via Crowdin:
		Chinese Traditional (100%)
		French (100%)
		Hungarian (100%)
		Polish (100%)
		Spanish (100%)
	Dependency updates:
		Bump surefire-version from 3.0.0-M4 to 3.0.0-M5

9.6.2 - 2020-06-15

	General:
		Fixed broken browsing on some renderers
		Fixed broken transcoding on renderers with KeepAspectRatio enabled
		Improved support for web interface on iOS (thanks, Jeff Puls!)
	Renderers:
		Updated support for external subtitles on VLC for Desktop (thanks, JuanPZ!)
	Translation updates via Crowdin:
		Russian (94%)
	Dependency updates:
		netty from 4.1.50 to 3.10.6

9.6.1 - 2020-06-14

	General:
		Fixed Linux startup error

9.6.0 - 2020-06-12

	General:
		Improved compatibility of files containing both supported and unsupported streams
		Improved support for MP4 and OGA/OGG audio on the web interface
		Fixed automatic updating on Windows and macOS
		Fixed renderer control windows not refocusing on Windows when renderer image was clicked
		Fixed support for external subtitles when using the se syntax in renderer config
		Fixed not transcoding embedded subtitles when we should
	Renderers:
		Updated support for external subtitles on VLC for iOS (thanks, JuanPZ!)
		Updated support for external subtitles on Panasonic VT60
	Translation updates via Crowdin:
		English (British) (5%)
		Macedonian (0%)
	Dependency updates:
		commons-io from 2.6 to 2.7
		exec-maven-plugin from 1.6.0 to 3.0.0
		JRE from 8 to 14.0.1
		maven-project-info-reports-plugin from 3.0.0 to 3.1.0
		metadata-extractor from 2.13.0 to 2.14.0
		netty from 3.10.6 to 4.1.50
		oshi-core from 5.1.0 to 5.1.2
		rome from 1.12.2 to 1.13.1

9.5.0 - 2020-05-24

	General:
		Significant improvements to scanning and browsing speed and resource use
		Fixed episode titles in the Media Library
		Fixed aspect ratio validation
		Added more automatic regression tests for file format detection
	Renderers:
		Added support for Vimu Player on Amazon Fire TV Stick (thanks, nouse and Nadahar!)
	Translation updates via Crowdin:
		Dutch (92%)
		Turkish (100%)
	Dependency updates:
		7zipj from 9.20-2.00 to 16.02-2.01
		assertj-core from 3.16.0 to 3.16.1
		junrar from 1.0.1 to 4.0.0
		oshi-core from 5.0.1 to 5.1.0

9.4.3 - 2020-05-09

	General:
		Added support for streaming and transcoding from AV1 video codec
		Added quickrun commands to aid rapid development
		Memory use improvements on macOS
		Transcoding compatibility fixes
		Fixed support for transcoding videos with no audio
	Renderers:
		Improved support for LG OLED TVs
		Improved support for Panasonic VT60 TVs
		Improved support for Sony AG-series TVs
		Improved support for Sony X-series TVs
	Translation updates via Crowdin:
		Serbian (87%)
		Turkish (100%)
	Dependencies:
		Updated assertj to 3.16.0
		Updated doxia-module-docbook-simple to 1.9.1
		Updated FFmpeg on macOS to 20200504 (5767a2e) to support more media formats
		Updated git-commit-id-plugin to 4.0.0
		Updated h2database to 1.4.199
		Updated icu4j to 67.1
		Updated jaxb-impl to 2.3.3
		Updated JMustache to 1.15
		Updated JNA to 5.5.0
		Updated junit5 to 5.6.2
		Updated maven-antrun-plugin to 3.0.0
		Updated maven-assembly-plugin to 3.3.0
		Updated maven-checkstyle-plugin to 3.1.0
		Updated maven-compiler-plugin to 3.8.1
		Updated maven-pmd-plugin to 3.13.0
		Updated maven-project-info-reports-plugin to 3.0.0
		Updated maven-site-plugin to 3.9.0
		Updated metadata-extractor to 2.13.0
		Updated Oshi to 5.0.1
		Updated plexus-utils to 3.3.0
		Updated rome to 1.12.2
		Updated saaj-impl to 1.5.2
		Updated spotbugs-maven-plugin to 4.0.0

9.4.2 - 2020-04-21

	General:
		Improved detection of M4V and MP4 files
		Improved automated regression tests for media format detection
		Improved detection of incomplete binaries
		Fixed Linux failing to use bundled FFmpeg (thanks, snicket2100!)
		Fixed support for custom server names with special characters (thanks, snicket2100!)
	Renderers:
		Improved support for H.264 videos on VLC for iOS
	Translation updates via Crowdin:
		Bulgarian (96%)
		Finnish (100%)
		French (100%)
		Hungarian (84%)
		Romanian (100%)
		Slovak (100%)
	Dependencies:
		Updated Chromecast api-v2 to 0.11.3
		Fixed broken FFmpeg binary on Linux x86
		Updated Google Gson to 2.8.6
		Updated Google Guava to 29.0
		Updated h2database to 1.4.200
		Updated Java Runtime Environment to 1.8.251

9.4.1 - 2020-04-08

	Translation updates via Crowdin:
		Bulgarian (93%)
		Czech (100%)
		Danish (99%)
		Finnish (99%)
		French (99%)
		Hebrew (21%)
		Italian (100%)
		Polish (99%)
		Portuguese (99%)
		Portuguese (Brazilian) (99%)
		Spanish (100%)
		Swedish (100%)
		Ukrainian (23%)
	Dependencies:
		Updated FFmpeg on macOS to fix a startup bug
		Updated JUnit5 to 5.6.1
		Updated Maven Javadoc plugin to 3.2.0
		Updated Maven Assembly plugin to 3.2.0

9.4.0 - 2020-04-05

	General:
		Started releasing 5 Linux builds: x86, x86_64, ARM, ARMhf and ARM64
		Linux builds all default to using the system FFmpeg if it exists
		Improved transcoding speed
		Fixed broken cache loading in some situations
		Fixed transcoding bugs
		Fixed subtitles bugs
		Improved logging
	Renderers:
		Added support for Denon AVR-4311CI (thanks, sc3141!)
		Added support for Denon AVR-X4200W (thanks, sc3141!)
	Translation updates via Crowdin:
		Danish (99%)
		Greek (92%)
		Hebrew (19%)
		Portuguese (99%)
		Swedish (99%)
	Dependencies:
		Fixed incorrect MediaInfo version on Windows, now it is 18.12
		Updated FFmpeg to 3362330 (20200328) on all operating systems
		Updated Twelvemonkeys ImageIO to 3.5

9.3.1 - 2020-03-22

	General:
		Updated build documentation in BUILD.md (thanks, luca-vercelli!)
	Renderers:
		Fixed too much transcoding on Samsung 9 series TVs
		Fixed WAV support on PS3
	Translation updates via Crowdin:
		Arabic (36%)
		Serbian (87%)
		Slovenian (60%)

9.3.0 - 2020-02-23

	General:
		Added support for devices that require MRR authorization, like Denon AVR devices (thanks, sc3141)
		Added support for symlinks in the folder selector
		Fixed subtitles being transcoded unnecessarily
		Fixed compiling on new OpenJDK versions
		Improved support for audio files
		Improved parsing of uncommon files
		Fixed attempting to add empty files
		Added some regression testing for our use of FFmpeg and MediaInfo
	Dependencies:
		Updated JRE to 1.8.241

9.2.0 - 2020-01-13

	General:
		Added more precise support for subtitles in renderer profiles. See the si and se options in DefaultRenderer.conf.
		Fixed uninstallation of Windows service (thanks, pponce!)
		Use secure connections for program updates (thanks, xaitax!)
		Fixed startup on Linux using ARM processors (thanks, felsen2011!)
		Logging improvements
	Renderers:
		Added support Sony BRAVIA AG series TVs (thanks, rubin55!)
		Improved support for 2019 Samsung TVs, including 8K streaming
		Improved support for Panasonic GX800B
	Translation updates via Crowdin:
		Chinese Traditional (100%)
		Croatian (44%)
		Danish (100%)
		English (United Kingdom) (3%)
		Hungarian (71%)
		Korean (33%)
		Slovak (100%)
		Slovenian (60%)
		Swedish (97%)

9.1.0 - 2019-11-01

	General:
		Added automatic updating to Linux and macOS (it already existed on Windows)
		Added the UMS version to the web interface
		Added a different icon in macOS dark mode (thanks, bcbomb47!)
		Fixed pixelation of icon on some Linux distributions
	Renderers:
		Fixed support for external subtitles on some Samsung TVs (thanks, pipin!)
	Translation updates via Crowdin:
		Bulgarian (93%)
		Croatian (29%)
		Danish (99%)
		Dutch (93%)
		Finnish (100%)
		Italian (100%)
		Korean (33%)
		Persian (35%)
		Turkish (100%)
	Dependencies:
		Updated JRE to 1.8.231

9.0.1 - 2019-10-06

	General:
		Fixed not using standalone Java on Linux
		Increased default maximum memory on Linux to match other OS (1280MB)
		Improved speed of some database lookups
		Improved speed of OpenSubtitles validation
		Logging improvements
		Switched from findbugs to spotbugs
		Fixed broken tooltips
		Fixed startup link not uninstalling on Windows
		Fixed error when prettifying some filenames
	Renderers:
		Improved support for Mirascreen  (thanks, Dušan Kazik)
		Improved support for Sony BluRay BDP-S3700
	Translation updates via Crowdin:
		Bulgarian (91%)
		Chinese Traditional (100%)
		Croatian (28%)
		Estonian (22%)
		Hungarian (70%)
		Russian (94%)
		Slovak (88%)
		Spanish (100%)
		Swedish (93%)
	Dependencies:
		Updated ImageIO to 3.4.2

9.0.0 - Changes since 9.0.0-b2 - 2019-09-06

	General:
		Adds UMS to Windows Firewall exceptions on install
		Better cleanup of install directory on install/uninstall on Windows
		Fixed transcoding when using our Docker image (thanks, tcely!)
		Fixed track numbers not prepending in Media Library
		Fixed startup crash when invalid characters are in the OS Path
		Fixed adding unsupported files to the database
	Renderers:
		Improved support for TrueHD videos on Samsung UHD TVs
	Translation updates via Crowdin:
		Chinese Traditional (94%)
		Croatian (20%)
		Czech (100%)
		Danish (89%)
		Finnish (32%)
		Japanese (100%)
		Polish (100%)
		Russian (93%)
		Slovak (88%)
		Thai (11%)
	Dependencies:
		Updated Git Commit ID Plugin to 2.2.4
		Updated JUnit to 5.2.0
		Updated Maven Compiler Plugin to 3.8.0
		Updated Maven Enforcer Plugin to 3.0.0-M1
		Updated Maven Javadoc Plugin to 3.0.1
		Updated Maven Site Plugin to 3.7
		Updated Metadata Extractor to 2.12.0 for improved image parsing performance

9.0.0 - Changes since 8.2.0 - 2019-09-06

	General:
		We no longer require Java installation on any operating system
		Adds UMS to Windows Firewall exceptions on install
		Added new renderer setting vbd (video bit depth) to allow filetype-specific bit-depth support configuration
		Fixed filename prettifying not displaying episode names and other related problems
		Fixed the PrependTrackNumbers renderer setting (thanks, tcely!)
		Fixed transcoding when using our Docker image (thanks, tcely!)
		Fixed startup crash when invalid characters are in the OS Path
		Fixed adding unsupported files to the database
	Renderers:
		Improved support for TrueHD videos on Samsung UHD TVs
	Languages:
		Fixed broken "hide engines" string
		Updated translations via Crowdin:
			Chinese Simplified (100%)
			Chinese Traditional (94%)
			Croatian (20%)
			Czech (100%)
			Danish (89%)
			Finnish (32%)
			French (100%)
			German (100%)
			Hungarian (68%)
			Italian (98%)
			Japanese (100%)
			Norwegian (89%)
			Polish (100%)
			Portuguese (Brazilian) (100%)
			Romanian (100%)
			Russian (93%)
			Slovak (88%)
			Spanish (96%)
			Swedish (93%)
			Thai (11%)
	Dependencies:
		Updated Chromecast api-v2 to 0.11.0
		Updated fm.last coverartarchive-api to 2.1.1
		Updated Git Commit ID Plugin to 2.2.4
		Updated icu4j to 64.2
		Updated JNA to 5.3.1
		Updated JUnit to 5.2.0
		Updated Maven Compiler Plugin to 3.8.0
		Updated Maven Enforcer Plugin to 3.0.0-M1
		Updated Maven Javadoc Plugin to 3.0.1
		Updated Maven Site Plugin to 3.7
		Updated Metadata Extractor to 2.12.0 for improved image parsing performance
		Updated Oshi to 3.13.3
		Updated Surefire to 2.22.2

9.0.0-b2 - 2019-07-30

	General:
		Fixed startup on Windows and Linux

9.0.0-b1 - 2019-07-26

	General:
		We no longer require Java installation on any operating system
		Added new renderer setting vbd (video bit depth) to allow filetype-specific bit-depth support configuration
		Fixed filename prettifying not displaying episode names and other related problems
		Fixed the PrependTrackNumbers renderer setting (thanks, tcely!)
	Languages:
		Fixed broken "hide engines" string
		Updated translations via Crowdin:
			Chinese Simplified updated
			French translation updated
			German translation updated
			Hungarian translation updated
			Italian translation updated
			Japanese translation updated
			Norwegian translation updated
			Polish translation updated
			Portuguese (Brazilian) translation updated
			Romanian translation updated
			Russian translation updated
			Spanish translation updated
			Swedish translation updated
	Dependencies:
		Updated Chromecast api-v2 to 0.11.0
		Updated fm.last coverartarchive-api to 2.1.1
		Updated icu4j to 64.2
		Updated JNA to 5.3.1
		Updated Oshi to 3.13.3
		Updated Surefire to 2.22.2

8.2.0 - 2019-06-21

	General:
		Removed duplicate information from TV episode filenames in the Media Library
		Improved filename recognition
		Append engines to filenames instead of prepend, to stop breaking alphabetization
		Added the possibility to have a virtual folder without adding it to the Media Library (thanks, maciekberry!)
		Fixed UMS not starting as a service on Windows (thanks, pponce!)
		Fixed a broken translation when alerting a user that they are sharing a non-existing folder
	Languages:
		Updated translations via Crowdin:
			Afrikaans translation updated
			Arabic translation updated
			Catalan translation updated
			Croatian translation updated
			Czech translation updated
			Danish translation updated
			Dutch translation completed
			German translation updated
			Greek translation updated
			English (UK) translation updated
			Finnish translation updated
			French translation updated
			Hebrew translation updated
			Hungarian translation updated
			Icelandic translation updated
			Italian translation updated
			Japanese translation completed
			Korean translation completed
			Norwegian translation updated
			Persian translation updated
			Polish translation updated
			Portuguese translation updated
			Portuguese (Brazilian) translation updated
			Romanian translation updated
			Russian translation updated
			Serbian (Cyrillic) translation updated
			Slovak translation updated
			Slovenian translation updated
			Spanish translation updated
			Thai translation updated
	Dependencies:
		Updated Apache HttpAsyncClient to 4.1.4

8.1.0 - 2019-05-03

	General:
		Performance improvements
		Fixed conversion of 3D subtitles
		Fixed bug with the computer sleep management feature (thanks, scriptorron and Nadahar!)
		Fixed bugs with Virtual Folders
		Fixed error when browsing web folders
		Updated build and install documentation (thanks, Ruben Barkow!)
	Renderers:
		Improved support for 4k and h265 videos on LG webOS TVs
		Improved support for Samsung MS750 soundbars
	Languages:
		Updated translations via Crowdin:
			Danish translation updated
			Dutch translation updated
			French translation updated
			Japanese translation validated
			Portuguese (Brazilian) translation updated
			Russian translation updated
			Turkish translation updated
	Dependencies:
		Updated h2database to 1.4.199, which improves speed and memory use

8.0.1 - 2019-03-31

	Dependencies:
		Rolled back h2database to 1.4.196, which fixes broken music metadata

8.0.0 - Changes since 7.9.0 - 2019-03-29

	General:
		Added new Shared Content tab for managing all local and web content
		Added country flags for audio and subtitles to video thumbnails in TRANSCODE folders
		Added option to customize the subtitles info that is appended to filenames
		Made the subtitles info more standardized
		Greatly optimized memory use and long-term stability (thanks, skeptical!)
		Improved default web content entries
		Database cleanup step removes files that are no longer shared
		Fixed a lot of bugs related to subtitles
		Fixed functionality of the Back button on the web interface in some situations
		Fixed "Season" not displaying on subsequent visits in Media Library
		Optimized performance of Media Library folders
		Improved reliability when using "Defer to MEncoder" option
		Fixed some broken Media Library queries
	Renderers:
		Allow MP3 streaming on VLC
		Transcode unsupported audio to MP3 on Samsung JU6400
		Fixed video transcoding on Sony Bravia EX TVs
	Languages:
		Updated translations via Crowdin:
			Dutch translation updated
			Italian translation completed
			Japanese translation completed
			Persian translation updated
			Polish translation completed and validated
			Russian translation updated
			Slovenian translation updated
			Serbian (Cyrillic) translation updated
			Ukrainian translation updated
	Dependencies:
		Updated MediaInfo to 18.12 on Windows and macOS
		Updated h2database to 1.4.198

8.0.0 - Changes since 8.0.0-RC1 - 2019-03-29

	General:
		Improved reliability when using "Defer to MEncoder" option
		Fixed functionality of the Back button on the web interface in some situations
		Fixed "Season" not displaying on subsequent visits in Media Library
		Optimized performance of Media Library folders
		Fixed some broken Media Library queries
		Fixed music info (artist, album, genre, album artist, and year) not being saved to the resource or database
	Renderers:
		Allow MP3 streaming on VLC
		Transcode unsupported audio to MP3 on Samsung JU6400
		Fixed video transcoding on Sony Bravia EX TVs
	Languages:
		Updated translations via Crowdin:
			Dutch translation updated
			Italian translation completed
			Japanese translation completed
			Persian translation updated
			Polish translation completed and validated
			Russian translation updated
			Slovenian translation updated
			Serbian (Cyrillic) translation updated
			Ukrainian translation updated
	Dependencies:
		Updated h2database to 1.4.198

8.0.0-RC1 - 2019-2-22

	General
		Database cleanup step removes files that are no longer shared
		Cleanup of new config settings
		All changes in 7.9.0

7.9.0 - 2019-02-22

	General:
		Added the ability to ignore folders by name, which defaults to ".unwanted"
		The server shows up on devices sooner
		Increased the difference between button hover/pressed states
		Improved stability and logging when moving files after fully playing
		Improved recognition of AAC
		Fixed H.264 profile not updating
		Fixed low bitrate audio on transcoded web videos (thanks, SsJVasto!)
		Fixed content updating in the Media Library on some devices
		Logging fixes
	Renderers:
		Improved support for Roku 4
		Improved support for Samsung 9 Series TVs
	Languages:
		Updated translations via Crowdin:
			Dutch translation updated
			Bulgarian translation updated
			Korean translation updated
			Swedish translation updated
			Turkish translation updated

8.0.0-b1 - 2019-01-12

	General:
		Added option to disable FFmpeg GPU acceleration
		Fixed too much transcoding due to not respecting language priority settings
		Fixed failure to store audio stream information
		Fixed failure to start
		Fixed missing text when adding web content
		Fixed missing text on the first time language chooser
		Fixed text clash on the Subtitles settings subtab of the Transcoding settings tab
		Fixed logspam
		All changes in 7.8.0

7.8.0 - 2019-01-12

	General:
		Added examples for all binary tools paths in UMS.conf (thanks, Amadeus-!)
		Added detection of JDK to the Windows installer (thanks, siboXD!)
		Fixed custom option parsing for ffmpeg_gpu_decoding_acceleration_method (thanks, goncalossilva!)
		Fixed error when FFmpeg deferred to MEncoder
	Web interface:
		Added Subtitle Translator to the menu (thanks, atamariya!)
		Added new home screen with automatic entry (thanks, Abel Espinosa!)
		Added player navigation using keys to return to home screen (thanks, Abel Espinosa!)
		Added new logo icon fully displayed and transparent in web interface browse view (thanks, Abel Espinosa!)
		Made caption, folder list and play toolbar buttons compliant with material design (thanks, Abel Espinosa!)
		Fixed audio thumbnails (thanks, Abel Espinosa!)
		Fixed some buttons not displaying correctly (thanks, Abel Espinosa!)
		Fixed broken hover effect on Firefox (thanks, Abel Espinosa!)
		Modified menu icons
	Languages:
		Updated translations via Crowdin:
			French translation updated
			Icelandic translation updated
			Japanese translation updated
			Ukranian translation updated

8.0.0-a2 - 2018-12-27 - Changes since 8.0.0-a1

	General:
		Fixed support for picture-based subtitles
		Fixed images being displayed in low quality
		Fixed a bug when writing OpenSubtitles data to the database
		Fixed audio flags in TRANSCODE folders
		All changes in 7.7.1
	Dependencies:
		Updated MediaInfo to 18.12 on Windows and macOS

7.7.1 - 2018-12-27

	General:
		Fixed broken transcoding via MEncoder on Windows and Linux
		Improved support for Sony BRAVIA EX Series TVs
	Languages:
		Updated translations via Crowdin:
			French translation updated
			German translation completed
			Portuguese translation completed
			Portuguese (Brazilian) translation completed

8.0.0-a1 - 2018-12-21 - Changes since 7.7.0

	General:
		Added new Shared Content tab for managing all local and web content
		Added country flags for audio and subtitles to video thumbnails in TRANSCODE folders
		Added option to customize the subtitles info that is appended to filenames
		Made the subtitles info more standardized
		Greatly optimized memory use and long-term stability (thanks, skeptical!)
		Improved default web content entries
		Fixed a lot of bugs related to subtitles

7.7.0 - 2018-12-21

	General:
		Improved code formatting (thanks, drakulis!)
		Fixed double subtitles when transcoding video
		Fixed XViD recognition
		Fixed incorrect music genre labels (thanks, maciekberry!)
		Fixed web stream transcoding
	Renderers:
		Improved support for Philips PUS TVs
		Improved support for Samsung Q6 Series TVs
		Improved support for Samsung Q9 Series TVs
	Web interface:
		Use H.264 on the web interface in Chrome and Firefox
		Removed the limit on resolution
		Added font scaling setting (thanks, Abel Espinosa!)
		Removed custom scrollbar styling (thanks, Abel Espinosa!)
		Fixed a hover effect bug on Firefox (thanks, Abel Espinosa!)
		Fixed the dynamic view (thanks, Abel Espinosa!)
	Languages:
		Updated translations via Crowdin:
			Chinese Simplified translation completed
			Czech translation completed and validated
			German translation updated
			Hungarian translation updated
			Portuguese (Brazilian) translation updated
			Romanian translation completed and validated
			Russian translation updated
			Slovak translation updated
			Thai translation updated

7.6.2 - 2018-11-22

	General:
		Fixed broken browsing on some devices

7.6.1 - 2018-11-22

	General:
		Increased thumbnail compression for less memory use
		Fixed broken browsing on some Samsung devices (thanks, drakulis!)
		Fixed fully played thumbnails not working after restarts
		Improved code formatting (thanks, drakulis!)
	Languages:
		Chinese Simplified translation updated
		Swedish translation updated

7.6.0 - 2018-11-16

	General:
		Started to release installer wizard for macOS (thanks, js-kyle!)
		Started to use the "Album Artist" field for better audio file browsing of compilations (thanks, maciekberry!)
		Improved network connection stability (thanks, reorder!)
		Improved the web interface's accessibility and support for remote controls (thanks, acanive!)
		Removed the broken whitelist functionality (thanks, Nadahar!)
		Removed the broken plugins functionality (thanks, Nadahar!)
		Improved the Docker configuration (thanks, atamariya!)
		Improved the speed of our automated testing suite
		Improved automated testing of filename prettifying (thanks, stasinos!)
		Improved our default folder sharing stability (thanks, Nadahar!)
		Improved support for aspect ratio and scan order (thanks, Nadahar!)
		Improved file scanning/parsing speed
		Fixed tsMuxeR on macOS (thanks, onon765trb!)
		Fixed burning picture subtitles when other filters are also used in FFmpeg
		Fixed FFmpeg not enabling the GPU configuration toggle
		Fixed thumbnails not persisting to the database unless TRACE logging is enabled
		Fixed binaries not being picked up from the PATH on Linux (thanks, Nadahar!)
		Fixed support for Java 10+ on Linux
		Added build documentation to BUILD.md
		Updated documentation in CONTRIBUTING.md and STYLEGUIDE.md
		Improved code formatting (thanks, drakulis!)
	Formats/Codecs: (thanks, Sami32!)
		Added recognition of video codecs: ASF, FFV1, RLE, S4UD, TGA and VRO
		Added recognition of audio formats: CAF
		Added recognition of audio codecs: CELP, MACE, Nellymoser and QCELP
		Fixed parsing of framerates via MediaInfo
		Improved recognition of AAC, FLV and WMA
	Renderers:
		Added support for Bush Freeview (thanks, atamariya!)
		Added support for the built-in "Resume" functionality on Samsung TVs (thanks, drakulis!)
		Improved support for H.264 codec and SRT subtitles on PS4 (thanks, fgimenezm!)
	Languages:
		Updated translations via Crowdin:
			Arabic translation updated
			Bulgarian translation updated
			Catalan translation updated
			Chinese Simplified translation updated
			Chinese Traditional translation updated
			Croatian translation updated
			Czech translation updated
			Danish translation updated
			Dutch translation updated
			Finnish translation updated
			French translation completed and validated
			German translation updated
			Greek translation updated
			Hungarian translation updated
			Italian translation completed
			Japanese translation updated
			Korean translation updated
			Norwegian translation updated
			Persian translation updated
			Polish translation completed and validated
			Portuguese translation updated
			Portuguese (Brazilian) translation updated
			Romanian translation updated
			Russian translation updated
			Slovak translation updated
			Slovenian translation updated
			Serbian (Cyrillic) translation updated
			Spanish translation completed
			Swedish translation updated
			Turkish translation updated
	Dependencies:
		Added commons-text 1.3 (thanks, Nadahar!)
		Updated Cling to 2.1.2
		Updated commons-io to 2.6 (thanks, Nadahar!)
		Updated commons-lang3 to 3.7
		Updated JUnrar to 1.0.1 (thanks, Nadahar!)
		Updated Seamless to 1.1.2
		Updated sevenzip jbinding to 9.20-2.00beta
		Updated tsMuxeR on macOS to 2.6.11

7.5.0 - 2018-10-13

	General:
		Added animated Restart Server button when a restart is needed to apply a new setting (thanks, Nadahar!)
		Added hover and push effects to buttons (thanks, Nadahar!)
		Added Docker build files (thanks, atamariya!)
		Added ability to specify supported framerates in renderer configs (thanks, Nadahar!)
		Improved support for Windows XP (thanks, Nadahar!)
		Improved support for DFF, DSF, MP4 and WAV files (thanks, Sami32!)
		Updated the GUI for FFmpeg options
		Fixed duplicate hardware acceleration options
		Fixed the state of scan buttons while startup scan is running
		Fixed the display of the web interface on Safari
		Fixed MEncoder not transcoding to H.264 on macOS
		Fixed a lot of minor bugs
	Renderers:
		Added detection of more Samsung mobile devices (thanks, Sami32!)
		Improved support for Onkyo audio receivers
		Improved support for Samsung UHD (4k) TVs (thanks, SurfaceS!)
		Improved support for Samsung Galaxy Note Tab (thanks, Sami32!)
		Fixed FLAC playback on Xbox One (thanks, 25233Guyver!)
	Languages:
		Updated translations via Crowdin:
			Chinese Simplified translation completed
			Czech translation completed and validated
			Portuguese (Brazilian) translation updated
			Slovak translation updated
			Spanish translation completed
	Dependencies:
		Updated Maven Assembly plugin to 3.1.0
		Updated Maven Compiler plugin to 3.7.0
		Updated Maven Enforcer plugin to 1.4.1
		Updated Maven Findbugs plugin to 3.0.5
		Updated Git Commit ID plugin to 2.2.3
		Updated gson to 2.8.2

7.4.0 - 2018-09-22

	General:
		Uses the media folders by default on Windows and macOS instead of the whole user directory
		Added support for using GPU (video cards) for decoding via FFmpeg (thanks, onon765trb!)
		Added option for higher quality audio resampling via FFmpeg (thanks, onon765trb!)
		Files downloaded via the web interface have the correct filename (thanks, js-kyle!)
		Improved stability when using custom FFmpeg settings (thanks, onon765trb!)
		Made plugin web queries use SSL/HTTPS
		Stop sometimes throwing errors when downgrading versions
		Disabled the broken minimization on macOS
		Fixed the wizard file chooser not working on macOS
	Languages:
		Updated translations via Crowdin:
			Chinese Traditional translation completed
			French translation completed and validated
			Italian translation updated
			Polish translation completed and validated
	Renderers:
		Fixed aspect ratio of transcoded videos on Panasonic ST60 TVs

7.3.1 - 2018-09-01

	General:
		Fixed transcoding of videos with multiple audio streams with FFmpeg
		Fixed not being able to delete folders containing folders that are within watched folders on Windows
		Fixed Fully Played status not saving for some users
		Fixed files not being immediately deleted from the database if their parent folder was deleted
		Fixed standalone build auto updater downloading non-standalone builds
	Languages:
		Updated translations via Crowdin:
			Chinese Simplified translation completed
			French translation completed and validated
			Italian translation updated
			German translation completed
			Polish translation completed and validated
			Portuguese translation completed
			Portuguese (Brazilian) translation updated
			Romanian translation completed
			Russian translation completed
			Spanish translation completed

7.3.0 - 2018-08-13

	General:
		Started to release standalone builds for Windows (no Java installation required)
		Added option to disable startup folder scanning in the first-run wizard
		Fixed fully played status sometimes not saving
	Languages:
		Updated translations via Crowdin

7.2.1 - 2018-07-29

	General:
		Fixed a database upgrade bug
	Languages:
		Updated Bulgarian translation via Crowdin

7.2.0 - 2018-07-27

	General:
		Auto-updater no longer requires UMS to be run with admin rights on Windows
		Improved support for ATRAC and DolbyE formats
		Reduced size of local database dramatically
		Increased maximum memory limit on macOS to match Windows
		Disabled Chromecast Extension API by default
		Improved documentation
		Improved database referential integrity
		Fixed bugs with the auto-updater on Windows
	Languages:
		Allow translation of more of the auto-updater
		Updated translations via Crowdin

7.1.0 - 2018-06-16

	General:
		Aspect ratio is maintained in web interface thumbnails
		Updated web interface to play OGA audio
		Cleaned up and fixed DefaultRenderer.conf
		Display whether video is a sample with prettifying enabled
		Fixed AAC audio parsing via FFmpeg
		Fixed movie edition not being displayed
		Fixed handling of multiple video streams while transcoding
		Improved logging
		Fixed Resume file support on some renderers
		Fixed maximum memory setting on Java 9+
	Languages:
		Added Croatian translation (thanks, Ram Demon!)
		Updated translations via Crowdin
		Fixed incorrect translations being applied to some settings
	Renderers:
		Improved support for Panasonic DX series TVs
		Fixed external ASS/SSA subtitles support on Samsung 8 and 9 series TVs
	Dependencies:
		Updated MediaInfo to 18.03.1

7.0.1 - 2018-04-15

	General:
		Improved thumbnail support
		Improved folder scanning speed
		Reduced memory use during folder scanning
		Fixed fully played feature on clean installs
		Fixed tsMuxeR FFmpeg support
		Updated UMS.conf with changes from 7.0.0
	Languages:
		Updated German, Italian, Norwegian, Russian and Swedish translations via Crowdin
	Renderers:
		Added support for Roku TV (NSP 8)
		Improved support for Roku 3

7.0.0 - 2018-03-27 - Changes since 6.8.0

	General:
		Added "TV Shows", "Movies", "3D Movies", and "Unsorted" folders to the "Media Library" folder
		Added right-click menu to navigation pane
		Renamed "Cache" folder to "Media Library" folder and enabled it by default
		Changed "Hide" options to "Show", e.g. "Hide Media Library folder" becomes "Show Media Library folder"
		When a file is added/changed/removed in a shared folder, UMS detects it
		Fully played tracking is stored in the SQL database
		Shared folders are scanned for changes on startup (configurable)
		Made folder scanning up to 10x faster
		Made Media Library browsing faster
		Gave folder scanning a lower priority than other UMS functions to make them work more smoothly
		Fixed some bugs related to sending media metadata (org_pn) to renderers
		Fixed bug where removing a directory in UMS does not remove its contents from the Media Library.
		Fixed sorting in dynamic folders
		Reduced lookups and bandwidth to OpenSubtitles
		Fixed MusicBrainz bugs
		Fixed many other bugs
	Languages:
		Synchronized translations with Crowdin
	Renderers:
		Added Samsung 8 Series config
		Updated FetchTV config
		Improved support for Samsung Galaxy S7

7.0.0 - 2018-03-27 - Changes since 7.0.0-rc2

	General:
		Made folder scanning up to 10x faster
		Made Media Library browsing faster
		Gave folder scanning a lower priority than other UMS functions to make it work more smoothly
		Added configuration option to toggle shared folder scanning on startup
		Fixed "By Date" virtual folders on joined folders
		Fixed a database initialization bug
		Fixed order of "By date" folders, now they go from newest to oldest
		Fixed some bugs related to sending media metadata (org_pn) to renderers
	Languages:
		Synchronized translations with Crowdin
	Renderers:
		Added Samsung 8 Series config
		Updated FetchTV config
		Improved support for Samsung Galaxy S7

7.0.0-rc2 - 2018-01-04 - Changes since 7.0.0-rc1

	General:
		Added "Watched" folders to the Media Library, to mirror "Unwatched" functionality
		Fixed crashing when shared folders don't exist, aren't folders, or access is denied
		Fixed broken Media Library entries
		New Media folder is hidden by default (the same functionality exists in Media Library)
		All changes in 6.7.3, 6.7.4 and 6.8.0
	Dependencies:
		Updated h2database to 1.4.196

6.8.0 - 2018-01-04

	General:
		Added shared folder selection to the wizard
		Improved matching of results from CoverArtArchive
		Improved text in the wizard
		Fixed detection of Java Runtime Environment 9 on Windows
		Fixed charset handling during subtitle conversions
		Fixed video not always transcoding when encoded audio passthrough is used
		Fixed an exception if the renderer replied to GetProtocolInfo but didn't include sink
		Formatting in UMS.conf
		Various bug fixes
	Languages:
		Added Bengali translation (empty for now, please contribute to it on Crowdin!)
		Improved support for Japanese characters
	Renderers:
		Added 4k support to VLC for iOS
		Added support for sending 4k MP4 videos to PS4 natively

6.7.4 - 2017-10-03

	General:
		Fixed a bug where thumbnails wouldn't always be generated when MediaInfo isn't used or available
		Fixed regression in folder thumbnails on non-Windows platforms
		Fixed a crash when the web interface's port is occupied, and do not fail restarting the server, if the server hasn't created yet (thanks, Zsombor Gegesy!)
		Fixed many general bugs
	Renderers:
		Added support for HE-AAC audio in MKV and MP4 files on LG BP550
		Improved detection and file support for LG WebOS TVs

6.7.3 - 2017-08-13

	General:
		Improved support for AVI and MJPEG
		Improved general DLNA implementation
		Fixed duplicate entries in transcode folders
		Fixed logging in macOS
		Fixed menu name in macOS
	Renderers:
		Added renderer configuration for conversion of 3D video to 2D
		Added support for Fetch TV
		Added support for Onkyo TXNR8xx
		Improved support for Cambridge Audio Azur BD
		Improved support for OPPO BDP
		Improved support for Panasonic VIERA TXL32V10E
		Improved support for Sony BRAVIA 5500 and EX TVs
		Improved support for Sony SMP-N100
		Improved support for Sony STR-DA5800ES
		Fixed album art for Onkyo receivers (and probably others)
		Fixed support for Xbox 360
	Dependencies:
		Updated MediaInfo to 0.7.97, which:
			Improves support for HEVC (H.265), FLV, MKV, TIFF and more

7.0.0-rc1 - 2017-08-04 - Changes since 7.0.0-b2

	General:
		Increased speed of Media Library features
		Fixed sorting of TV seasons
		Fixed false lookup matches of titles
		Fixed realtime file adding in new folders
		Fixed forgetting about a file after moving it
		Fixed many other bugs
		Reduced lookups and bandwidth to OpenSubtitles
		Fixed MusicBrainz bugs
		All changes in 6.7.0, 6.7.1 and 6.7.2
		Unreleased changes from v6:
			Improved support for AVI and MJPEG
			Improved general DLNA implementation
			Fixed duplicate entries in transcode folders
			Fixed logging in macOS
			Fixed menu name in macOS
	Renderers:
		Unreleased changes from v6:
			Added renderer configuration for conversion of 3D video to 2D
			Added support for Onkyo TXNR8xx
			Improved support for Cambridge Audio Azur BD
			Improved support for OPPO BDP
			Improved support for Panasonic VIERA TXL32V10E
			Improved support for Sony BRAVIA 5500 and EX TVs
			Improved support for Sony SMP-N100
			Improved support for Sony STR-DA5800ES
			Fixed album art for Onkyo receivers (and probably others)
			Fixed support for Xbox 360
	Dependencies:
		Updated h2database to 1.4.195
		Unreleased changes from v6:
			Updated MediaInfo to 0.7.97, which:
				Improves support for HEVC (H.265), FLV, MKV, TIFF and more

6.7.2 - 2017-07-09

	General:
		Added support for WMA10
		Improved splash screen timing and enabled it by default
		Improved support for MediaInfo on non-Windows platforms
		Improved CPU and memory logging
		Improved detection of network speed
		Updated comments in DefaultRenderer.conf
		Updated documentation
		Fixed trace logging on macOS
		Fixed a crash on Windows 10
	Languages:
		Synchronized translations with Crowdin
		Fixed Hebrew language support
	Dependencies:
		Updated FFmpeg
		Updated JNA to 4.4.0
		Updated Surefire to 2.20

6.7.1 - 2017-06-11

	General:
		Fixed playback on Linux
		Fixed support for OGA and 3GA files
	Dependencies:
		Updated MediaInfo to 0.7.96

6.7.0 - 2017-06-10

	General:
		Added a button for the web interface
		Expanded support for preventing sleep mode on Windows and macOS
		Improved support for MusicBrainz
		Improved support for CoverArtArchive
		Improved support for images
		Improved thumbnailing
		Updated documentation
		Improved DIDL-Lite implementation
		Added support for more RealAudio formats
		Fixed DVD support
		Fixed many bugs
	Languages:
		Synchronized translations with Crowdin
	Renderers:
		Added support for Chromecast Ultra
		Added support for MXPlayer on Google Android
		Added support for Microsoft Windows Media Player
		Improved support for Chromecast
		Improved support for Samsung 9 Series TVs
		Improved support for Sony BRAVIA EX TVs
		Improved detection of Onkyo TXNR7xx devices
		Improved detection of Panasonic Viera ST60 TVs
		Improved detection of Samsung J6400 TVs
		Improved detection of Samsung JU6400 TVs
		Improved detection of Samsung Galaxy S5 phones
		Improved detection of Sony BRAVIA XE TVs
	Dependencies:
		Updated external-maven-plugin to 0.2
		Updated FFmpeg to latest builds
		Upgraded LogBack to version 1.2.3
		Updated maven-surefire-plugin to 2.20
		Updated MediaInfo to 0.7.94
		Updated metadata-extractor to 2.10.1
		Updated TwelveMonkeys ImageIO to version 3.3.1

7.0.0-b2 - 2017-04-02

	General:
		Fixed bugs with the fully played feature
	Languages:
		Synchronized translations with Crowdin

7.0.0-b1 - 2017-03-26 - Changes since 7.0.0-a1

	General:
		Added "Unwatched" folder to the videos folder if "Hide fully played media" is not enabled
		File addition/removal is handled specifically instead of doing a library scan
		Optimized database storage
		Reduced memory use
		Fixed the setting to hide fully played media in the Media Library
		Fixed sorting in Media Library virtual folders
		Fixed thread-safety/concurrency bugs
		All changes in 6.5.3 and 6.6.0
		Improved logging
	Languages:
		Synchronized translations with Crowdin
	Renderers:
		Improved support for Samsung 9 series TVs
		Improved detection of Panasonic ST60 TVs
		Improved detection of Samsung JU6400 TVs
	Dependencies:
		Updated external-maven-plugin to 0.2
		Updated FFmpeg to the latest builds

6.6.0 - 2017-02-25

	General:
		Added support for AIFF files
		Improved detection of the language of subtitles
		Debugging and logging improvements
		Fixed forcing the usage of discrete GPUs on Apple computers with dual GPUs
		Fixed minor bugs
	Languages:
		Added Thai translation
		Synchronized translations with Crowdin
	Renderers:
		Added support for Panasonic DMR Blu-ray recorders
		Added support for Pioneer Blu-ray players
		Added support for Sony BRAVIA XD 70/75 series TVs
		Added support for Yamaha RX-A2050
		Enabled UPnP control support on Panasonic TVs
		Improved support for Google Chromecast
		Improved support for LG WebOS TVs
		Improved support for Panasonic Viera VT60 TVs
		Improved support for Philips PFL TVs
		Improved support for Sony BRAVIA KDL-NX800 series TVs
		Improved support for Sony BRAVIA XD 70/75/80/83/85/93/94 series TVs
		Fixed image stretching on Sony BRAVIA 5500 series TVs
	Dependencies:
		Updated Apache commons-lang to 3.5
		Updated ChromeCast Java API to 0.9.2
		Updated dcraw to 9.27
		Updated ICU4J to 58.2
		Updated logback to 1.1.8
		Updated MediaInfo to 0.7.91

6.5.3 - 2017-01-07

	General:
		Added detection of the Chromium and Vivaldi web browsers
		Fixed bug where video container and codec settings weren't saved
	Languages:
		Synchronized translations with Crowdin
	Renderers:
		Added support for Sony Bravia X series TVs
	Dependencies:
		Updated Git Commit ID plugin to 2.2.1
		Updated Maven Antrun plugin to 1.8
		Updated Maven Assembly plugin to 3.0.0
		Updated Maven Compiler plugin to 3.6.0
		Updated Maven Enforcer plugin to 1.4.1
		Updated Maven Findbugs plugin to 3.0.4
		Updated Maven Javadoc plugin to 2.10.4
		Updated Maven PMD plugin to 3.7
		Updated Maven Project Info Reports plugin to 2.9
		Updated Maven Site plugin to 3.6
		Updated Maven Surefire plugin to 2.19.1
		Updated Maven Surefire Report plugin to 2.19.1

7.0.0-a1 - 2016-12-31

	General:
		Added "TV Shows", "Movies", "3D Movies", and "Unsorted" folders to the "Media Library" folder
		Added right-click menu to navigation pane
		Renamed "Cache" folder to "Media Library" folder and enabled it by default
		Changed "Hide" options to "Show", e.g. "Hide Media Library folder" becomes "Show Media Library folder"
		Library is re-scanned whenever a file is added/changed/removed in a shared folder
		Fixed bug where removing a directory in UMS does not remove its contents from the Media Library.
		Fixed sorting in dynamic folders
		Fully played tracking is stored in the SQL database

6.5.2 - 2016-11-27

	General:
		Respect the renderer setting H264Level41Limited when deciding whether to stream or transcode
		Search for subtitles in alternative folder even when a subtitle was already found (thanks, tdcosta100!)
		Logging improvements
		Improved program shutdown stability
		Updated docs
		Updated image parsing from MediaInfo and Sanselan to Imaging
		Fixed manual renderer selection
		Improved support for OGA, MKA, ADTS, WEBM, 3GA and 3G2A files
		Made disabling transcoding more reliable
		Minor bugfixes
	Languages:
		Minor English updates
		Synchronized translations with Crowdin
	Renderers:
		Added FLAC support on Sony PS4
		Improved AAC support on LG LM620 TVs
		Improved detection of Sony Bravia W series TVs
		Improved AVI/DivX support on Panasonic TX-L32V10E TVs
		Improved Google Chromecast support
		Fixed LPCM audio on Sony PS3
		Fixed aspect ratios on Sony Bravia 5500 series TVs
	Dependencies:
		Updated h2database to 1.4.193

6.5.1 - 2016-10-02

	General:
		Added Dolby Atmos detection
		Added renderer config setting RemoveTagsFromSRTSubtitles
		Config file fixes and improvements
		Reduced CPU usage
		Improved adherence to DLNA standards
		Restart button changes to red when a restart is required
		Fixed audio channel parsing
		Fixed audio pitch when transcoding some files
	Languages:
		Synchronized translations with Crowdin
	Renderers:
		Added support for VLC for desktop
		Improved seeking support on AnyCast
		Improved support for some filetypes on VLC for iOS
		Improved support for Panasonic E6 TVs
		Improved support for AVI files on XBMC
	Dependencies:
		Updated jaudiotagger to 2.2.5
		Updated MediaInfo to 0.7.88
		Updated NSIS to 2.5.1

6.5.0 - 2016-08-01

	General:
		Use ellipses for overflowing text in the left menu on the web interface
		Improved speed of parsing files
		Improved documentation in DefaultRenderer.conf, UMS.conf and the code
		Fixed support for semicolons in paths in FFmpeg
		Fixed the cancellation of media library scans
		Fixed videos being transcoded too often because of bitrate halving
		Fixed support for video files within compressed folders
		Fixed the MIME type for WAV DTS files
	Renderers:
		Added support for LG Blu-ray players
		Added support for Naim Mu-So wireless audio systems
		Improved detection of LG TVs
		Improved support for AC-3 audio on VLC on iOS and Apple TVs
		Improved support for XviD codec on Panasonic TVs and VLC on iOS and Apple TVs
		Improved support for images on Panasonic TXL32V10E TVs
		Fixed support for virtual folders like New Media and Cache on Panasonic TVs
		Fixed support for WebVTT subtitles on Samsung TVs
	Languages:
		Synchronized translations with Crowdin
		Danish translation was completed and validated (thanks, jensen83, nba, squadjot, The_lonely_Glowstone and the_slayer_dk!)
		Portuguese translation was completed (thanks, arqmatiasreis, El_Locco, Nadahar and plucas!)
	External Components:
		Updated assertj to 2.5.0
		Updated ChromeCast Java API to 0.9.2
		Updated Cling to 2.1.1
		Updated commons-io to 2.5
		Updated doxia plugin to 1.7
		Updated exec maven plugin to 1.5.0
		Updated gson to 2.7
		Updated logback to 1.1.7
		Updated maven antrun plugin to 1.8
		Updated maven compiler plugin to 3.5.1
		Updated maven enforcer plugin to 1.4.1
		Updated maven site plugin to 3.5.1
		Updated maven source plugin to 3.0.1
		Updated MediaInfo to 0.7.87
		Updated Netty to 3.10.6

6.4.0 - 2016-06-26

	General:
		Regularly initiate UPnP searches for new renderers
		Added renderer config settings HalveBitrate and SupportedVideoBitDepths
		Prevent sleep mode while streaming by default
		Tweaked language
		Improved/fixed some documentation (thanks, Sami32!)
		Improved file parsing speed
		Improved logging
		FFmpeg no longer defers to MEncoder for embedded fonts since it supports them
		FFmpeg no longer defers to MEncoder for internal ASS subtitles
		ALIVE messages send less frequently by default
		Fixed support for CMYK JPEG images
		Fixed 24-bit FLAC fake videos showing up for non-PS3 renderers
		Fixed unsupported subtitles being streamed
		Fixed transcoding bitrate in rare cases
		Fixed MIME types for WAV audio and TIFF images
		Fixed renderer TextWrap
		Fixed renderers only being detected if they are started before UMS
	Renderers:
		Added support for VLC on Apple TV
		Improved video quality on wireless networks for Panasonic VT60
		Improved support for subtitles on Panasonic CX700 Series TVs
		Improved support for Panasonic CX680 Series TVs
		Improved support for Panasonic E6 Series TVs
		Improved support for Sony Bravia NX800 TVs (thanks, prescott66!)
		Improved detection of Vizio TVs
	Languages:
		Czech translation was completed (thanks, jirkapas and panther7!)
		French translation was completed and validated (thanks, archaos, Kielo, Kirvx, misterfred and Nadar!)
		Italian translation was completed (thanks, alebrambilla1986, av3c01, bonatigennaro7, FoxGhost07, fsc_mar, johnjonh, jumputer, morag87, RickyReds, supp and vladiesel!)
		Polish translation was completed and validated (thanks, anonymodmous, K4r0lSz, Nadar and robo25!)
		Russian translation was completed (thanks, antonyfg, dronidzer, guzu-guzu, lxnderty, Nadar and Tianuchka!)
		Swedish translation was completed (thanks, klebom, linushg111, mattias_karlsson_89, millenniumb, Nadar, qwert352, rchk, richarda, Rowly, sebastianboos, Stetoskop, strayhat and swarish!)
		Turkish translation was completed and validated (thanks, hasanbahcekapili, OnarEngincan and onuroztemizel!)
	External Components:
		Updated FFmpeg to builds from this month
		Updated FLAC to 1.3.1 (thanks, Sami32!)
		Updated h2database to 1.4.192
		Updated MediaInfo to 0.7.86

6.3.2 - 2016-05-27

	General:
		Added user config setting ALIVE_delay
		Fixed calculating network speeds with offline renderers
		Fixed detection of the MPEG-1 video codec
	Renderers:
		Fixed connectivity with Sony renderers
	Languages:
		Synchronized translations with crowdin

6.3.1.1 - 2016-05-22

	General:
		Fixed connectivity issues

6.3.1 - 2016-05-20

	General:
		Improved connectivity with devices
		Improved support for 3D subtitles
		Fixed support for PAL DVDs
	Renderers:
		Improved support for LG EG910V TVs (thanks, ZakarFin!)
		Improved support for Panasonic Viera E6 Series TVs (thanks, Sami32!)
		Improved detection of Windows Media Player (thanks, WolfganP!)
		Fixed support for WMV 8 and below on Panasonic TVs
	Languages:
		Synchronized translations with crowdin
	External Components:
		Updated Cobertura Maven Plugin to 2.7
		Updated Maven PMD Plugin to 3.6
		Updated Maven Source Plugin to 3.0.0
		Updated slf4j to 1.7.21

6.3.0 - 2016-05-07

	General:
		Added renderer config setting VideoFormatsSupportingStreamedExternalSubtitles
		Added renderer config setting StreamSubsForTranscodedVideo
		Ignore the article "a" by default, along with "the"
		Improved support for 3D subtitles
		Improved support for streaming external subtitles
		Defer to FFmpeg for transcoding VobSub subtitles
		Fixed header animation delay on web interface
	Renderers:
		Added support for Yamaha RXV500D (thanks, newbietux!)
		Improved support for Miracast M806
		Improved support for Panasonic Viera GT50 TVs (thanks, pcjco!)
		Improved support for Panasonic Viera TXL32V10E TVs
	Languages:
		Synchronized translations with crowdin
	External Components:
		Updated FFmpeg
		Updated MediaInfo to 0.7.85
		Updated MPlayer/MEncoder for Windows to SB65

6.2.2 - 2016-04-09

	General:
		Improved support for subtitles in FFmpeg
		Improved support for custom fonts
		Improved general performance
		Added renderer config setting KeepAspectRatioTranscoding
		Fixed FFmpeg not using the TranscodedVideoAudioSampleRate renderer config setting
		Fixed detection of 3D videos
		Fixed filenames starting with "The" being listed first
		Fixed link to musicbrainz (thanks, Utano!)
		Fixed automatic subtitles language detection overruling manual detection
		Put the database in the profile directory on non-Windows operating systems
	Renderers:
		Improved support for Panasonic TVs
		Improved support for Sony TVs
	Languages:
		Synchronized translations with crowdin
	External Components:
		Updated icu4j to 57.1
		Updated MediaInfo to 0.7.84

6.2.1 - 2016-03-28

	General:
		Added more tooltips
		Fixed DTS-HD being detected as DTS
		Fixed support for embedded ASS subtitles in FFmpeg
		Fixed minor bugs
	Renderers:
		Improved support for Panasonic ST60 TVs
		Improved support for Samsung J6200 TVs
	Languages:
		Synchronized translations with crowdin
	External Components:
		Updated chromecast-java-api-v2 to 0.9.1
		Updated FFmpeg
		Updated Google gson to 2.6.2
		Updated jai-imageio-core to 1.3.1
		Updated jmustache to 1.12
		Updated JNA to 4.2.2
		Updated logback to 1.1.6
		Updated slf4j to 1.7.19

6.2.0 - 2016-03-12

	General:
		Use image instead of text in thumbnail overlays
		Improved the web interface
		Added support for using Open Subtitles credentials
		Improved filename prettifying
		Fixed bump support on mobile devices
	Renderers:
		Improved Panasonic VT60 support
		Improved Samsung J55xx support
		Fixed duplicate subtitles on Samsung TVs
	Languages:
		Synchronized translations with crowdin
	External Components:
		Updated Apache Commons Collections to 3.2.2
		Updated l10n Maven plugin to 1.4
		Updated logback to 1.1.5
		Updated Maven Project Info Reports plugin to 2.8.1
		Updated MediaInfo to 0.7.83
		Updated slf4j to 1.7.16

6.1.0 - 2016-02-20

	General:
		Added a back link to the web interface
		Improved logging
		Squashed lots of bugs
		Stopped systems from using installed versions of JNA
		Thumbnails are unpadded by default
		Improved speed of parsing subtitles
		Improved filename prettifying speed and stability
		Filename prettifying uses more information from Open Subtitles
		Fixed FFmpeg not using custom fonts (thanks, Tiago!)
		Fixed seeking with FFmpeg when embedded picture-based subtitles
		Use FFmpeg by default to transcode embedded picture-based subtitles
		Stop MediaInfo from using the network
	Renderers:
		Added ThumbnailPadding option
		Improved Panasonic VT60 support
		Improved Panasonic CX700 support
		Improved Samsung PL51E490 support
	Languages:
		Synchronized translations with crowdin
	External Components:
		Updated FFmpeg
		Updated MediaInfo to 0.7.82

6.0.0 - 2016-01-30 - Changes since 5.5.0

	General:
		Added "Fully played action" setting to control what UMS does with media that has been fully played. The options are:
			Do nothing
			Add an overlay to the thumbnail (default)
			Hide the file
			Move the file to a different folder
			Move the file to the recycle/trash bin
		Added "Thumbnails" renderer config option, to specify whether the renderer can display thumbnails
		The Status tab updates when media is fast-forwarded or rewound
		Audio and image thumbnails are output at consistent dimensions
		Added SquareAudioThumbnails and SquareImageThumbnails renderer configuration options
		Improved support for thumbnail generation
		Improved detection of the position of media while playing
		Improved logging
		Improved headless support
		Fixed font size of subtitles
		Improved filename prettifying
		Fixed transcoding to LPCM
		Added and updated tooltips
		Stability improvements
		Fixed and improved automatic cover downloading
		Fixed OS X version
	Renderers:
		Improved Status tab display for Panasonic TVs
		Improved VLC for iOS support
		Improved Panasonic VT60 support
	Languages:
		Synchronized translations with crowdin
	External Components:
		Updated Google gson to 2.5
		Updated h2database to 1.4.191
		Updated MediaInfo to 0.7.81

6.0.0 - 2016-01-30 - Changes since 6.0.0-b2

	General:
		Improved headless support
		Fixed font size of subtitles
		Improved performance
		Stability improvements
		Improved filename prettifying
		Fixed transcoding to LPCM
		Added and updated tooltips
		Fixed and improved automatic cover downloading
		Fixed OS X version
	Renderers:
		Improved VLC for iOS support
		Improved Panasonic VT60 support
	Languages:
		Synchronized translations with crowdin
	External Components:
		Updated Google gson to 2.5
		Updated h2database to 1.4.191
		Updated MediaInfo to 0.7.81

5.5.0 and 6.0.0-b2 - 2016-01-06

	General:
		Language selection is offered on first startup
		Improved display on Retina screens
		Improved language selection interface
		Improved conversion of 3D subtitles
		Fixed positioning of subtitles in FFmpeg
		Fixed tooltip consistency
		Fixed detection of file permissions on Windows
		Fixed detection of headless state
		Fixed many minor bugs
	Renderers:
		Updated images for Android, PS3 and PS4
		Fixed AirPlayer support
		Fixed VLC for iOS support
	Languages:
		Added Afrikaans, Hungarian, Persian (Farsi), Serbian, Slovak, Ukrainian and Vietnamese
		Allow the translation of language names
		Synchronized translations with crowdin
	External Components:
		Updated Maven exec plugin to 1.4.0
		Rolled back JNA to 4.1.0, which fixes a playback error

6.0.0-b1 - 2015-12-29

	General:
		Added SquareAudioThumbnails and SquareImageThumbnails renderer configuration options
		Improved support for thumbnail generation
		Improved detection of the position of media while playing
		Improved logging
		All changes from 5.4.0

5.4.0 - 2015-12-29

	General:
		Added splash screen, disabled by default
		Improved support for subtitles in FFmpeg
		Improved headless support on Windows
		Improved support for unrecognized filetypes
		Improved speed and accuracy of the filename prettifying feature
		Fixed support for 3D subtitles
		Fixed the saving of window size
	Renderers:
		Added support for AnyCast
		Added support for Miracast M806
		Added support for Sony Bravia NX800 TVs
		Improved playback support for several renderers
	External Components:
		Updated Cling to 2.1.0
		Updated FFmpeg
		Updated MediaInfo to 0.7.80

6.0.0-a1 - 2015-11-23 - Changes since 5.3.1

	General:
		Added "Fully played action" setting to control what UMS does with media that has been fully played. The options are:
			Do nothing
			Add an overlay to the thumbnail (default)
			Hide the file
			Move the file to a different folder
			Move the file to the recycle/trash bin
		Added "Thumbnails" renderer config option, to specify whether the renderer can display thumbnails
		The Status tab updates when media is fast-forwarded or rewound
		Audio and image thumbnails are output at consistent dimensions
	Renderers:
		Improved Status tab display for Panasonic TVs
	Languages:
		Synchronized translations with Crowdin

5.3.1 - 2015-11-23

	General:
		Window state is saved when maximized
		Improved font-scaling on high-DPI displays
		Improved file permissions checks
		Improved filename prettifying
		Fixed Linux not finding FFmpeg (thanks, Oxalin!)
	Renderers:
		Added detection of the Edge browser
		Fixed support for subtitles on Samsung J series TVs
	Languages:
		Synchronized translations with Crowdin
	External Components:
		Updated icu4j version to 56.1
		Updated JGoodies Looks to 2.7.0
		Updated JNA to 4.2.1

5.3.0 - 2015-10-28

	General:
		The main window saves its size and position
		Fixed DTS-HD being detected as regular DTS
		Fixed the use of UMS profiles
		Made automatic updater more stable
		Improved detection of write permissions
		Improved plugin installation support
		Made profile a possible command line argument
		Improved speed when reading shared folders
		Fixed several bugs
	Renderers:
		Disabled folder thumbnails on Apple iOS apps
		Improved support for VLC for iOS
		Panasonic TVs use higher-quality thumbnails
	External Components:
		Updated FFmpeg for Windows to 7c8fcbb, which increases transcoding quality and speed
		Updated h2database to 1.4.190
		Updated Maven Assembly plugin to 2.6
		Updated Maven Eclipse plugin to 2.10
		Updated Maven Surefire plugin to 2.19
		Updated MediaInfo for Windows to 0.7.78, which improved file parsing speed
		Updated MPlayer/MEncoder for Windows to SB64, which increases transcoding quality and speed
		Updated Netty to 3.10.5

5.2.3 - 2015-09-28

	General:
		Improved logging and logging options
		Improved language translations
		Improved Windows 10 support
		Improved playback stability
		Changed default thumbnail seeking position from 2 seconds to 4 seconds
		Minor status tab updates
		Fixed the web interface Flash player
		Fixed many bugs
	Renderers:
		Improved detection of AirPlayer
		Improved folder population time on Panasonic TVs
		Improved support for Samsung TVs
		Improved support for Vizio TVs
	Languages:
		Improved accuracy of automatic subtitles language detection
		Updated many languages based on contributions from Crowdin
	External Components:
		Updated AssertJ to 2.2.0
		Updated h2database to 1.4.189
		Updated FFmpeg for Windows
		Updated Javassist to 3.20.0-GA
		Updated jmustache to 1.10
		Updated JNA to 4.2.0
		Updated maven-antrun-plugin to 1.7
		Updated Plexus-utils to 3.0.22
		Updated Seamless to 1.1.1

5.2.2 - 2015-08-13
	General:
		Fixed audio transcoding with embedded images (thanks, Nadahar!)
		Improved MIME type handling
		Improved logging
		Expanded filename prettifying
		Improved renderer selection interface (thanks, Nadahar!)
		Improved file support (thanks, Nadahar!)
		Speed improvements
	Renderers:
		Improved support for Xbox 360
	Languages:
		Updated all languages to remove unused translations (thanks, ler0y and Nadahar!)
		Updated Dutch translation (thanks, ler0y!)
		Updated English (thanks, ler0y!)
		Updated Polish translation (thanks, Karol Szastok!)
	External Components:
		Updated FFmpeg for Windows
		Updated MediaInfo to 0.7.75
		Updated Netty to 3.10.4

5.2.1 - 2015-07-11

	General:
		Improved efficiency of configuration file loading (thanks, Nadahar!)
		Improved support for quotation marks and apostrophes on some renderers (thanks, Nadahar!)
		Fixed broken 32-bit MediaInfo dependency
	Renderers:
		Fixed Xbox One thumbnails
	Languages:
		Updated Spanish translation (thanks, AlfredoRamos!)

5.2.0 - 2015-07-09

	General:
		Made tsMuxeR use FFmpeg instead of MEncoder
		Made tsMuxeR transcode audio to AAC if the renderer expects AAC
		Improved FFmpeg responsiveness during fast-forward and rewind
		Improved automatic rescaling
		Fixed detection of aspect ratios
		Merged the install/uninstall Windows Service buttons into one button (thanks, taconaut!)
		Add zoom/fit to web image viewer
		Fixed a bug with sending external subtitles
		Fixed MIME type finalization when parsing media
		Fixed transcoding to AAC via MEncoder
		Support late resolution of media length
		Improved the accuracy of video metadata
		Added ThumbnailWidth and ThumbnailHeight renderer settings
		Added support for file extensions: AC3, AMR and TTA (thanks, ler0y!)
		Fixed custom device configuration loading
		Added more logging
		Fixed folders not showing if they were previously empty, with "hide empty folders" enabled (thanks, jensaarai!)
		Cleaned up the titles in the transcode folder in some cases
		Various minor improvements/fixes
		Expanded code testing for improved stability
	Renderers:
		Added support for Sony PlayStation 4
		Improved thumbnail display on Panasonic TVs
		Improved thumbnail display on Microsoft Xbox One
	Languages:
		Updated English translation (thanks, Nadahar!)
		Updated Norwegian translation (thanks, Nadahar!)
	External Components:
		Rolled back FFmpeg on OS X to fix transcoding for some users
		Updated Netty to 4.0.29

5.1.4 - 2015-06-14

	General:
		Limits clickable areas of settings more precisely
		Uses a default renderer image when none exist
		Made H.264 transcoding faster
		Improved stability and speed with large amounts of images
		Expanded code testing coverage (thanks, Jensaarai!)
		Improved detection of languages
		Improved filename prettifying
		Code optimizations
	Renderers:
		Improved support for DTS audio on Panasonic VT60 TVs
	Languages:
		Updated French translation (thanks, Kirvx!)
	External Components:
		Updated FFmpeg to builds from 20150521, which:
			Improved support for many containers and codecs
			Fixed bugs
		Updated InterFrame to 2.8.2, which improved speed
		Updated Maven Assembly Plugin to 2.5.4
		Updated Maven Compiler Plugin to 3.3
		Updated Maven Git Commit ID Plugin to 2.1.15
		Updated Maven Javadoc Plugin to 2.10.3
		Updated Maven Surefire Plugin to 2.18.1
		Updated MediaInfo to 0.7.74, which improved detection of file information
		Updated Netty to 4.0.28, which improved network operations (thanks, mfulgo!)

5.1.3 - 2015-05-05

	General:
		Added and improved documentation in UMS.conf and DefaultRenderer.conf
		Added the URL for the web interface to the logs on the Logs tab
		Improved detection of the H.263 codec (thanks, leroy!)
		Improved stability when transcoding subtitles by default
		Simplified MEncoder commands in some situations
		Fixed bugs
	Renderers:
		Improves support for Samsung EH5300 TVs (thanks, panzer!)
		Fixed initial folders bug on Xbox 360/One
		Improved detection of Xbox One
		Improved detection of several Samsung TVs
	Languages:
		Updated Dutch translation (thanks, leroy!)
		Updated Spanish translation (thanks, AlfredoRamos!)

5.1.2 - 2015-04-15

	General:
		Added "upnp_enable" user-level option
		Added "log_level" user-level option
		Added "UpnpAllow" renderer-level option
		Added "Create TRACE logs" button
		Added the ability to restart the program (not just the server)
		Fixed videos being muxed instead of streamed
		Various minor fixes/improvements
	Renderers:
		Added DSD/DFF streaming support to Cambridge Audio Blu-ray Disc players
		Added more tags to DefaultRenderer.conf
		Improved renderer detection/handling
		Improved Android device detection
		Improved detection of Panasonic AS600 Series TVs
		Improved PS3 muxing via tsMuxeR
		Improved support for Samsung D6400 TVs
		Improved support for Samsung EH5300 TVs
		Improved support for transcoding to Technisat S1+
		Fixed support for MP3 files on some Samsung TVs and Blu-ray Disc players
	External Components:
		Updated h2database to 1.4.187
		Updated JDom to 2.0.6
		Updated Logback to 1.1.3
		Updated Maven AntRun Plugin to 1.8
		Updated Maven Findbugs Plugin to 3.0.1
		Updated Maven Git Commit ID Plugin to 2.1.13
		Updated Maven Javadoc Plugin to 2.10.2
		Updated Maven PMD Plugin to 3.4
		Updated MediaInfo to 0.7.73, which:
			Added and improved support for many formats
			Fixed bugs
		Updated slf4j to 1.7.12

5.1.1 - 2015-04-03

	General:
		Added support for links in tooltips (thanks, taconaut!)
		Added support for customizing background and foreground color in tooltips (thanks, taconaut!)
		Added PrependTrackNumbers renderer option to ensure that renderers order audio by track number (thanks, javand!)
		Made the RescaleByRenderer setting more consistent
		Fixed MP4, M4A and 3GP file compatibility on some renderers
		Fixed bug with the renderer selection window
		Fixed audio and image compatibility on some renderers
		Fixed transcoding with subtitles on 64-bit systems
	Renderers:
		Made Samsung televisions and Blu-ray Disc players order audio by track number

5.1.0 - 2015-03-21

	General:
		Added 64-bit versions of FFmpeg for a ~10% increase in transcoding speed
		Removed the "Save" button since changes to settings are now saved automatically
		Improved detection of playback states
		Improved connection awareness
		Improved True Motion frame interpolation speed by up to 20%
		Improved folder population speed
		Improved support for 3GPP files
		Improved descriptions of settings
		Slightly altered the memory usage bar
		Fixed the renderer SeekByTime setting
		Fixed renderer detection and recognition in some cases
		Fixed display names for videos that have the Track (song title) value set
		Fixed incorrect ContentFeatures headers being sent to renderers (thanks, master-nevi!)
		Fixed the web logviewer
		Fixed bugs with 3D subtitles
		Fixed the setting for using embedded subtitles styles
	Renderers:
		Fixed filename bug on Sony Bravia TVs
		Improved support for LG LED-backlit LCD 2014 TVs
		Improved support for Samsung H6203 TVs
	Languages:
		Made more strings translatable
		Updated Czech translation
		Updated Dutch translation (thanks, ler0y!)
		Updated French translation (thanks, Kirvx!)
		Updated Italian translation (thanks, bartsimp!)
	External Components:
		Updated Chromecast Java API to 0.0.6
		Updated Commons Codec to 1.10
		Updated FEST Util to 1.2.5
		Updated FFmpeg for Windows and OS X, which:
			Improved support for many containers and codecs
			Fixed bugs
		Updated Gson to 2.3.1
		Updated h2database to 1.4.186, which:
			Fixed memory issues
			Fixed caching issues
		Updated InterFrame to 2.8.0, which:
			Increased processing speed by up to 20%
			Reduced memory use
			Reduced dependencies
		Updated JDom to 2.0.2
		Updated JUnit to 4.12
		Updated Plexus Utils to 3.0.21
		Updated slf4j to 1.7.10

5.0.1 - 2015-02-16

	General:
		Fixed text in the Windows automatic updater
		Fixed MediaInfo not parsing some rare data
		Fixed detection of font attachments
		Fixed support for subtitled files with apostrophes in the name
		Fixed errors when packing debug files
		Fixed support for resuming playback on some devices
		Improved support for MOV, 3GP and 3G2 files
		Reduced CPU load when the Status tab is visible
		Improved support for 1920x1088 videos
		Fixed logging error
		Fixed the web interface when browsing via Safari
		Improved stability when disabling renderers
	Renderers:
		Added support for Kodi Media Center
		Added support for more Panasonic Blu-ray players
		Added support for Samsung HT-F4 series home entertainment systems
		Improved detection of some LG TVs
		Improved detection of some Panasonic Blu-ray players and TVs
		Improved detection of some Samsung devices
		Improved support for Samsung ES6100 TVs
		Improved support for Samsung ES6575 TVs
		Improved support for Sony Xperia Z3 smart phones
		Updated the image for Xbox 360
		Fixed 24-bit FLAC playback on PS3 via the Videos folder
	Languages:
		Updated Czech translation
		Updated French translation (thanks, Kirvx!)
		Updated Spanish translation (thanks, AlfredoRamos!)
	External Components:
		Updated MediaInfo for OS X to 0.7.71, which:
			Improved folder population time
			Added and improved support for many formats
			Fixed bugs

5.0.0 - 2015-01-25 - Changes since 4.4.0

	General:
		Added support for UPnP connections and playback
		Added the option to use info from IMDB with filename prettifying
		Added HTML5 video support to the web interface
		Create custom per-device configuration files to override any general renderer or UMS setting
		Major redesign of the status tab to show per-renderer information
		Minor tweaks to the GUI
		Improved player control
		Includes the possibility to allow UMS to control renders automatically
		Web player can also be controlled
		Automatic reloading of external files
		Documentation updates
		New xmb playlist folders with optional automatic starting, looping, and saving, editable from web or xmb
		A dynamic xmb playlist for on-the-fly playlist creation from web or xmb
		Better bump/gui player playlists with optional automatic looping
		Push playback support for chromecast and web browsers
		Changed the interface for enabling/disabling renderer configurations
		Changed the default setting for wrap_dts_into_pcm to false
		Made sure files are always directly streamable via the Transcoding folder
		Added the UMS_MAX_MEMORY variable to UMS.sh
		Fixed audio downsampling
		Fixed a bug with parsing formats from MediaInfo
		Removed the database compacting method that was never recommended
		Fixed a startup error
		Minor bugfixes and code optimizations
	Renderers:
		Added support for Panasonic DMP-BDT360 devices
		Added support for Samsung H6500 Blu-ray Disc players
		Added support for Telefunken TVs
		Improved support for Chromecast (cast from UMS webui/gui)
		Improved support for Panasonic Viera AS650 series TVs
		Improved support for Samsung C6600 series TVs
		Improved support for Sony Blu-ray Disc players
		Improved support for Sony Bravia W series TVs
		Improved file compatibility on non-PS3 renderers
		Fixed detection of some Samsung renderers
	Languages:
		Updated Czech translation
	External Components:
		Updated h2database to 1.4.185
		Updated Maven Assembly Plugin to 2.5.2
		Updated Maven Compiler Plugin to 3.2
		Updated Maven Git Commit ID Plugin to 2.1.11
		Updated Maven Source Plugin to 2.4
		Updated Maven Surefire plugins to 2.18
		Updated MediaInfo for Windows to 0.7.71, which:
			Improves folder population time
			Improves detection of many formats
			Fixes bugs
		Updated MPlayer/MEncoder for Windows to SB63, which:
			Fixes support for DVD audio
			Improves support for many containers and codecs
			Fixes bugs


5.0.0 - 2015-01-25 - Changes since 5.0.0-b1

	General:
		Changed the interface for enabling/disabling renderer configurations
		Changed the default setting for wrap_dts_into_pcm to false
		Made sure files are always directly streamable via the Transcoding folder
		Added the UMS_MAX_MEMORY variable to UMS.sh
		Using info from IMDB with filename prettifying works for anime
		Minor improvements to the web interface
		Fixed audio downsampling
		Fixed a bug with parsing formats from MediaInfo
		Fixed a startup error
		Minor bugfixes and code optimizations
		Removed the database compacting method that was never recommended
		All changes from 4.3.1 and 4.4.0
	Renderers:
		Added support for Panasonic DMP-BDT360 devices
		Added support for Samsung H6500 Blu-ray Disc players
		Added support for Telefunken TVs
		Improved support for Panasonic Viera AS650 series TVs
		Improved support for Samsung C6600 series TVs
		Improved support for Sony Blu-ray Disc players
		Improved support for Sony Bravia W series TVs
		Improved file compatibility on non-PS3 renderers
		Fixed detection of some Samsung renderers
	Languages:
		Updated Czech translation
	External Components:
		Updated Cling to 2.0.1
		Updated h2database to 1.4.185
		Updated Maven Assembly Plugin to 2.5.2
		Updated Maven Compiler Plugin to 3.2
		Updated Maven Git Commit ID Plugin to 2.1.11
		Updated Maven Source Plugin to 2.4
		Updated Maven Surefire plugins to 2.18
		Updated MediaInfo for Windows to 0.7.71, which:
			Improves folder population time
			Improves detection of many formats
			Fixes bugs
		Updated MPlayer/MEncoder for Windows to SB63, which:
			Improves support for many containers and codecs
			Fixes bugs
		Updated Seamless to 1.1.0

4.4.0 - 2015-01-11

	General:
		Improved compatibility of H.264 transcoded videos
		Fixed error on Linux when VLC is not installed
		Fixed recognition of BMP, Theora and Vorbis (thanks, ler0y!)
		Added support for several more formats and codecs in renderer configs (thanks, ler0y!)
		Added "SendFolderThumbnails" renderer option
		Fixed some cases of unnecessary video transcoding
		Fixed AviSynth output compatibility on some renderers
		Improved speed of True Motion processing
		Updated setting descriptions
		Cleaned up logging
		Fixed MP3 transcoding support
		Improved library creation speed and prevented unnecessary transcoding for non-video files
	Renderers:
		Added support for Samsung ES8005 TVs (thanks, wazer!)
		Added support for Samsung BD-C6800 Blu-ray Disc Players
		Added support for Sony Xperia Z3
		Added support for Yamaha R-N500
		Improved support for Hama IR320
		Improved support for Panasonic AS600E TVs (thanks, Etmoc!)
		Improved support for Panasonic VT60 TVs
		Improved support for Samsung H6400 series TVs
		Improved support for Sony Blu-ray Disc players from 2013
		Improved support for Sony Bravia NX70x series TVs
		Improved support for Sony Bravia W series TVs
		Improved support for Xbox 360
		Fixed audio transcoding on Sony Bravia EX series TVs
		Fixed timeouts on Philips TVs
		Fixed support for images on Panasonic TVs
		Updated DefaultRenderer.conf
	Languages:
		Updated Spanish translation (thanks, AlfredoRamos!)
	External Components:
		Updated FFmpeg for Windows and OS X, which:
			Improves support for many containers and codecs
			Fixes bugs
		Updated MPlayer/MEncoder for Windows to SB62, which:
			Fixes support for Opus
			Fixes support for DVDs
			Fixes color and italic support for MicroDVD subtitles
			Improves MPEG-2 output
			Fixes x264 hardware acceleration
			Improves support for many containers and codecs
			Fixes VBR and VFR support in H.264 output
			Fixes bugs
		Updated Netty to 3.9.6, which fixed bugs

4.3.1 - 2014-12-12

	General:
		Windows installer removes deprecated renderer files
		Fixed a startup crash
	Renderers:
		Added support for Hisense K680 TVs
		Added support for Samsung F5505 TVs
		Improved support for LG LS5700 TVs (thanks, AngelShine!)
		Improved support for Panasonic S60 Series TVs
		Improved support for Samsung WiseLink devices
		Improved support for Sony Bravia HX75 Series TVs

5.0.0-b1 - 2014-11-?? - Changes since 5.0.0-a1
	General:
		Major redesign of the status tab to show per-renderer information
		Improved player control
		Includes the possibility to allow UMS to control renders automatically
		Web player can also be controlled
		Automatic reloading of external files
		Documentation updates
		New xmb playlist folders with optional automatic starting, looping, and saving, editable from web or xmb
		A Dynamic xmb playlist for on-the-fly playlist creation from web or xmb
		Better bump/gui player playlists with optional automatic looping
		Push playback support for chromecast and web browsers
		Improved Chromecast support (cast from UMS webui/gui)
		All changes from 4.2.1, 4.2.2 and 4.3.0

4.3.0 - 2014-12-06

	General:
		Added the ability to transcode to H.265
		Improved filename prettifying
		Improved support for many formats and codecs
		Fixed VLC engine
		Formatted the default renderer file
	Renderers:
		Added support for LG UB820V TVs
		Added support for Logitech Squeezebox
		Fixed video aspect ratios on Philips and Sony TVs
		Improved support for Hama IR320
		Improved support for LG LM660 TVs
		Improved support for Netgem N7700
		Improved support for Roku 3
		Improved support for Samsung EH6070 TVs
		Improved support for Samsung H4500 TVs
		Improved support for Samsung HU7000 TVs
		Improved support for Samsung HU9000 TVs
		Improved support for Xbox 360
	Languages:
		Updated Dutch translation (thanks, leroy!)

4.2.2 - 2014-11-17

	General:
		Improved compatibility of H.264 transcoding when using MEncoder
		Customized buffer color
		Fixed Live Subtitles
	Renderers:
		Added support for Hama IR320 (thanks, Enrice!)
		Added support for Roku 3 (incomplete) (thanks, weyrava and drocket!)
		Added support for Panasonic ET60 Series TVs (thanks, Enrice!)
		Added support for Samsung F5100 Blu-ray Disc players (thanks, maracucho!)
		Added support for Samsung Galaxy S5 (thanks, FlyMcDoogal!)
		Added support for Sony PlayStation Vita (incomplete) (thanks, Verequies, Balmung and xubz!)
		Fixed Xbox One detection (thanks, Whogie!)
	Languages:
		Updated Spanish translation (thanks, AlfredoRamos!)
	External Components:
		Updated JNA to 4.1.0, which fixes bugs
		Updated Maven Exec Plugin to 1.3.2, which fixes bugs
		Updated Maven Findbugs Plugin to 3.0.0, which fixes bugs
		Updated Maven Javadoc Plugin to 2.10.1, which fixes bugs
		Updated Maven Jdepend Plugin to 2.0, which fixes bugs
		Updated Maven Site Plugin to 3.4, which fixes bugs
		Updated Netty to 3.9.5, which fixes bugs

4.2.1 - 2014-11-01

	General:
		Increased speed of FFmpeg transcoding to H.264
		Updated UMS.conf
		Improved filename prettifying
		Re-added "Force default renderer" option to the GUI
		Minor GUI fixes and improvements
		Fontconfig caches are not generated if subtitles are disabled
		Renamed most renderer config files
		Fixed special characters in folder names not displaying correctly
	Renderers:
		Added support for LG WebOS TVs
		Added support for Panasonic SC-BTT Blu-ray Disc Home Theater Sound Systems
		Added support for Samsung ES8000 TVs
		Added support for Samsung F5900 Blu-ray Disc players
		Added support for Technisat S1+
		Improved detection of Windows Media Player
		Improved support for H.264 videos on some renderers
		Improved support for Cambridge Audio Azur BD players
		Improved support for Samsung WiseLink renderers
		Improved support for subtitles on Sony Bravia EX TVs (thanks, master-nevi!)
		Improved support for Panasonic TVs
		Improved support for Xbox One
		Fixed seeking on Samsung E+ Series TVs
	Languages:
		Updated French translation (thanks, Kirvx!)
	External Components:
		Updated MPlayer/MEncoder for Windows to SB60, which:
			Improves H.264 transcoding
			Improves H.265 support
			Improves support for many containers and codecs
			Fixes bugs

5.0.0-a1 - 2014-10-23

	Changes unique to 5.0.0-a1:
		General:
			Added support for UPnP connections and playback
			Added the option to use info from IMDB with filename prettifying
			Added HTML5 video support to the web interface
			Create custom per-device configuration files to override any general renderer or UMS setting
			Minor tweaks to the GUI

	Changes from the 4.x release branch:
		General:
			Increased speed of FFmpeg transcoding to H.264
			Updated UMS.conf
			Improved filename prettifying
			Re-added "Force default renderer" option to the GUI
			Minor GUI fixes and improvements
			Fontconfig caches are not generated if subtitles are disabled
		Renderers:
			Added support for Panasonic SC-BTT Blu-ray Disc Home Theater Sound Systems
			Improved support for Panasonic TVs
			Improved support for Xbox One
		Languages:
			Updated French translation (thanks, Kirvx!)

4.2.0 - 2014-10-17

	General:
		Added support for automatic 2D to 3D subtitles conversion
		Added renderer support for converting 3D video to a different 3D format
		Added a new template renderer file "DefaultRenderer.conf" which contains all possible renderer config options (like PS3.conf did before)
		Made cache recreation happen only when it has changed, instead of with every new release
		FFmpeg defers to MEncoder for subtitle transcoding if there are embedded fonts, since FFmpeg can't use them
		Improved video quality when transcoding via FFmpeg over wired networks
		Improved language detection
		Transcode instead of streaming videos when their bitrate is too high for the renderer
		Updated tooltips
		Fixed the cache not storing all data (thanks, taconaut!)
		Fixed full-SBS 3D support via FFmpeg
		Fixed not transcoding subtitles for renderers that can stream the file format but not with subtitles
	Renderers:
		Added support for Samsung HT-E3 Series Blu-ray Home Entertainment Systems
		Improved support for Panasonic TVs
		Improved support for Philips TVs (thanks, ler0y!)
		Improved support for Xbox One
		Improved some renderer images
		Formatted all renderer configs
		Removed unnecessary (same as default) values from all renderer configs
	Languages:
		Updated French translation (thanks, Kirvx!)
	External Components:
		Updated FFmpeg for Windows and OS X to 20141014 builds, which:
			Fixes a bug with subtitle parsing
			Improves support for many containers and codecs
			Fixes bugs
		Updated InterFrame to 2.6.0, which:
			Improves quality
			Improves GPU support
			Fixes bugs
		Updated Java Runtime Environment automatic downloader for Windows to 8u25, which:
			Improves security

4.1.3 - 2014-10-03

	General:
		Improved default settings for smoother playback on wired and wireless networks
		Made FFmpeg more reliable when transcoding embedded subtitles
		Enabled file cache by default
		Updated logo (subtle)
		Improved cache handling
		Disabled FFmpeg deferring to MEncoder for subtitles by default
		Fixed thumbnails on some Samsung TVs
		Fixed general bugs
	Renderers:
		Fixed unnecessary high CPU usage on LG TVs (thanks, amalic!)
		Fixed support for Sony Home Theatre Systems
		Fixed support for Xbox One
		Improved support for Samsung UE ES6575 TVs
	Languages:
		Updated Czech translation
		Updated French translation (thanks, Kirvx!)

4.1.2 - 2014-09-24

	General:
		Folder population speed improvements with filename prettifying enabled
		Web interface client-side speed improvements
		Web interface design tweaks
		Expanded filename prettifying
		Split the renderer option SupportedSubtitlesFormats into SupportedExternalSubtitlesFormats and SupportedInternalSubtitlesFormats
		Optimized video bitrate over wireless connections
		Made more strings translatable
		Fixed FFmpeg not working on OS X
		Fixed FFmpeg to MEncoder deferral via the Transcode folder
		Fixed subtitles being transcoded when they didn't need to be
		Fixed web thumbnails sometimes
		Fixed the Windows installer downloading the 32-bit version of Java on 64-bit systems
		Fixed the Windows installer offering to automatically close UMS
	Renderers:
		Improved support for Panasonic TX-L32V10E TVs
		Improved support for Samsung renderers
	Languages:
		Updated Czech translation
		Updated French translation (thanks, Kirvx!)
	External Components:
		Updated FFmpeg for OS X to g7cf1f0f, which:
			Improves support for many containers and codecs
			Fixes bugs
		Updated jQuery to 1.11.1, which:
			Fixes bugs
		Updated NSIS LockedList to 3.0.0.3, which:
			Adds support for detecting 64-bit processes
			Fixes bugs

4.1.1 - 2014-09-06

	General:
		Made videos start faster sometimes when using FFmpeg
		Improved accuracy when parsing rare files
		Improved folder population speed
		Fixed x264 transcoding when using recent versions of MEncoder
		Fixed Windows Java 6 versions trying to update to Java 7
	Renderers:
		Fixed external subtitle streaming support on Samsung TVs
	Languages:
		Updated French translation (thanks, Kirvx!)
		Updated Spanish translation
	External Components:
		Updated MPlayer/MEncoder for Windows to SB59, which:
			Fixes decoding of PGS subtitles
			Improves support for many containers and codecs
			Improves 3D compatibility on some devices
			Fixes bugs
		Updated Netty to 3.9.4, which:
			Improves security
			Fixes bugs
			Improves memory use

4.1.0 - 2014-09-01

	General:
		Added previous and next buttons to web interface
		Added the ability to prioritize renderer loading order
		Windows uninstaller no longer deletes custom renderer configs
		Windows uninstaller no longer leaves behind unused files
		Windows automatic updater downloads the correct build for the user's Java version
		FFmpeg defers to MEncoder for transcoding subtitles by default
		Fixed subtitles stretching bug
		Fixed startup crash on non-Windows operating systems
		Fixed subtitles not being transcoded when the renderer supported streaming the file
		Fixed automatic wireless quality optimizations not being applied
		Improved logging and log packing
		Speed improvements
	Renderers:
		Added support for Xbox One via DLNA
		Improved support for Panasonic VT60 TVs
	Languages:
		Updated Spanish translation (thanks, AlfredoRamos!)
	External Components:
		Updated Java Runtime Environment automatic downloader for Windows to 8u20, which:
			Improves security
		Updated FFmpeg for Windows and OS X to builds from 2014-08-28, which:
			Improves support for many containers and codecs
			Fixes bugs

4.0.3 - 2014-08-12

	General:
		Fixed MP4 support on some renderers
		Fixed transcoded video resolutions with odd numbers on some renderers
		Fixed error when retrieving audio channels
		Updated descriptions in UMS.conf
	Renderers:
		Fixed audio support on WDTV Live (thanks, DualC!)
		Fixed external subtitle support on Samsung devices
		Improved support for Panasonic VT60 TVs
	Languages:
		Fixed Spanish translation
		Updated most translations in minor ways
		Updated Dutch translation (thanks, leroy!)
	External Components:
		Updated MediaInfo for Windows to 0.7.69, which:
			Improves folder population times
			Improves stability
			Improves detection of HEVC streams
			Fixes bugs

4.0.2 - 2014-08-06

	General:
		The Java 8 build on Windows offers to update Java 7
		Added all default renderer values to PS3.conf to make it easier to support new renderers
		Added documentation and formatting to PS3.conf
		Fixed automatic Java download/installation on Windows for users without it
		Fixed the Live Subtitles feature
		Fixed external subtitle support
	Renderers:
		Improved support for DirecTV HR
		Improved support for Panasonic TVs
		Improved support for Samsung devices
	External Components:
		Updated Java Runtime Environment automatic downloader for Windows to 8u11, which:
			Improves security
		Updated Maven Compiler Plugin to 3.1

4.0.1 - 2014-08-01

	General:
		Added option InternalSubtitlesSupported to renderer configs for greater control over file compatibility
		Added option "Force external subtitles"
		Added the option "Use embedded style" to FFmpeg instead of just MEncoder
		Fixed a bug with the adaptive bitrate setting GUI
		Fixed bugs with subtitles not being loaded
		Fixed displaying the Windows-specific "prevent operating system from sleeping" option on other operating systems
		Fixed FFmpeg sometimes sending too many audio channels
		Fixed FFmpeg using a bitrate too high for audio transcoding
	Languages:
		Updated Spanish translation (thanks, AlfredoRamos!)
	External Components:
		Updated FFmpeg to builds from 2014-07-16, which:
			Improves support for many containers and codecs
			Fixes bugs

4.0.0 - 2014-07-25 - Changes since 3.6.4

	General:
		Added a web interface, available at localhost:9001
		Added option to automatically adjust the maximum bandwidth by measuring the speed of the connection to the devices
		Added a new transcoding option to allow H.264 video with AAC audio
		Added "Random" file sorting option
		Added tooltips to more options in the GUI
		Added option to use PsPing on Windows to make network speed estimates more accurate (ping_path)
		Added renderer option LimitFolders to specify whether the renderer has a folder limit
		Added renderer option SendDateMetadata to specify whether to send last modified date metadata to the renderer
		Added searching to each folder
		Added web_port and web_enable settings to UMS.conf
		Added support for 4k videos by automatically scaling them to a resolution that the renderer supports
		Made FFmpeg the default transcoding engine
		Made the buffer more animated
		Changed the names of transcoding options in renderer config files for clarity
		Output surround audio (AC-3) instead of stereo (MP2) when using VLC (needs testing)
		Enabled subtitles in VLC (thanks, tdcosta100!)
		Improved the TextWrap function, which makes filenames fit better on certain renderers
		Improved detection of which videos are automatically muxable
		Made more strings translatable
		Fixed performing unnecessary network speed checks
		Fixed a bug with the Recently Played folder
		Fixed padding in FFmpeg for DVD video resolution
		Fixed documentation images on Linux
		Fixed external subtitles sometimes not loading
		Fixed files with resolutions that are too high for the renderer streaming if they are otherwise supported
		Fixed the VLC Web Video legacy engine only outputting 25fps
		Fixed UMS occasionally telling renderers to expect a different video format than they get
		General speed improvements
		Updated several descriptions and formatted UMS.conf (user config) and PS3.conf (reference renderer config)
	Renderers:
		Added support for Google Chromecast
		Added support for Sony Bravia XBR TVs
		Improved aspect ratios on Sony Bravia EX TVs
		Improved support for Apple mobile devices (iPad, iPhone, iPod)
		Improved support for AirPlayer
		Improved support for Android
		Improved support for BlackBerry
		Improved support for Cambridge Audio Blu-ray players
		Improved support for D-Link DSM-510
		Improved support for Freebox HD
		Improved support for Freecom Music Pal
		Improved support for LG Smart TV Upgrader
		Improved support for OPPO Blu-ray players
		Improved support for Panasonic TX-L32V10E TVs
		Improved support for Popcorn Hour
		Improved support for Pioneer Kuro
		Improved support for Realtek media players
		Improved support for Samsung TVs, Blu-ray players and mobile devices
		Improved support for Showtime
		Improved support for Sony Bravia TVs, media players, Blu-ray players and Xperia devices
		Improved support for Streamium
		Improved support for Telstra T-Box
		Improved support for VideoWeb TV
		Improved support for Vizio TVs
		Improved support for WDTV Live
		Improved support for XBMC
		Improved support for Xbox 360
	Languages:
		Updated Dutch translation (thanks, leroy!)

4.0.0 - 2014-07-25 - Changes since 4.0.0-b1

	General:
		Made the buffer more animated
		Added web_port and web_enable settings to UMS.conf
		Added support for 4k videos by automatically scaling them to a resolution that the renderer supports
		Added renderer option SendDateMetadata to specify whether to send last modified date metadata to the renderer
		Improved detection of which videos are automatically muxable
		Fixed and improved web interface search
		Fixed web interface content-type
		Fixed images on web interface on Linux and OS X
		Fixed documentation images on Linux
		Fixed occasional error on Linux when packing debug files
		Fixed external subtitles sometimes not loading
		Fixed files with resolutions that are too high for the renderer streaming if they are otherwise supported
		Fixed the VLC Web Video legacy engine only outputting 25fps
		Fixed UMS occasionally telling renderers to expect a different video format than they get
		General speed improvements
		Updated several descriptions and formatted UMS.conf
	Renderers:
		Added support for Sony Bravia XBR TVs
		Improved support for Apple mobile devices (iPad, iPhone, iPod)
		Improved support for AirPlayer
		Improved support for Android
		Improved support for BlackBerry
		Improved support for Cambridge Audio Blu-ray players
		Improved support for Chromecast
		Improved support for D-Link DSM-510
		Improved support for Freebox HD
		Improved support for Freecom Music Pal
		Improved support for LG Smart TV Upgrader
		Improved support for OPPO Blu-ray players
		Improved support for Popcorn Hour
		Improved support for Pioneer Kuro
		Improved support for Realtek media players
		Improved support for Samsung TVs, Blu-ray players and mobile devices
		Improved support for Showtime
		Improved support for Sony Bravia TVs, media players, Blu-ray players and Xperia devices
		Improved support for Streamium
		Improved support for Telstra T-Box
		Improved support for VideoWeb TV
		Improved support for Vizio TVs
		Improved support for WDTV Live
		Improved support for XBMC
		Improved support for Xbox 360
		Recognize Xbox One and PlayStation 4 when they connect to the web interface
	Languages:
		Updated Dutch translation (thanks, leroy!)

4.0.0-b1 - 2014-07-11 - Changes since 4.0.0-a1

	General:
		Added option to automatically adjust the maximum bandwidth by measuring the speed of the connection to the devices
		Added searching to the web interface
		Added and improved documentation for more renderer options to PS3.conf (CustomFFmpegOptions, OverrideFFmpegVideoFilter and KeepAspectRatio)
		Added "Random" file sorting option
		Added tooltips to more options in the GUI
		Added option to use PsPing on Windows to make network speed estimates more accurate (ping_path)
		Added renderer option LimitFolders to specify whether the renderer has a folder limit or not
		Added searching to each folder
		Improved web interface display, especially on mobile devices
		Improved changing settings via web interface
		Removed the folder limit from the web interface
		Made more strings translatable
		Fixed a bug with filename prettifying
		Made the renderer setting KeepAspectRatio more reliable
		Renamed the renderer option OverrideVideoFilter to OverrideFFmpegVideoFilter
		Fixed incorrect aspect ratios being cached
		Fixed bugs with FFmpeg subtitles
		Fixed a bug with adding files
		Fixed ignored renderers
		Fixed performing unnecessary speed checks
		Fixed a bug with the Recently Played folder
	Renderers:
		Improved support for DirecTV HR
		Improved support for Panasonic TVs
		Improved support for Samsung TVs, Blu-ray players and mobile devices
		Improved aspect ratios on Sony Bravia EX TVs
	External Components:
		Updated Apache commons-lang to 3.3.2, which:
			Fixes bugs
		Updated Logback to 1.1.2, which:
			Fixes bugs
		Updated Maven FindBugs Plugin to 2.5.4
		Updated Maven Git Commit ID Plugin to 2.1.9
		Updated MPlayer/MEncoder for Windows to SB58, which:
			Improves H.265 support
			Improves buffer allocation
			Makes seeking more accurate
			Improves support for many containers and codecs
			Improves speed
			Fixes bugs
		Updated Netty to 3.9.1, which:
			Fixes bugs
		Updated slf4j to 1.7.7

3.6.4 - 2014-06-27

	General:
		Fixed a bug with filename prettifying
		Fixed a bug with FFmpeg subtitles
		Fixed a bug with adding files
		Fixed ignored renderers
	Renderers:
		Improved support for Samsung devices
	External Components:
		Updated MPlayer/MEncoder for Windows to SB58, which:
			Improves H.265 support
			Improves buffer allocation
			Makes seeking more accurate
			Improves support for many containers and codecs
			Improves speed
			Fixes bugs

3.6.3 - 2014-06-13 - Changes since 3.6.2

	General:
		Added and improved documentation for more renderer options to PS3.conf (CustomFFmpegOptions, OverrideFFmpegVideoFilter and KeepAspectRatio)
		Added support for configuring whether UMS can run just one or multiple instances
		Made the renderer setting KeepAspectRatio more reliable
		Renamed the renderer option OverrideVideoFilter to OverrideFFmpegVideoFilter
		Fixed incorrect aspect ratios being cached
		Fixed thumbnail seek position
		Fixed bugs with FFmpeg subtitle styles
	Renderers:
		Added support for nPlayer, skifta and TwonkyBeam on portable Apple devices (thanks, MattDetroit!)
		Added support for LG LM620 TVs (thanks, michal-sapsa!)
		Added support for Sony Bravia W7 series TVs (thanks, shtirlic!)
		Improved support for DirecTV HR
		Improved support for OPPO devices
		Improved support for Panasonic TVs
		Improved support for Samsung mobile devices
	Languages:
		Updated Czech translation
		Updated Spanish translation (thanks, AlfredoRamos!)
	External Components:
		Updated Apache commons-lang to 3.3.2, which:
			Fixes bugs
		Updated Logback to 1.1.2, which:
			Fixes bugs
		Updated Maven FindBugs Plugin to 2.5.4
		Updated Maven Git Commit ID Plugin to 2.1.9
		Updated MPlayer/MEncoder for Windows to SB55, which:
			Fixes DTS-ES to AC-3 transcoding
		Updated Netty to 3.9.1, which:
			Fixes bugs
		Updated slf4j to 1.7.7

4.0.0-a1 - 2014-06-02 - Changes since 3.6.2

	General:
		Added a web interface, available at localhost:9001
		Added a new transcoding option to allow H.264 video with AAC audio
		Added documentation for more renderer options to PS3.conf (CustomFFmpegOptions and OverrideVideoFilter)
		Added support for configuring whether UMS can run just one or multiple instances
		Changed the names of transcoding options in renderer config files for clarity
		Output surround audio (AC-3) instead of stereo (MP2) when using VLC (needs testing)
		Enabled subtitles in VLC (thanks, tdcosta100!)
		Made FFmpeg the default transcoding engine
		Improved the TextWrap function, which makes filenames fit better on certain renderers
		Fixed padding in FFmpeg for DVD video resolution
		Fixed thumbnail seek position
	Renderers:
		Added support for Google Chromecast
		Added support for nPlayer, skifta and TwonkyBeam on portable Apple devices (thanks, MattDetroit!)
		Added support for LG LM620 TVs (thanks, michal-sapsa!)
		Added support for Sony Bravia W7 series TVs (thanks, shtirlic!)
		Improved support for OPPO devices
		Improved support for Panasonic TX-L32V10E TVs
	Languages:
		Updated Czech translation
		Updated Spanish translation (thanks, AlfredoRamos!)
	External Components:
		Updated Apache commons-lang to 3.3.2, which:
			Fixes bugs
		Updated Logback to 1.1.2, which:
			Fixes bugs
		Updated Maven Git Commit ID Plugin to 2.1.9
		Updated Netty to 3.9.1, which:
			Fixes bugs
		Updated slf4j to 1.7.7

3.6.2 - 2014-05-26

	General:
		Tweaked the default Wi-Fi settings for smoother playback on slower networks
		Fixed automatic updating on Windows
		Fixed a memory leak
		Fixed several bugs on OS X
		Optimized code
		Improved logging
	Renderers:
		Improved support for Apple mobile devices (iPad, iPhone, iPod)
		Improved support for DirecTV HR
		Improved support for LG LA644V Smart TV
		Improved support for Nokia N900
		Improved support for Pioneer Kuro
		Improved support for Samsung mobile devices
		Improved support for Sony Bravia 4500
		Improved support for Sony Bravia EX620
		Improved support for Telstra T-Box
	Languages:
		Updated Spanish translation (thanks, AlfredoRamos!)

3.6.1 - 2014-05-15

	General:
		Made resuming more intuitive
		Fixed the aspect ratio of thumbnails by default (thanks, tdcosta100!)
		Fixed the buffer going nuts after automatic subtitle conversion (thanks, tdcosta100!)
		Fixed support for URLs in playlists
		Fixed error with thumbnails of resume files (thanks, tdcosta100!)
		Fixed the user interface when hiding advanced options
	Languages:
		Made more strings translatable
		Updated Czech translation
		Updated Russian translation (thanks, Tianuchka!)
	External Components:
		Updated FFmpeg to builds from 2014-05-05, which:
			Improves support for many containers and codecs
			Fixes bugs
		Updated h2database to 1.3.176, which:
			Improves stability
		Updated MPlayer/MEncoder for Windows to SB57, which:
			Adds AAC encoding support
			Improves H.265 support
			Improves support for many containers and codecs
			Improves speed
			Fixes bugs

3.6.0 - 2014-04-28

	General:
		Use the Entertainment application category on OS X instead of Other
		MPlayer thumbnail generation is disabled in the GUI if "Generate thumbnails" is unticked
		Made Matroska container detection more reliable
		Message dialogs display in the center of the program instead of the screen
		The FFmpegAudio engine supports web audio
		Valid MIME types are always sent to the renderer
		Thumbnails of resume files are taken from the correct time (thanks, tdcosta100!)
		The "Hide empty folders" option works for New Media folders (thanks, jpeyper!)
		Improved support for transcoding to H.264 (thanks, tdcosta100!)
		Improved detection of H.264 (thanks, tdcosta100!)
		Improved speed
		Fixed FFmpeg subtitles conversion (thanks, tdcosta100!)
		Fixed the ability to stop scanning the library
		Fixed the ability to resume videos with external subtitles
		Fixed LPCM transcoding
		Fixed renderer selection
		Fixed timeseek detection (thanks, tdcosta100!)
	Renderers:
		Added support for Cambridge Audio Azur BD players (thanks, Triplefun!)
		Added support for LG LA6200 TVs (thanks, douglasanpa!)
		Added support for Sony Bravia BX305 TVs (thanks, douglasanpa!)
		Added support for more sizes of LG LA644V TVs
		Allow muxing of non-mod4 videos to Bravia TVs (thanks, tdcosta100!)
	Languages:
		Updated Czech translation
		Updated Spanish translation (thanks, AlfredoRamos!)
	External Components:
		Updated Java Runtime Environment automatic downloader for Windows to 7u55, which:
			Improves security

3.5.0 - 2014-03-19

	General:
		Added a reminder at the end of the configuration wizard about optionally setting shared folders
		Added an option to run the configuration wizard on the next program start
		Added the ability to specify folders to ignore (folders_ignored in UMS.conf)
		Made the program icon look better in some situations (added more resolutions)
		Expanded filename prettifying
		Fixed OpenSubtitles support
		Improved stability of packing debug files
		Improved readme file
		Improved speed
		Cleaned some renderer configs
		Increased the safety of using -ac, -ab and -c:a in the CustomFFmpegOptions string in renderer configs
	Renderers:
		Improved support for DirecTV and Samsung phones when using FFmpeg
	Languages:
		Updated English
	External Components:
		Updated h2database to 1.3.175, which:
			Improves stability
		Updated Logback to 1.1.1
		Updated MPlayer/MEncoder for Windows to SB56, which:
			Improves DVD support
			Fixes encoding sometimes finishing too soon
			Improves stability
			Improves support for the following video codecs: MPEG-1, MPEG-2, H.264, H.265, VP8, VP9
			Improves support for the following audio codecs: DTS
			Improves support for the following containers: Matroska, MOV, OGG
			Adds x265 encoding support
			Improves audio sync
			MPEG-4 adheres more strictly to spec limits
		Updated Netty to 3.9.0, which:
			Improves speed
			Fixes bugs
		Updated slf4j to 1.7.6, which:
			Improves stability

3.4.2 - 2014-02-25

	General:
		Improved fontconfig cache creation
		The shared directory setting in the config is read more safely
		Fixed video playback on some renderers including OPPO devices
		Fixed a crash with certain files
		Fixed a buffer display error
	Languages:
		Updated Spanish translation (thanks, AlfredoRamos!)
	External Components:
		Updated FFmpeg to builds from 2014-02-12, which:
			Improves support for the following video codecs: H.264, H.265, MPEG-1, MPEG-2, VC-1, VP8, VP9
			Improves support for the following audio codecs: AC-3, DTS, WMA
			Improves support for the following containers: AVI, Matroska, MPEG-TS
			Fixes memory leaks
			Improves detection of 60FPS
			Improves audio sync
		Updated MediaInfo for Windows to 0.7.64, which:
			Fixes crashes
		Updated Netty to 3.7.0

3.4.1 - 2014-02-14

	General:
		Fixed MPlayer thumbnail generation
		Fixed support for the MovieInfo and LastFMScrobbler plugins
		Improved math accuracy
		Improved thread-safety
		Improved code speed
		Minor GUI improvements
	External Components:
		Updated JGoodies Forms to 1.6.0
		Updated Maven Checkstyle plugin to 2.11
		Updated Maven Doxia Docbook Simple plugin to 1.5
		Updated Maven FindBugs Plugin to 2.5.3
		Updated Maven Site plugin to 3.3
		Updated Maven Surefire Report plugin to 2.16

3.4.0 - 2014-02-06

	General:
		Added "Encoded Audio Passthrough" option for DTS and AC-3 audio
		Added support for 3D MKV files (mk3d)
		Made font cache creation over 50% faster on Windows
		Font cache creation is done in the background
		Expanded filename prettifying
		Clicking "Check for updates" tells you when there are no updates
		Image thumbnails are generated faster
		Fixed FLAC support on some players
		Removed broken entry from WEB.conf
		Minor English language improvements
	Renderers:
		Improved support for the Vizio renderer
	External Components:
		Updated Commons Codec to 1.9
		Updated Commons Configuration to 1.10
		Updated FFmpeg to builds from 2014-01-05+, which:
			Adds support for reading and setting stereoscopic (3D) metadata for MPEG-2, H.264 and H.265 streams
			Improves AV sync, especially when outputting MPEG-TS
			Improves support for the following audio codecs: AC-3, DTS
			Improves support for the following video codecs: VP9, H.264, H.265
			Improves AviSynth support
			Improves Matroska support
		Updated InterFrame to 2.5.1, which:
			Improves quality
			Improves GPU support
			Fixes bugs
		Updated JGoodies Forms to 1.7.2
		Updated JGoodies Looks to 2.5.3
		Updated JNA to 4.0.0, which:
			Fixes bugs
		Updated MPlayer/MEncoder for Windows to SB55, which:
			Adds Google VP9 support
			Improves support for some DVDs
			Speed improvements
			Fixes memory leaks
			Fixes a fontconfig bug
			Improves support for the following containers: AVI, Matroska, MPEG-TS
			Improves support for the following video codecs: H.264, H.265, MPEG-1, MPEG-2, VC-1, VP8
			Improves support for the following audio codecs: AAC, AC-3
			Improves detection of 60FPS
		Updated MediaInfo for Windows to 0.7.67, which:
			Fixes bugs
		Updated Netty to 3.9.0, which:
			Improves speed
			Fixes bugs
		Updated Thumbnailator to 0.4.7

3.3.0 - 2013-12-08

	General:
		Added the option to toggle the display of the profile name after the server name on the renderer
		Added a warning on the Status tab if the UMS port is being blocked
		Added some missing entries to UMS.conf
		Added automatic video scaling to renderer resolution limits in FFmpeg
		Added the ability to use asterisks for the "Force transcoding" and "Skip transcoding" options
		Added config option "itunes_library_path"
		Improved detection of which videos should be muxed automatically when using MEncoder or FFmpeg
		Fixed files with compatible video but incompatible audio not muxing video
		Fixed support for outputting 24-bit FLAC files as 24-bit by muxing them with video
		Fixed audio playback if VLC is enabled
		Made server restarts more reliable
		Made sure FFmpeg outputs a compatible colorspace when transcoding to H.264
		Made sure the transcode folder appears before files instead of after (below)
		Renamed config setting "ffmpeg_font_config" to "ffmpeg_fontconfig"
		Renamed config setting "hide_new_media" to "hide_new_media_folder"
	Renderers:
		Improved video compatibility on Panasonic TVs
		Updated the PS3 image
		Updated the XBMC logo
	Languages:
		Minor language tweaks
		Renamed "Advanced Monitoring" to "Include in New Media folder"

3.2.0 - 2013-12-01

	General:
		Added font cache generation for FFmpeg on startup
		Added the ability for users to set their own server name
		Added example virtual folders file in the profile directory
		The virtual folders file is now loaded from the profile directory instead of the installation directory
		Made seeking more accurate with FFmpeg
		When FFmpeg muxes, it always uses tsMuxeR
		Improved compatibility with videos with unusual colorspaces
		Improved compatibility when transcoding to H.264 via FFmpeg
		Improved compatibility with video with HE-AAC audio
		Improved compatibility when using tsMuxeR via FFmpeg
		Improved compatibility with high-resolution videos such as 3D-SBS and 3D-OU
		Fixed resume files causing a startup crash
		Fixed some settings causing a crash
		Fixed bug with FFmpeg subtitles
	Languages:
		Updated Spanish translation (thanks, AlfredoRamos!)
	External Components:
		Updated FFmpeg to builds from 2013-11-27, which:
			Adds support for HEVC (H.265)
			Improves seeking accuracy
			Improves AV sync
			Fixes memory leaks
			Optimizes AC-3 decoding
			Improves support for 60FPS videos
			Improves AVI support
			Silences meaningless errors
			Improves DTS support
			Supports seeking to non-keyframes in Matroska files
			Improves AC-3 support
			Fixes support for rare MP4 videos

3.1.2.1 - 2013-11-26

	General:
		Fixed a video compatibility bug affecting some users

3.1.2 - 2013-11-25

	General:
		Added tooltips to more options
		Changed the appearance of tooltips
		Removed the ability to set both FFmpeg muxing options at the same time in the GUI
		Minor GUI improvements
		Made the resume feature more stable
		Fixed video compatibility bugs
		Fixed FFmpeg on Linux systems which do not have FFmpeg installed

3.1.1 - 2013-11-24

	General:
		Sort renderers by name in the "Default renderer" option
		Fixed a bug that caused unnecessary transcoding/muxing
		Fixed subtitles not being transcoded when they should be
		Fixed parsing of FFmpeg protocols
		Fixed memory leaks
		Optimized code
	Renderers:
		Improved video support on Panasonic TX-L32V10E TVs
		Improved video support on PS3
	Languages:
		Updated Dutch translation (thanks, leroy!)
		Updated Spanish translation (thanks, AlfredoRamos!)
	External Components:
		Updated h2database to 1.3.174, which:
			Improves speed
			Improves stability
		Updated MediaInfo for Windows to 0.7.65, which:
			Improves stability

3.1.0 - 2013-11-18

	General:
		Added new "Random" sorting option (thanks, etrunko!)
		Added support for sending subtitles (losslessly) to renderers via closed captioning
		Added option to clear all monitored files
		Added support for picture-based subtitles (like VobSub) in FFmpeg
		Added whitelist option for advanced users
		Added support for automatically muxing WEB-DL files on all operating systems by default
		Added automatic tsMuxeR muxing via FFmpeg
		Added tooltips to more settings
		Improved video compatibility when transcoding to H.264 via MEncoder or FFmpeg
		Increased speed of FFmpeg transcoding by default by ~25% in some cases
		Disabled AviSynth/MEncoder by default
		Fixed subtitle character conversion in FFmpeg
		Fixed the configuration option to disable multithreading with FFmpeg engines
		Fixed the New Media folder not adhering to the empty folder setting
		Fixed the broken FFmpeg binary on Linux
	Renderers:
		Added support for Sony STR-DA5800ES (thanks, AYColumbia!)
		Added support for sending SubRip and MicroDVD subtitles to Panasonic TVs
		Added support for sending SubRip subtitles to WD TV Live
		Added support for sending SubRip subtitles to Xbox Media Center
		Improved thumbnail support on Sony Bravia TVs
	Languages:
		Updated Italian translation (thanks, nocciola82!)
		Updated Spanish translation (thanks, AlfredoRamos!)
	External Components:
		Updated MPlayer/MEncoder for Windows to SB53, which:
			Adds support for HEVC (H.265)
			Adds support for 4k AVC (H.264)
			Supports seeking to non-keyframes in Matroska files
			Improves seeking accuracy
			Improves H.264 encoding quality in fade-heavy clips
			Improves bitrate calculation
			Improves ASS/SSA subtitle support
			Improves support for some DVDs
			Improves support for 60FPS videos
			Improves DTS support
			Improves AVI support
			Increases speed
			Fixes a crash when trying to play a DVD from different region than currently set in the DVD drive
			Fixes memory leaks
			Fixes multithreaded decoding
			Fixes bugs
		Updated tsMuxeR for Linux and OS X to 2.2.3b, which:
			Adds support for DTS-HD elementary stream with extra DTSHD headers
			Improves muxing speed
			Improves stream standards compatibility
			Fixes E-AC3 support
			Adds support for DTS-express audio
			Reduces bandwidth
		Updated tsMuxeR for Windows to 2.2.3b, which:
			Adds support for DTS-HD elementary stream with extra DTSHD headers
			Reduces bandwidth

3.0.0 - 2013-11-01 - Changes since 2.6.5

	General:
		Transcoding quality automatically adjusts for content and network speed by default, allowing wireless users an easier experience
		Added the New Media folder, which contains unwatched media
		Added the Recently Played folder, which contains watched media
		Added the ability to resume videos after closing the program, for details on its use see the tooltip on the Navigation tab
		Added support for muxing (losslessly streaming) more files with tsMuxeR by default
		Added support for sending subtitle streams (without transcoding) to more devices
		Added more information to our debug logs
		Added internal subtitle support to FFmpeg
		Added subtitle seeking support to FFmpeg
		Added subtitle styling (fontconfig) support to FFmpeg
		Added "Prettify filenames" feature, for details on its use see the tooltip on the Navigation tab
		Added configuration wizard to simplify common things like network configuration
		Added an option to hide/show advanced options, hopefully making things less confusing/daunting for most users
		Added VLC video transcoding (thanks, LordQuackstar!)
		Added new options to iTunes library browsing: Browse by Artist, Album and Genre
		Added manual configuration option (hide_subs_info) to hide subtitle info from filenames while browsing
		Added checkboxes to enable/disable renderers
		Added tooltips to several options to make them clearer
		Added H.264 transcoding quality option
		Enabled support for cover files in virtual folders (thanks, D-Kalck!)
		Improved video transcoding quality by default
		Improved audio transcoding quality in FFmpeg by default
		A more helpful error is displayed when something is using the UMS HTTP port
		Made some custom MEncoder options safer to use
		Improved file compatibility for mod2 videos
		Improved support for iTunes compilations
		Made video playback more stable
		FFmpeg starts faster
		Improved speed of list population
		Audio files are added to the transcode folder
		Chapter folders are hidden if the video duration is less than the chapter length
		Improved file compatibility detection for H.264 muxing
		Renamed a lot of configuration variables
		Removed the obsolete MPlayerAudio, MPlayerWebAudio and MPlayerWebVideoDump engines
		Improved memory defaults on Windows
		The "Clean Install" option on Windows removes the program directory
		The server logo displays on more renderers
		A status message is displayed while UMS is checking the MPlayer font cache
		MPlayer/MEncoder is no longer included in the Linux builds
		Added renderer config option DisableMencoderNoskip
		Made the use of acodec in CustomMencoderOptions safer
		Videos work when tsMuxeR is the only engine enabled
		Code optimizations to run faster
		Fixed duplicate entries in DVD/playlist/feed folders
		Fixed FFmpeg web videos on Linux and OS X
		Fixed external ASS/SSA support (Thanks, skeptical!)
		Fixed FFmpeg muxing on non-PS3 renderers
		Fixed use of custom Java Heapsize (Thanks, OptimusPrime!)
		Fixed streaming radio timing out after 1:45:32
		Fixed MEncoder subtitle support when the font name contains a space
		Fixed startup issue on OS X
		Fixed RAW image transcoding and support 28 additional RAW file extensions
		Fixed incorrect server IP address when starting UMS by launching UMS.exe or UMS.bat
		Fixed and improved support for virtual folders
		Fixed conflicts between some renderer config options
		Fixed tsMuxeR video detection bug
		Fixed x264 transcoding with MEncoder
		Fixed a rare startup bug
		Fixed layout issues
		Fixed memory leaks
		Fixed various bugs
	Renderers:
		Added support for LG 42LA644V TVs (thanks, khmelnov.roman)
		Added profile for Samsung D7000 TVs
		Added support for Samsung mobiles (Galaxy S3, S4, Note, Note 2, and Note 3)
		Added support for Panasonic TX-L32V10E TVs
		Added thumbnail support to Samsung 2012 ES and EH models (thanks, dmitche3!)
		Added support for Bravia W series TVs
		Improved support for KalemSoft Media Player on BlackBerry PlayBook
		Updated support for Bravia W series TVs (thanks, flux131)
		Updated Samsung AllShare logo
		Updated notes on Philips PFL (thanks, ler0y!)
		Xbox 360 supports 5.1 audio
		Faster transcoding on Xbox 360
		Improved DirecTV support
		Updated DirecTV image
		Improved PS3 support
		Enabled text-wrapping for long names on Sony Blu-ray players
	Languages:
		Added English (UK) to the list of languages
		Updated Czech translation
		Updated Dutch translation (thanks, Leroy!)
		Updated French translation
		Updated Spanish translation (thanks, AlfredoRamos and uman2k!)
	External Components:
		Updated Apache Commons Codec to 1.8
		Updated Checkstyle to 2.10
		Updated Cobertura Maven Plugin to 2.5.2
		Updated FFmpeg on all operating systems, which:
			Improves ASS/SSA subtitle support
			Improves handling of aspect ratios, even when muxing
			Fixes multithreaded MPEG-4 decoding
			Added multithreading to ALAC decoding
			Speeds up JPEG decoding
			Fixes and improves MP3, AAC and DTS decoding stability
			Fixes memory leaks
			Fixes channel mapping with surround AAC audio
			Improves H.264 and VC-1 support
			Improves Vorbis support
			Improves Matroska support
			Improves MPEG-TS muxing
		Updated FindBugs Maven Plugin to 2.5.2
		Updated Gson to 2.2.4
		Updated H2 Database Engine to 1.3.173
		Updated Java Runtime Environment automatic downloader for Windows to 7u45, which:
			Improves security
		Updated JBoss APIviz to 1.3.2
		Updated jgoodies-forms to 1.6.0
		Updated jgoodies-looks to 2.5.2
		Updated JUnit to 4.11
		Updated Logback to 1.0.13
		Updated Maven Cobertura Plugin to 2.6
		Updated Maven Doxia Docbook Simple Plugin to 1.4
		Updated Maven Git Commit ID Plugin to 2.1.5, which:
			Improves stability
		Updated Maven IDEA Plugin to 2.2.1
		Updated Maven Javadoc Plugin to 2.9.1, which:
			Makes it more threadsafe
			Improves security
		Updated Maven Project Info Reports to 2.7
		Updated Maven Source Plugin to 2.2.1
		Updated Maven Surefire to 2.16
			Fixes problems with character encodings
		Updated MediaInfo for Windows to 0.7.64, which:
			Adds H.265 (HEVC) support
			Fixed some crashes with .mov files
			Improved AVI support
			Added HEVC/H.265 in MKV support
			Added Dolby E support
			Improved AVI, MKV, MPEG-TS, QuickTime, RMVB, MPEG-4, AAC, AVC, AC-3 and DTS support
			Faster MPEG-TS analysis
		Updated Netty to 3.6.6
		Updated PMD Maven Plugin to 3.0.1
		Updated slf4j to 1.7.5
		Updated Thumbnailator to 0.4.5, which:
			Fixes memory errors with the latest Java Runtimes
			Fixes use of Exif orientation
		Updated tsMuxeR for Windows to 2.0.6b, which:
			Improves muxing speed
			Improves stream standards compatibility
			Fixes E-AC3 support
			Adds support for DTS-express audio
		Updated xmlwise to 1.2.11

3.0.0 - 2013-11-01 - Changes since 3.0.0-b1

	General:
		Added support for muxing (losslessly streaming) more files with tsMuxeR by default
		Added support for sending subtitle streams (without transcoding) to more devices
		Added more information to our debug logs
		The "Clean Install" option on Windows removes the program directory
		The server logo displays on more renderers
		The wizard defaults to "no" for DTS streaming by default for compatibility
		A status message is displayed while UMS is checking the MPlayer font cache
		MPlayer/MEncoder is no longer included in the Linux builds
		Fixed FFmpeg web videos on Linux and OS X
		Added renderer config option DisableMencoderNoskip
		Made the use of acodec in CustomMencoderOptions safer
		Videos work when tsMuxeR is the only engine enabled
		Fixed a bug where VLC would include unwanted subtitles
		Fixed MIME types in some situations
		Fixed (hopefully) a duration/seeking bug
		Fixed the video resuming feature
		Code optimizations to run faster
	Renderers:
		Added support for LG 42LA644V TVs (thanks, khmelnov.roman)
		Added support for Samsung mobiles (Galaxy S3, S4, Note, Note 2, and Note 3)
		Added support for Panasonic TX-L32V10E TVs
		Updated notes on Philips PFL (thanks, ler0y!)
		Updated support for Bravia W series TVs (thanks, flux131)
		Updated Samsung AllShare logo
	Languages:
		Updated Czech translation
		Updated Spanish translation (thanks, AlfredoRamos!)
	External Components:
		Updated Java Runtime Environment automatic downloader for Windows to 7u45, which:
			Improves security
		Updated tsMuxeR for Windows to 2.0.6b, which:
			Improves muxing speed
			Improves stream standards compatibility
			Fixes E-AC3 support
			Adds support for DTS-express audio

3.0.0-b1 - 2013-10-17 - Changes since 3.0.0-a5

	General:
		Added the New Media folder, which contains unwatched media
		Added the Recently Played folder, which contains watched media
		Added checkboxes to enable/disable renderers
		Enabled support for cover files in virtual folders (thanks, D-Kalck!)
		A more helpful error is displayed when something is using the UMS HTTP port
		Improved calculation of subtitle font size in FFmpeg
		Expanded filename prettifying
		Made some custom MEncoder options safer to use
		Improved file compatibility for mod2 videos
		Fixed and improved support for virtual folders
		Fixed conflicts between some renderer config options
		Fixed tsMuxeR video detection bug
		Fixed x264 transcoding with MEncoder
		Fixed a rare startup bug
		Fixed FFmpeg subtitles bug
		Fixed a general subtitles bug (thanks, Skeptical!)
		Fixed several minor bugs
		Fixed plugin compatibility problems
	Renderers:
		Improved DirecTV support
		Updated DirecTV image
		Improved PS3 support
		Enabled text-wrapping for long names on Sony Blu-ray players
	External Components:
		Updated H2 Database Engine to 1.3.173
		Updated Maven Cobertura Plugin to 2.6
		Updated Maven Doxia Docbook Simple Plugin to 1.4
		Updated Maven Git Commit ID Plugin to 2.1.5, which:
			Improves stability
		Updated Maven IDEA Plugin to 2.2.1
		Updated Maven Javadoc Plugin to 2.9.1, which:
			Makes it more threadsafe
			Improves security
		Updated Maven Surefire Plugin to 2.16, which:
			Fixes problems with character encodings

3.0.0-a5 - 2013-09-02 - Changes since 3.0.0-a4

	General:
		Fixed a startup problem affecting some users

3.0.0-a4 - 2013-09-01 - Changes since 3.0.0-a3

	General:
		Improved support for iTunes compilations
		Added support for external subtitles in VLC
		Expanded and fixed filename prettifying
		Made video playback more stable
		Fixed support for non-English iTunes libraries
		Fixed external ASS/SSA support (Thanks, skeptical!)
		Fixed FFmpeg muxing on non-PS3 renderers
		Fixed use of custom Java Heapsize (Thanks, OptimusPrime!)
		Fixed VLC audio/subtitle language selection
		Fixed streaming radio timing out after 1:45:32
		Fixed MEncoder subtitle support when the font name contains a space
		Run the program in Java's server mode when using 64-bit JRE
	Renderers:
		Added support for Bravia W series TVs
		Improved support for KalemSoft Media Player on BlackBerry PlayBook
	Languages:
		Updated French translation
		Updated Spanish translation (thanks, AlfredoRamos!)
	External Components:
		Updated Apache Commons Codec to 1.8
		Updated Checkstyle to 2.10
		Updated Cobertura Maven Plugin to 2.5.2
		Updated FindBugs Maven Plugin to 2.5.2
		Updated Gson to 2.2.4
		Updated H2 Database Engine to 1.3.172
		Updated JBoss APIviz to 1.3.2
		Updated jgoodies-forms to 1.6.0
		Updated jgoodies-looks to 2.5.2
		Updated JUnit to 4.11
		Updated Logback to 1.0.13
		Updated Maven Project Info Reports to 2.7
		Updated Maven Source Plugin to 2.2.1
		Updated Maven Surefire to 2.15
		Updated PMD Maven Plugin to 3.0.1
		Updated slf4j to 1.7.5
		Updated Thumbnailator to 0.4.5, which:
			Fixes use of Exif orientation
		Updated xmlwise to 1.2.11

3.0.0-a3 - 2013-07-12 - Changes since 3.0.0-a2

	General:
		Fixed folder browsing in 32-bit mode
	External Components:
		Updated MediaInfo for Windows to 0.7.64, which:
			Added HEVC/H.265 in MKV support
			Added Dolby E support
			Improved AVI, MKV, MPEG-TS, QuickTime, RMVB, MPEG-4, AAC, AVC, AC-3 and DTS support
			Faster MPEG-TS analysis

3.0.0-a2 - 2013-07-05 - Changes since 3.0.0-a1

	General:
		Added manual configuration option (hide_subs_info) to hide subtitle info from filenames while browsing
		FFmpeg starts faster
		Improved quality of H.264 transcoding by default
		Added H.264 transcoding quality option
		Improved speed of list population
		VLC video no longer loops
		Added multithreading support to VLC
		Audio files are added to the transcode folder
		Chapter folders are hidden if the video duration is less than the chapter length
		Improved file compatibility detection for H.264 muxing
		Renamed a lot of configuration variables
		Added thumbnail support to Samsung 2012 ES and EH models (thanks, dmitche3!)
		Removed the obsolete MPlayerAudio, MPlayerWebAudio and MPlayerWebVideoDump engines
		Fixed duplicate entries in DVD/playlist/feed folders
		Fixed startup issue on OS X
		Fixed RAW image transcoding and support 28 additional RAW file extensions
		Fixed incorrect server IP address when starting UMS by launching UMS.exe or UMS.bat
		Fixed layout issues
		Fixed various bugs
	Languages:
		Updated Dutch translation (thanks, Leroy!)
	External Components:
		Updated FFmpeg on all operating systems, which:
			Improves ASS/SSA subtitle support
			Improves handling of aspect ratios, even when muxing
			Fixes multithreaded MPEG-4 decoding
			Added multithreading to ALAC decoding
			Speeds up JPEG decoding
			Fixes and improves MP3, AAC and DTS decoding stability
			Fixes memory leaks
			Fixes channel mapping with surround AAC audio
			Improves H.264 and VC-1 support
			Improves Vorbis support
			Improves Matroska support
			Improves MPEG-TS muxing
		Updated MediaInfo for Windows to 0.7.63, which:
			Adds H.265 (HEVC) support
			Fixed some crashes with .mov files
			Improved AVI support
		Updated Netty to 3.6.6
		Updated Thumbnailator to 0.4.4, which:
			Fixes memory errors with the latest Java Runtimes

2.6.5 - 2013-06-06 - Changes since 2.6.4

	General:
		Made sure it is possible to override thumbnails
		Fixed RAW image transcoding and support 28 additional RAW file extensions
	Languages:
		Updated Czech translation
		Updated Dutch translation (thanks, leroy!)
		Updated Spanish translation (thanks, Alfredo Ramos and uman2k!)
	External Components:
		Updated MediaInfo for Windows to 0.7.63, which:
			Adds H.265 (HEVC) support
			Fixed some crashes with .mov files
			Improved AVI support
		Updated Netty to 3.6.6
		Updated Thumbnailator to 0.4.4, which:
			Fixes memory errors with the latest Java Runtimes

3.0.0-a1 - 2013-05-15

	General:
		Added internal subtitle support to FFmpeg
		Added subtitle seeking support to FFmpeg
		Added subtitle styling (fontconfig) support to FFmpeg
		Added "Prettify filenames" feature, for details on its use see the tooltip on the Navigation tab
		Added configuration wizard to simplify common things like network configuration
		Added an option to hide/show advanced options, hopefully making things less confusing/daunting for most users
		Added VLC video transcoding (thanks, LordQuackstar!)
		Added new options to iTunes library browsing: Browse by Artist, Album and Genre
		Added the ability to resume videos after closing the program, for details on its use see the tooltip on the Navigation tab
		Transcoding quality automatically adjusts for content and network speed by default, allowing wireless users an easier experience
		Improved memory defaults on Windows
		Improved video transcoding quality by default
		Improved audio transcoding quality in FFmpeg by default
		Fixed memory leaks
		Added tooltips to several options to make them clearer
		OS X build runs optimized Java 7 code
	Renderers:
		Xbox 360 supports 5.1 audio
		Faster transcoding on Xbox 360
		Added profile for Samsung D7000 TVs
	Languages:
		Added English (UK) to the list of languages
		Updated Czech translation
		Updated Spanish translation (thanks, Alfredo Ramos and uman2k!)

2.6.4 - 2013-05-10

	General:
		Made the menu icon on OS X grayscale to fit with most other programs (thanks, Kefran!)
		Made file list population faster
		Added option to ignore certain renderer configurations
		Improved file compatibility on all renderers
	Renderers:
		Improved file compatibility on Sony Bravia TVs
		Improved file compatibility on Samsung TVs
	Languages:
		Updated English settings
		Updated all language flags
		Corrected Slovak language flag
		Added language flags for Icelandic, Lithuanian and Slovenian

2.6.3 - 2013-05-01

	General:
		Improved detection of renderers
		Improved AAC/M4A support
	External Components:
		Updated Apache commons-lang to 3.1
		Updated thumbnailator to 0.4.3

2.6.2 - 2013-04-22

	General:
		Fixed audio sync when muxing DTS via FFmpeg
		Improved descriptions in GUI
		Added support for the file:// protocol in WEB.conf
		Protocols are matched before extensions in WEB.conf
		Fixed "Can't assign requested address" bug after update to Java 1.6.0.45 on Mac OS X
		Added installation instructions to the Linux distribution
		Fixed incorrect server IP address when starting via UMS.exe or UMS.bat
	External Components:
		Updated FFmpeg for all operating systems, which:
			Fixes RTMP bugs
			Improves support for the following codecs/containers: WMV, MPEG, H.264, Matroska, AVI, AAC, AC-3, WMA, MP3, FLV, OGG
			Improves 60FPS video support
			Supports more audio channel layouts
			Improves pthread support
			Fixes memory leaks
			Improves subtitle decoding support
			Adds support for subtitles character encoding conversion
			Makes duration estimation more reliable
			Adds support for .ape files
			Improves memory use when using hardware acceleration
			Fixes multithreaded MPEG-4 decoding
		Updated Java Runtime Environment automatic downloader for Windows to 7u21, which:
			Has lots of security fixes and improvements

2.6.1 - 2013-04-16

	General:
		Added option to toggle H.264 remuxing in FFmpeg
		Improved stability of Live Subtitles
		Re-enabled WMP detection
		Speed improvements
		Fixed transcoding to Xbox via FFmpeg
		Added support for WTV files
		A-to-Z virtual folders support sorting options
		Fixed a crash with rare MP3 files
		Allow subtitles to be disabled on Samsung TVs
	Renderers:
		Improved Sony Blu-ray Player support (thanks, skeptical!)
		Fixed exception on Android
		Fixed stuttering issues with Bravia HX TVs
	Languages:
		Updated Dutch translation (thanks, leroy!)
		Updated Italian language (thanks, nocciola82!)
	External Components:
		Updated FFmpeg for Linux to burek's 2013-04-04 build, which:
			Is compiled statically

2.6.0 - 2013-03-29

	General:
		Added "Live Subtitles" which means you can select subtitles from the Internet via your device. It can be enabled on the Navigation/Share Settings page.
		Improved layout of the Navigation/Share Settings page
		Implemented default subtitle preferences, to use them:
			Either use the "Clean Install" option on Windows or put "eng,off;*,eng;*,und" into the "Audio/subtitles language priority" box in the "Subtitles settings" tab on the Transcoding Settings page
		Fixed DTS muxing with FFmpeg
		Added new "precoder" functionality, useful for plugin developers
		Added H.264 encoding support to MEncoder (when renderers specify it)
	Languages:
		Updated Czech translation
		Updated English settings labels
		Updated Simplified Chinese translation (thanks, lovenemesis!)
	External Components:
		Updated h2 database to 1.3.171, which:
			Increases speed
			Improves JDK 7 compatibility
		Updated MPlayer/MEncoder for Windows to SB52, which:
			Improves audio sync
			Improves DVD support
			Supports filenames with uncommon characters
			Fixes memory leaks
			Improves sync with Real videos
			Fixes a crash
		Updated Netty to 3.6.3, which:
			Makes browsing more stable

2.5.2.2 - 2013-03-19

	General:
		Reverted maximum memory increase

2.5.2.1 - 2013-03-18

	General:
		Fixed the startup error introduced in 2.5.2

2.5.2 - 2013-03-18

	General:
		Offer to automatically update Java on Windows from 6 to 7
		Fixed library updating with cache enabled
		Made detection of network speed more accurate (thanks, ExSport!)
		Library scanning interface improvements
		Set a higher maximum memory by default via the Windows installer for computers with 4GB+ of RAM

2.5.1 - 2013-03-15

	General:
		Improved subtitle support on non-PS3 renderers
		Made library/file loading faster
		Fixed 24-bit flac support with tsMuxeR
		Stopped using 2 database locations for media caching on Windows
		Allow library scanning to be stopped
		Library scanning interface improvements
	Renderers:
		Added support for Sony Home Theatre systems
		Added support for Onkyo TX-NR717
		Improved Samsung AllShare compatibility

2.5.0 - 2013-03-05

	General:
		Updated layout on the Transcoding Settings tab
		Improved aspect ratio handling on Panasonic and Sony TVs
		Enabled ASS/SSA subtitle support on Linux by default
		Now compiled with Java 7
		Optimized code for Java 7
		The installer only tells Windows to run UMS at startup on new installs or if the user has specified it in UMS
		Added RTMP support to FFmpeg Web Video engine
		Fixed fontconfig support on OS X
		FFmpeg can transcode to x264 with the renderer option TranscodeVideo=H264TSAC3
		FFmpeg supports video muxing
		Made muxing more reliable
		Improved audio sync in FFmpeg
		Improved FFmpeg buffering
		Bandwidth limit is more accurate with FFmpeg
	Renderers:
		Added support for KalemSoft Media Player on BlackBerry PlayBook devices
		Added support for Netgear NeoTV
		Added support for Telstra T-Box
		Added support for Yamaha RX-3900
		Improved support for Sony Blu-ray players
	Languages:
		Made languages more consistent with eachother
		Updated Russian translation (thanks, Tianuchka!)
		Changed default audio/subtitles language priority for English users, which:
			Disables subtitles when audio is English, otherwise look for English subtitles, prioritizing external before internal subtitles
		Added language flags for Arabic, Croatian, Estonian, Latvian, Serbian and Vietnamese
	External Components:
		Updated FFmpeg for Windows and Linux to SB8, which:
			Increases x264 encoding speed
		Updated InterFrame to 2.5.0, which:
			Improves scene-change detection
			Minimizes artifacts
		Updated Java Runtime Environment automatic downloader for Windows to 7u17, which:
			Fixes serious security holes
		Updated MediaInfo for Windows to 0.7.62

2.4.2 - 2013-02-17

	General:
		Fixed bug on some renderers where no files/folders were showing
		The Clean Install option on Windows deletes the MPlayer Contconfig cache
	External Components:
		Updated FFmpeg for OS X to 1.1.2, which:
			Adds automatic multithreading
			Improves QuickTime format support
			Supports decoding WMA Lossless
			Supports decoding RealAudio Lossless
			Fixes security issues
			Fixes over 150 bugs
			Supports RTMP
			Supports Opus
			Supports encoding external subtitles
			Supports decoding DTS-HD

2.4.1 - 2013-02-15

	General:
		Improved autostart support
	Renderers:
		Added support for Sharp Aquos TVs
		Added support for Showtime 4
	External Components:
		Updated MPlayer/MEncoder for Windows and Linux to SB50, which:
			Silences meaningless errors
			Supports 32-bit Linux installations
		Updated FFmpeg for Windows and Linux to SB7, which:
			Adds RTMP support
			Supports 32-bit Linux installations

2.4.0 - 2013-02-10

	General:
		FFmpeg supports external subtitles
		Linux build includes MPlayer, MEncoder and FFmpeg binaries like the other versions always have
		Fixed user setting to automatically load external subtitles
		Audio/subtitle language priority is now blank by default
		Improved RealVideo file support
		Added log level selector to the Logs tab
		Improved MP4 compatibility on PS3
		The word "the" at the start of filenames is ignored while sorting by default
		Program runs on Windows startup by default, can be changed on the General Configuration tab
		Fixed support for the Channels plugin
	Languages:
		Updated Czech translation
		Updated Russian translation (thanks, Tianuchka!)
		Made more things translatable
	External Components:
		Added MPlayer/MEncoder SB49 for Linux
		Added FFmpeg SB6 for Linux
		Updated Java Runtime Environment automatic downloader for Windows to 7u13, which:
			Fixes serious security holes
		Updated FFmpeg for Windows to SB6, which:
			Enables external subtitles

2.3.0 - 2013-01-27

	General:
		FFmpeg supports audio selection
		Improved MKV/MP4 support on PS3
		Fixed rare bug where files stop half way through
		Support streaming mp4 to WD TV Live
		Added initial support for Vizio Smart TVs
		Fixed playback on unknown renderers
		Fixed several FFmpeg-related bugs
		Improved support for videos whose containers change aspect ratios
		Tried to fix headless mode detection on Ubuntu
		Fixed various bugs
	External Components:
		Updated FFmpeg for Windows to SB5, which:
			Improves AC-3 audio buffering
			Fixes memory leaks
			Fixes a bug which detected transport streams as finished when they weren't
			Improved MPEG-PS encoding
			Made error codes more meaningful
			Improves Matroska (MKV) support
			Improves threading
			Optimized AC-3 decoding
		Updated InterFrame to 2.4.0, which:
			Improves scene-change detection

2.2.6 - 2013-01-21

	General:
		Some renderers (like Philips HTS) can connect more quickly with the server
		Improved support for Sony Bravia HX series TVs
		Improved design on OS X
		Fixed FFmpeg video transcoding on Xbox
		Fixed file permissions on Linux
		Plugins can use custom icons for files (thanks, skeptical!)
	Languages:
		Updated Korean translation (thanks, sunghyuk!)
	External Components:
		Updated MPlayer/MEncoder for Windows to SB49, which:
			Improves MP3 encoding speed
			Improves MKV support
			Improves threading
			Fixes memory leaks

2.2.5 - 2013-01-16

	General:
		Fixed transcoding support on some renderers (thanks for testing, Raker13!)
		Merged the Video Settings folder into the Server Settings folder
		Fixed a bug with the cache (thanks, valib!)
		Several code optimizations (thanks, valib!)
	External Components:
		Updated Java Runtime Environment automatic downloader for Windows to 7u11, which:
			Fixes serious security holes

2.2.4 - 2013-01-09

	General:
		Prevents internal and external subtitles from showing at the same time
	External Components:
		Updated MPlayer/MEncoder to SB48, which:
			Runs faster
			Crashes less
			Detects framerates more accurately
			Fixed subtitle bug on certain CPUs
			Improved audio/video sync
			Fixed memory leaks
			Improves AVI support
			Fixed audio stuttering/repeating bug
			Fixed alpha for ASS subtitles
			Improves permissions handling on Windows
			Removes incorrectly categorised fonts
			Makes ASS/SSA subtitle rendering up to 3.5x faster
			Improves AC-3 audio buffering
			Fixes a bug which detected transport streams as finished when they weren't
			Improved MPEG-PS encoding

2.2.3 - 2013-01-07

	General:
		Temporarily rolled back MPlayer/MEncoder for Windows to SB42 to fix playback bugs

2.2.2 - 2013-01-03

	General:
		Improved support of many files, especially on non-PS3 renderers
		Fixed AVI support on Panasonic TVs
		Cleaned up the "serving" text at the bottom of the program
		Fixed conditional horizontal scrollbar
		More accurately determine which formats tsMuxeR supports
	External Components:
		Updated InterFrame to 2.3.0, which:
			Increased speed
			Improved quality in high-action scenes
			Optimised memory use
			Supports more video cards
		Updated MPlayer/MEncoder to SB47, which:
			Disabled direct rendering for non-ref frames only again
			Fixes a bug which detected transport streams as finished when they weren't

2.2.1 - 2012-12-21

	General:
		Improved MEncoder audio sync
		Improved TS video support on PS3
		Installer offers to automatically close UMS if it is running
		Updated JRE auto-download to 7u10
		Fixed tsMuxeR support on non-PS3 renderers
		Improved MediaInfo support
		More files work on Panasonic TVs
		Updated images for PS3 and Panasonic TV renderers
	External Components:
		Updated MPlayer/MEncoder to SB46, which:
			Makes ASS/SSA subtitle rendering up to 3.5x faster
			Improves sync for files with negative timestamps
			Improves AC-3 audio buffering
			Fixes memory leaks

2.2.0 - 2012-12-11

	General:
		Added option that creates virtual A-Z subfolders in folders with a lot of files (the last option on the Navigation/Share Settings page)
		Added option to the Windows installer to perform a "clean install", which removes all configuration files before installing
		Design and usability improvements
		Logging improvements
		Fixed audio/subtitle priority defaults
		"Definitely disable subtitles" is more reliable
		Fixed FFmpeg Web Video streaming
		Fixed DTS support in FFmpeg
		Improved FFmpeg speed
		Added support for creating thumbnails from TIFF and other formats
		Fixed numerous smaller bugs
		Made thumbnail generation and browsing faster
		Don't show the text "{External Subtitles}" if the display name is "[No Encoding]" within the transcode folder
		Added support for True Motion and convertfps to AviSynth/FFmpeg engine
		Made multithreading more stable with AviSynth/FFmpeg engine
		Fixed RTL subtitle support
		Improved stability while seeking and transcoding
		Fixed custom MEncoder settings at renderer level
		Added a check to the Windows installer that prevents installation until UMS is closed
	Languages:
		Minor updates to all languages
		More text is translatable
		Updated Brazilian
		Updated Czech
	Renderers:
		Support more Android players (thanks, ExSport!)
		Improved support for Panasonic TVs (thanks, ExSport!)
	External Components:
		Updated MPlayer/MEncoder to SB45, which:
			Fixed audio stuttering/repeating bug
			Fixed alpha for ASS subtitles
			Improves permissions handling on Windows
			Removes incorrectly categorised fonts
		Updated FFmpeg to SB4, which:
			Improves audio sync when seeking
			Supports more rare avi files
			Improves support for demuxing DTS-HD
			Fixes dozens of memory leaks
			Improves audio sync for some AVI files using MP3 audio
			Improves FPS detection
			Improved sync for interlaced video
			Allows mid-stream channel layout change for flac audio
			Supports 24-bit flac encoding
			Improves support for some CPU-optimisations
			Fixed a lot of bugs with the implementation of h264

2.1.2 - 2012-12-01

	General:
		Fixed subtitle support for RTL languages (Arabic, Persian, etc.)
		Updated MPlayer/MEncoder to SB44, which:
			Fixed subtitle bug on certain CPUs
			Improved audio/video sync in some cases
			Fixed memory leaks
			Improves AVI support

2.1.1 - 2012-11-25

	General:
		Fixed a bug preventing UMS from starting
		Improved Plugin Management page design
		Minor language update

2.1.0 - 2012-11-23

	General:
		Enabled HiDPI for retina displays
		Fixed a bug that prevented showing Galaxy Nexus pictures
		Allow plugins more freedom on install
		Logging improvements
		Stop Windows Media Player from being detected because it has never been supported
		Language clarifications
		Updated Dutch Traditional translation (thanks, leroy!)
		Added support for all 3D SBS videos
		Fixed blocky video with some files
		Fixed bug where the program would not start without an internet connection
		Checks for VSFilter (AviSynth subtitles plugin) in K-Lite Codec Pack
		Fixed crash when scanning some MP3 files with large cover images on Linux (#22)
		Added support for external subtitles with the AviSynth/MEncoder engine
		Stopped virtual folder names being cut off after periods
		Fixed several rare crashes
		Renamed the Traces tab to Logs
		Made text and buttons on the Logs page more readable
	Plugin Management:
		Fixed and improved credentials management
		Improved Plugin Management tab design
		Buggy plugins no longer logspam
	Renderers:
		Added support for Sony Bravia EX620 TVs (thanks, morisato!)
	External components:
		Updated H2 Database Engine to 1.3.169, which:
			Makes library scanning faster
		Updated InterFrame to 2.2.0, which:
			Has less artifacts in the interpolated frames
		Rolled back MediaInfo on OS X to 0.7.58, which:
			Fixes a bug that caused all videos to be transcoded instead of streamed
		Updated MPlayer/MEncoder to SB43, which:
			Runs faster
			Crashes less
			Leaks memory less
			Detects framerates more accurately
			Improves audio sync

2.0.0 - 2012-10-31

	General:
		Design facelift
		Added support for more file archive formats (7-Zip, gzip and tar)
		Improved DVD and ISO support
		Fixed ASS/SSA subtitle position when using overscan compensation
		Fixed a rare bug where videos played at half-speed
		Updated JRE auto-download to 7u9 (latest)
		Admin permissions notifications work on Windows 8 (before they only worked on 7 and Vista)
		Fixed the FFmpeg Audio engine
		Updated English settings/descriptions
		Made some log messages more descriptive
		MEncoder's "A/V sync alternative method" option is applied correctly again
		Fixed error while loading iPhoto library
		Updated many program dependencies to benefit from many bugfixes
		Fixed bug where disabled engines would be picked
		Updated links on the Help page
		Made renderer config layouts more synchronised
		Added a button to uninstall the Windows service
		Removed fontconfig from MEncoder for Mac OS X for improved subs compatibility
		Made names in the transcode folder shorter by removing redundant information
		The user preference to hide file extensions is applied to the transcode folder subfolder names
	External components:
		Updated MPlayer/MEncoder to SB42, which:
			Runs faster
			Improves audio/video sync
			Faster handling of ASS/SSA subtitles
			Improves DVD support
		Updated FFmpeg to SB3, which:
			Fixes memory leaks
			Fixes other bugs
		Updated MediaInfo to 0.7.61, which:
			Fixes MKV framerate detection bug
			Fixes E-AC-3 duration detection bug
			More reliable DTS bitrate detection
		Updated Java Service Wrapper to 3.5.15, which:
			Improves Windows permissions handling
	Renderers:
		Added support for Sony Bravia HX800 TVs (thanks, lelin!)
		Enabled streaming more file formats to Android devices
		Improved DivX support on Panasonic devices (thanks, ExSport!)
	Plugin Management:
		Created new "Plugin Management" tab
		Added button to edit the plugin credential file
		Improved plugin installation process
		Added button to reload available plugin list
		Updated author column of plugin installer to include maintainers
		Added "version" column to plugin installer
	Languages:
		Turkish flag added for the transcode folder
		Updated settings labels for all languages

1.6.0 - 2012-10-01

	General:
		MEncoder and tsMuxeR no longer produce stretched audio with some videos
		Improved handling of initialization errors
		Updated h2 database to 1.3.168 (thanks, valib!)
		Changed audio/subtitle defaults to accept anything
		We no longer display language options in the transcode folder for engines that don't accept them
		Renamed the FFmpeg/AviSynth engine to AviSynth/FFmpeg
		The AviSynth/FFmpeg engine works again
	FFmpeg:
		Added DTS output support
		Video quality settings fixed
		Now respects the "remux AC-3" setting instead of always remuxing AC-3
	Renderers:
		Added support for Samsung SMT-G7400 (UPC Horizon)
	Languages:
		Made English language settings more accurate
		Updated Chinese Traditional translation (thanks, mcc!)

1.5.2 - 2012-09-27

	General:
		Temporarily rolled back MPlayer/MEncoder to SB37, which fixes DVD support
		Updated FFmpeg to SB2, which fixes seeking bugs
	Plugin Installer:
		Added Description column
		Added "run as administrator" reminder for Windows 7 and Vista users
		Installation progress window is centered
		Disabled manual row-editing
		Customised the column widths so all text is visible
	Languages:
		Removed deprecated part of Russian translation

1.5.1 - 2012-09-19

	General:
		FFmpeg and FFmpeg/AviSynth engines now use the "Video quality settings" from the "Common transcode settings" page
		Many improvements to the layout of settings
		Updated FFmpeg to 8bdba0b (20120914)
		Updated MPlayer and MEncoder for Windows to SB38, which:
			Enables more CPU optimisations (runs faster)
			Improves audio sync
			Tries to fix very occasional freezing issue
	Languages:
		Updated all languages

1.5.0 - 2012-09-04

	General:
		Made videos adhere more exactly to the maximum bandwidth limit
		Updated MPlayer/MEncoder to SB37, changelog: http://www.spirton.com/mplayer-mencoder-sb37-released/
		Updated MediaInfo to 0.7.60, changelog: http://mediainfo.sourceforge.net/Log
		Files are now sorted alphanuerically by default, e.g. Episode 2 before Episode 10
		#--TRANSCODE--# folder name is localized
		Cleaned up FFmpeg and MEncoder commands
		Use automatic enabling of multithreading with FFmpeg and FFmpeg/AviSynth engines
		Many improvements to UMS.conf and how it responds to updates, it is recommended to delete your old one
		Only use tsMuxeR to compensate for MEncoder ignoring audio delay when A/V sync alternative method is enabled (which it is by default)
		Fixed bugs in plugin installer
		Automatic encoding detection of non UTF-8 external subtitles for Russian, Greek, Hebrew, Chinese, Korean and Japanese languages (leave "Subtitles codepage" option blank)
		Improved handling of UTF-8 external subtitles
		Prevented image distortion on some DLNA clients with "Keep AC-3 track" option enabled
		Don't show entries for disabled engines in #--TRANSCODE--# folder
		Allow Traces tab panel to auto-scroll (thanks, LordQuackstar!)
		Replaced ImageMagick with Thumbnailator for thumbnail generation
		Fix FFmpeg engine's handling of unsupported custom options (thanks, ajamess)
		Fixed FFmpeg command line arguments used by tsMuxeR video
		Fixed DNLA 2114 errors when scanning non-readable subfolders with "hide empty folders" enabled
		Linux tarball: fix "cannot find tsMuxeR" error
		Fixed numerous small bugs
	Renderers:
		Added Sony SMP-N100 
		Added Yamaha RX-A1010 (thanks, merelin)
		Deprecated misnamed TranscodeVideo profile MPEGAC3: use MPEGPSAC3 instead
		Documented and cleaned up TranscodeVideo and TranscodeAudio profiles
	FFmpeg:
		Updated FFmpeg to a366bea (20120827)
		Follow the maximum bandwidth setting
		Mux AC3 instead of transcoding it
		Use better commands
	MEncoder:
		Disabled AC-3 remux if audio track has delay, which improves audio sync
		Disabled DTS and LPCM remuxing if tsMuxeR engine is unavailable
	Languages:
		Updated Bulgarian translation (thanks, JORDITO)
		Updated Dutch translation (thanks, leroy)
		Updated Russian
		Updated English settings labels

1.4.0 - 2012-07-18

	General:
		Many improvements to FFmpeg, from audio sync to file-support to stability
		Updated MPlayer and MEncoder for Windows to SB36, which:
			Supports a lot more file formats and colour-spaces
			Improves audio/video sync, especially with PAL (25FPS) videos
		Added FFmpeg multithreading option
		Updated FFmpeg for OS X to 57d5a224
		Added a GUI config editor for those who like to edit manually
		Improved audio/video sync when using MEncoder
		Improved audio channel detection
		Fixed support for some plugins
		Added support for TX3G (MPEG-4 Timed Text) subtitles
		Added support for WebM videos downloaded from YouTube
		Fixed DV video detection
		Fixed "Definitely disable subtitles" option with ASS/SSA subtitles
		Fixed default audio/subtitle priority options
		Fixed running on headless servers
		Windows 7 and Vista users are reminded to run as administrator before attempting to automatically update
		Fixed multithreading bug with MEncoder on Linux
		Made layout more consistent
	Plugin Installer:
		Added Plugin Installer which lets you automatically browse plugins and install them, see this page for details: http://www.universalmediaserver.com/forum/viewtopic.php?f=8&t=152
	Web:
		Added new default web video engine: FFMpeg Web Video
		Updated WEB.conf with working default video streams
		Added support for new web protocols: mmsh:// and mmst://
		Added The Onion to the default video feeds
	Languages:
		Updated Simplified Chinese (thanks, lovenemesis!)
		Updated Czech (thanks, valib!)
		Updated English
		Added image for Hebrew subtitle choosing

1.3.0 - 2012-07-09

	General:
		Enabled FFmpeg video player for all platforms
		Updated FFmpeg for Windows to e01f478 (20120319)
		Updated 32-bit MediaInfo to the 20120611 development snapshot which fixes a bug with detecting the duration of some avi files
		Improved FFmpeg commands
		Added support for PGS (Blu-ray) subtitles
		Added support for VobSub (DVD) subtitles in MP4 container
		Better handling of embedded ASS/SSA subtitle styling
		Fixed audio track selection for MP4 and MOV containers
		Localized audio and subtitle priority defaults
		Added option on Traces tab to pack useful debug information into a zip file
		Fixed 64-bit OS detection for Windows and OS X
		Made program-closing more reliable
		Fixed default settings
	Languages:
		Updated Catalan (thanks, aseques)
		Improved English

1.2.1 - 2012-06-30

	General:
		Improved video buffer stability
		The default versions of UMS.conf and WEB.conf are now put in ProgramData on Windows (thanks to vulcan for reporting this bug)
		Updated UMS.conf
		Updated renderer.conf creation instructions
		Fixed AviSynth/FFmpeg crash
		Improved AviSynth/FFmpeg engine code
		Enabled convertfps by default
		Made the AviSynth script instructions separate to the input box
		Minor design improvements to the Transcoding tab
		Made True Motion smoother
		Assorted bugfixes
	Languages:
		Updated Catalan
		Updated English

1.2.0 - 2012-06-26

	General:
		Create thumbnails from 2 seconds into the video by default
		Fixed silent installation
		Updated MPlayer and MEncoder for Windows to SB35, which:
			Supports more colours in ASS/SSA subtitles
			Fixes memory leaks
			Starts faster
			Improves fonts
			Improves caching
			Improves multithreading stability
		Updated MPlayer and MEncoder for OS X to SB32
		Lots of code optimisations
		Made program restarts more reliable
		Fixed AviSynth ConvertFPS option
		Improved AviSynth frame-serving stability
	Renderers:
		Added support for DirecTV HR series (thanks, DeFlanko!)
		Added workaround for 2.0 384 kbits AC3 PS3 audio bug (PMS issue #230 & #1414)
		Improved support for Panasonic TVs
	Installer:
		Only offer to run AviSynth installer if it isn't already installed
	Languages:
		Updated Czech

1.1.1 - 2012-06-14

	General:
		Fixed memory buffer handling
		Added LPCM transcoding option for all audio tracks
		Added DTS HD-MA support up to 7.1 channels: DTS core remux, LPCM and AC3 transcoding
		Added Dolby TrueHD support up to 7.1 channels: LPCM (recommended) and AC3 (buggy for 7.1) transcoding
		Added support for more rare audio formats
		Updated documentation in UMS.conf
	Languages:
		Improved English settings labels
		Fixed spacing across all languages on the "Common transcode settings" page
		Improved Russian translations
		Removed outdated translations
		Fixed display of maximum transcode buffer size in 5 languages (ca, es, is, it, sv)
		Improved support for RTL languages

1.1.0 - 2012-06-09

	General:
		Made program restarts more reliable
		Fixed bug that occurred when resetting cache
		Made buffer display in megabytes instead of bytes
		Updated MediaInfo to 0.7.58
		Branding
		Fixed overscan compensation bug on some renderers (thanks to tMH for reporting the bug!)
		Improved OS X tray icon (thanks, glebb!)
		Added workaround for folder depth limits
		Use UMS_PROFILE instead of PMS_PROFILE
		Added support for audio that is 48Hz/24Hz
		Fixed settings folder permissions
	Renderers:
		Added Yamaha RX-V671 (thanks, adresd!)
		Added LG Smart TV Upgrader (600ST)
		Added OPPO BDP-83 (thanks, counsil!)
		Added OPPO BDP-93 (thanks, counsil!)
		Added Sony Bravia 5500 series image
		Added Sony Bravia EX series image
		Added Panasonic TV image
		Improved D-Link DSM-510 image
		Improved Sony Blu-ray Player image
		Improved Xbox 360 image
		Improved Philips Streamium support
		Fixed support for Samsung 2012 TVs (thanks, trooperryan!)
		Numerous small improvements
	AviSynth True Motion (InterFrame):
		Supports more video cards
		Improved stability
		Improved compatibility with AviSynth 2.6
	Languages:
		Made Status tab more consistent across languages
		Removed outdated translations
	Installer:
		Option to install AviSynth 2.6 MT
		Option to set custom memory limit

1.0.1 - 2012-06-03

	Fixed a bug where XBOX 360 couldn't see the server (thanks to cmonster and Secate for testing)
	Lots of language updates and fixes
	AviSynth support was broken for some languages
	Fixed an iPhoto bug
	More branding
	Code improvements

1.0.0 - 2012-05-31

	Renamed and rebranded to Universal Media Server
	Updated MPlayer and MEncoder to SB34
	Updated MediaInfo to 0.7.57
	Enabled automatic updating
	Fixed NPE when toggling HTTP Engine V2
	Fixed global custom MEncoder options
	Fixed startup for symlinked UMS.sh (thanks, Matthijs!)
	Added documentation to UMS.conf (thanks, Hagar!)
	Updated JNA to support multiarch directories on Linux
	Added GUI support for right-to-left languages
	Language updates:
		- Added Arabic
		- Updated Czech
		- Added Hebrew (thanks, zvi-yamin!)
		- Updated English
	Renderer updates:
		- Added D-link DSM-510 (thanks, glenrocks!)
		- Added AcePlayer app (thanks, md.versi!)
		- Fixed Samsung 2012 TVs timeout (thanks, troop!)
	Added option to select and force the default renderer
