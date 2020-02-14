FROM maven:3.6-jdk-11 AS build
RUN mkdir -p /usr/src/app
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package

FROM adoptopenjdk:11.0.6_10-jdk-hotspot
RUN mkdir -p /usr/app
COPY --from=build /usr/src/app/target/*.jar /usr/app/datashield-service.jar
CMD java -jar /usr/app/datashield-service.jar