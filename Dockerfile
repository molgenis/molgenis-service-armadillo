FROM eclipse-temurin:17-jdk-focal
EXPOSE 8080
RUN mkdir /app
COPY ${JAR_FILE} molgenis-armadillo.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/molgenis-armadillo.jar"]
