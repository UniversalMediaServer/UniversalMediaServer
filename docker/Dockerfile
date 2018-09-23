FROM alpine

MAINTAINER Anand Tamariya <atamariya@gmail.com>

RUN apk add --no-cache ffmpeg mediainfo openjdk7-jre 

WORKDIR /usr/src/ums

VOLUME /media

VOLUME /profile

EXPOSE 9001 5002 1044

CMD java -DUMS_PROFILE=/profile -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true -Djna.nosys=true -cp ums.jar net.pms.PMS 

COPY . /usr/src/ums

