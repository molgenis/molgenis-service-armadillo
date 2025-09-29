#FROM --platform=linux/amd64 eclipse-temurin:17.0.12_7-jdk-focal
FROM --platform=linux/amd64 eclipse-temurin:21-jre-alpine
VOLUME /data
VOLUME /config
VOLUME /logs
ENV org_xerial_snappy_disable=true


RUN apk update && apk add --no-cache libc6-compat \
ln -s /lib/libc.musl-x86_64.so.1 /lib/ld-linux-x86-64.so.2

ARG JAR_FILE
EXPOSE 8080
COPY ${JAR_FILE} /app.jar
ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -DSPRING_CONFIG_ADDITIONAL_LOCATION=/config/application.yml -jar /app.jar 2>&1 | tee /logs/armadillo.log"]
