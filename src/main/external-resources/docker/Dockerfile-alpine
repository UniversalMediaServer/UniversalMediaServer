FROM alpine

LABEL org.opencontainers.image.title="Universal Media Server"

RUN apk add --no-cache coreutils ffmpeg mediainfo openjdk17-jre iputils mplayer ttf-dejavu

WORKDIR /profile

COPY ./profile /profile

COPY ./app/UMS.conf /profile

WORKDIR /ums

COPY ./app /ums

VOLUME /media

VOLUME /profile

EXPOSE 1900/udp 5001/tcp 5353/udp 9001/tcp 9002/tcp

ENV UMS_PROFILE=/profile

ENV JDK_JAVA_OPTIONS="-Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true -Djna.nosys=true"

ENTRYPOINT [ "java", "-jar", "ums.jar" ]
