FROM --platform=linux/amd64 eclipse-temurin:17.0.10_7-jdk-focal
VOLUME /data
VOLUME /config
VOLUME /logs

ARG JAR_FILE
EXPOSE 8080
COPY ${JAR_FILE} /app.jar
ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -DSPRING_CONFIG_ADDITIONAL_LOCATION=/config/application.yml -jar /app.jar 2>&1 | tee /logs/armadillo.log"]
