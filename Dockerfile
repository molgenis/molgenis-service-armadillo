FROM --platform=linux/amd64 eclipse-temurin:17.0.9_9-jdk-focal
VOLUME /data
VOLUME /config
VOLUME /logs

ARG JAR_FILE
EXPOSE 8080
COPY ${JAR_FILE} /app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-DSPRING_CONFIG_ADDITIONAL_LOCATION=/config/application.yml", "-jar","/app.jar"]
