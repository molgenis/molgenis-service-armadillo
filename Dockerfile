FROM eclipse-temurin:17.0.9_9-jdk-focal
VOLUME /data
ARG JAR_FILE
EXPOSE 8080
COPY ${JAR_FILE} /app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-DSPRING_CONFIG_ADDITIONAL_LOCATION=/application.yml", "-jar","/app.jar"]
