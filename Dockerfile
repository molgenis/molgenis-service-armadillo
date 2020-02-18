FROM adoptopenjdk:11.0.6_10-jdk-hotspot

ARG JAR_FILE

RUN mkdir -p /usr/app
COPY ${JAR_FILE} /usr/app/datashield-service.jar
CMD java -jar /usr/app/datashield-service.jar