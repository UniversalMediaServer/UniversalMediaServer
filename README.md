# Universal Media Server Fork

This fork is based on [Universal Media Server](https://www.universalmediaserver.com).

Universal Media Server is a DLNA, UPnP and HTTP/S Media Server.
It is capable of sharing video, audio and images between most modern devices.
It was originally based on PS3 Media Server by shagrath, in order to ensure greater stability and file-compatibility.

### Why is the fork needed

The current implementation of Universal Media Server (UMS) hasn't migrated all needed features for the best integration scenarios with other control points like [NextCP/2](https://sf666.github.io/nextcp2/overview/overview/), a control point focusing on audio user experiance. Furthermore, it is primarily optimized for video playback on TVs in a 2-box setup (since version 14), because of the UUID authorization impementation. This fork aims to address these limitations by introducing enhancements and new functionalities tailored to the needs of audiophile users.


Major improvement over the original implementation is an accurate renderer identification in a three-box setup — a configuration widely used by users who prioritize high - quality audio playback. With this enhancement, you can control your UPnP streaming devices through your preferred control point applications, such as BubbleUPnP, DENON HEOS App or the LINN App. The correct renderer configuration will be used, no matter to which renderer device you stream the content.


> [!CAUTION]
> To support the 3-box setup, the UMS "authorization" feature has been disabled on the server side. The UI let's you still configure it, but it will be ignored.

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


## Docker installation 

The quickest way to install this fork is by using a Docker container. This guide assumes that Docker is already installed on your system. If not, please refer to the official Docker documentation for installation instructions.

The following example uses `docker compose` to set up this fork of Universal Media Server (UMS) in a single command.

Create a `docker-compose.yml` file in your project directory and add the following content:

```yaml
services:
  ums:
    container_name: ums
    network_mode: host
    restart: unless-stopped
#    user: '1009:1003'
    volumes:
      - [MEDIA_VOLUME]:/media/music
      - ums_public:/profile
    environment:
      JAVA_TOOL_OPTIONS: '-Xms2048m -Xmx4096m -XX:+UseShenandoahGC -Xbootclasspath/a:/ums/web/react-client -Dums.profile.path=/profile -Dfile.encoding=UTF-8'
    image: ik6666/ums:latest

volumes:
  ums_public:
    external: true
```

Replace `[MEDIA_VOLUME]` with the path to your media files on the host system. This will mount the media directory into the UMS container. For example, if your media files are located at `/path/to/your/music`, replace `[MEDIA_VOLUME]` with `/path/to/your/music`.
If you want the container to run under a specific user ID and group ID, make sure to uncomment and adjust the `user` property in the `ums` service section. The example uses `1009:1003`, which you can change to match your system’s user and group IDs.

Create the external volumes `ums_public` if they do not exist yet. You can do this by running the following commands:

```bash
docker volume create ums_public
```

Check if the volumes were created successfully:

```bash
docker volume ls
``` 

The docker volumes are used to persist data across container restarts. The `ums_public` volume is used by UMS to store its configuration and its data.
If you want to use a different volume name, make sure to update the `docker-compose.yml` file accordingly.

To start the containers, run the following command in the directory where your `docker-compose.yml` file is located:

```bash
docker-compose up -d
```

To stop the containers, run:

```bash
docker-compose down
```

There is also a [docker installation example](https://sf666.github.io/nextcp2/quick_install/docker/) if you want to use this fork together with NextCP/2 control point in the same environment.