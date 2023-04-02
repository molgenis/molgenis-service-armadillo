FROM eclipse-temurin:17-jdk-focal
ENV SPRING_PROFILES_ACTIVE development
VOLUME /data
ARG JAR_FILE
EXPOSE 8080
COPY ${JAR_FILE} /app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]