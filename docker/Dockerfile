FROM ubuntu:22.04

RUN apt-get update
RUN apt-get install curl tar locales zip unzip vim coreutils -y
RUN sed -i '/en_US.UTF-8/s/^# //g' /etc/locale.gen \
    && locale-gen 
ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8

RUN rm /bin/sh && ln -s /bin/bash /bin/sh
RUN curl -s "https://get.sdkman.io" | bash
RUN /bin/bash -c "source /root/.sdkman/bin/sdkman-init.sh; sdk install java 21.0.6-tem"

ENV SDKMAN_DIR "/root/.sdkman"

RUN apt-get update
RUN apt-get install fonts-dejavu ffmpeg mediainfo iputils-ping mplayer -y

WORKDIR /usr/src/ums/

VOLUME /media

VOLUME /profile

EXPOSE 9001 5002 1044

COPY . /usr/src/ums/

ENV JAVA_TOOL_OPTIONS="-Xms2048m -Xmx4096m -XX:+UnlockExperimentalVMOptions -Xbootclasspath/a:/UniversalMediaServer/web/react-client -XX:+UseShenandoahGC -Dums.profile.path=/profile -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true -Djna.nosys=true"

ENTRYPOINT ["/bin/bash", "-c", "source /root/.sdkman/bin/sdkman-init.sh; java -jar ums.jar"]

