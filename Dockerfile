FROM adoptopenjdk:11.0.6_10-jdk-hotspot
COPY target/datashield-service-0.0.1-SNAPSHOT.jar datashield-service.jar
CMD java -jar datashield-service.jar