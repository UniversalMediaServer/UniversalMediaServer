# Universal Media Server Fork

This fork is based on ![Universal Media Server CI](https://github.com/UniversalMediaServer/UniversalMediaServer/workflows/CI/badge.svg) [![Crowdin](https://badges.crowdin.net/universalmediaserver/localized.svg)](https://crowdin.com/project/universalmediaserver)

[<img align="right" src="https://github.com/UniversalMediaServer/UniversalMediaServer/blob/main/src/main/resources/images/logo.png?raw=true" alt="Universal Media Server" width="256" height="auto"/>][1] Universal Media Server is a DLNA, UPnP and HTTP/S Media Server.
It is capable of sharing video, audio and images between most modern devices.
It was originally based on PS3 Media Server by shagrath, in order to ensure greater stability and file-compatibility.

### Why is the fork needed

The current implementation of Universal Media Server (UMS) hasn't migrated all needed features for the best integration scenarios with [NextCP/2](https://github.com/sf666), a control point focusing on audio user experiance. Furthermore, it is primarily optimized for video playback on TVs in a 2-box setup (since version 14), because of the UUID authorization impementation. This fork aims to address these limitations by introducing enhancements and new functionalities tailored to the needs of audiophile users.

Major improvement over the original implementation is an accurate renderer identification in a three-box setup — a configuration widely used by users who prioritize high - quality audio playback. With this enhancement, you can control your UPnP streaming devices through your preferred control point applications, such as BubbleUPnP or the LINN App. The correct renderer configuration will be used, no matter to which renderer device you stream the content.

ATTENTION: To support the three-box setup, the UMS "authorization" feature has been disabled on the server side. The UI let's you still configure it, but it will be ignored.


## Additional features supported by UMS fork

The features listed below are supported on the server side. This means you don't have to copy your files between your media library and you client machine or mount a network drive. The UMS fork handles all the actions initiated by nextCP/2 on the server side!

  - Like Music Albums
  - Star Rating Support
  - Resources Rating
  - Server Side Playlist support
    - create new playlists
    - delete playlists
    - add songs to playlist
    - remove songs from playlist
  - Album art updates for resources (i.e. internet radio station) 

  [1]: https://www.universalmediaserver.com
