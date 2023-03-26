FROM eclipse-temurin:17-jdk-focal
EXPOSE 8080
RUN mkdir /app
COPY build/libs/molgenis-armadillo-*.jar /app/molgenis-armadillo.jar
ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-Djava.security.egd=file:/dev/./urandom","-jar","/app/spring-boot-application.jar"]
