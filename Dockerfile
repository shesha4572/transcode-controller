FROM maven:latest as builder

WORKDIR /transcode-controller
COPY . .
RUN mvn clean install -Dmaven.test.skip=true

FROM amazoncorretto:17-alpine
WORKDIR /transcode-controller
COPY --from=builder /transcode-controller/target/*.jar app.jar
RUN apk update
RUN apk add --no-cache ffmpeg
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
