FROM gradle:9-jdk25-alpine AS build

WORKDIR /home/gradle/project

COPY build.gradle settings.gradle gradlew* ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon build || return 0
COPY . .
RUN ./gradlew clean build --no-daemon -x test
RUN mv build/libs/*[!-plain].jar /app.jar


FROM debian:bookworm-slim AS ffmpeg
WORKDIR /tmp
RUN apt-get update && apt-get install -y curl xz-utils && \
    curl -sL https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz -o ffmpeg.tar.xz && \
    tar -xf ffmpeg.tar.xz && \
    mv ffmpeg-*-amd64-static /ffmpeg && \
    rm -rf /var/lib/apt/lists/* /tmp/*

FROM openjdk:25-ea-1-bookworm AS service
LABEL authors="anisekai"

WORKDIR /app

COPY --from=ffmpeg /ffmpeg /opt/ffmpeg
COPY --from=ffmpeg /ffmpeg/ffmpeg /usr/bin/ffmpeg
COPY --from=ffmpeg /ffmpeg/ffprobe /usr/bin/ffprobe

COPY --from=build /app.jar .

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
